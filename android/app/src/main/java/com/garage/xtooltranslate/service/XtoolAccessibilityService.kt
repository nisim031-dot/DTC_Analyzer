package com.garage.xtooltranslate.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.garage.xtooltranslate.capture.OcrEngine
import com.garage.xtooltranslate.capture.ScreenCaptureService
import com.garage.xtooltranslate.model.DetectedText
import com.garage.xtooltranslate.model.TextSource
import com.garage.xtooltranslate.overlay.OverlayController
import com.garage.xtooltranslate.translate.MlKitTranslator
import com.garage.xtooltranslate.translate.TranslationRepository
import com.garage.xtooltranslate.util.TextHeuristics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * מקור הטקסט הראשי + מארח ה-overlay.
 *
 * זרימה: אירוע נגישות → הליכה על עץ ה-nodes → אם אטום, נפילה ל-OCR →
 * תרגום אצווה → עדכון ה-overlay. הכל מווסת (throttle) למניעת עומס.
 */
class XtoolAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var translator: MlKitTranslator
    private lateinit var repository: TranslationRepository
    private lateinit var overlay: OverlayController
    private val ocrEngine = OcrEngine()

    private var lastRun = 0L
    private var inFlight: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        translator = MlKitTranslator()
        repository = TranslationRepository.create(applicationContext, translator)
        overlay = OverlayController(this, useAccessibilityOverlay = true)
        overlay.show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> scheduleRefresh()
        }
    }

    /** ויסות: לכל היותר רענון אחד ל-THROTTLE_MS, ובלי חפיפה. */
    private fun scheduleRefresh() {
        val now = System.currentTimeMillis()
        if (now - lastRun < THROTTLE_MS) return
        if (inFlight?.isActive == true) return
        lastRun = now
        inFlight = scope.launch { refresh() }
    }

    private suspend fun refresh() {
        val root = rootInActiveWindow ?: return
        val screen = Rect().also { root.getBoundsInScreen(it) }
        val nodes = ArrayList<DetectedText>()
        collectText(root, nodes)

        val detected: List<DetectedText> =
            if (TextHeuristics.shouldFallbackToOcr(nodes, screen)) {
                runOcr() ?: nodes
            } else {
                nodes
            }

        val labels = repository.translateBatch(detected)
        // עדכון ה-overlay חייב לרוץ ב-thread הראשי
        scope.launch(Dispatchers.Main) { overlay.update(labels) }
    }

    /** הליכה רקורסיבית על עץ הצמתים ואיסוף טקסט עם גבולות מסך. */
    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<DetectedText>) {
        if (node == null) return
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank() && TextHeuristics.isLikelyTranslatable(text)) {
            val r = Rect().also { node.getBoundsInScreen(it) }
            if (r.width() > 0 && r.height() > 0) {
                out.add(DetectedText(text, r, TextSource.NODE))
            }
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), out)
        }
    }

    /** נפילה ל-OCR: לוכד פריים מ-ScreenCaptureService ומריץ זיהוי. */
    private suspend fun runOcr(): List<DetectedText>? {
        val capture = ScreenCaptureService.instance ?: return null
        val bitmap = capture.captureLatest() ?: return null
        return try {
            ocrEngine.recognize(bitmap, capture.scaleX, capture.scaleY)
        } catch (e: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    override fun onInterrupt() {
        overlay.clear()
    }

    override fun onDestroy() {
        scope.cancel()
        overlay.hide()
        translator.close()
        ocrEngine.close()
        super.onDestroy()
    }

    companion object {
        private const val THROTTLE_MS = 800L
    }
}
