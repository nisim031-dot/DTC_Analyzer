package com.garage.xtooltranslate.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.garage.xtooltranslate.model.RenderedLabel

/**
 * מנהל את חלון ה-overlay מעל ממשק ה-XTOOL.
 *
 * מעדיף TYPE_ACCESSIBILITY_OVERLAY (לא דורש הרשאת "הצגה מעל אפליקציות",
 * ה-API המיועד). נופל ל-TYPE_APPLICATION_OVERLAY אם נדרש.
 */
class OverlayController(
    private val context: Context,
    private val useAccessibilityOverlay: Boolean = true,
) {
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: OverlayView? = null

    fun show() {
        if (view != null) return
        val overlay = OverlayView(context)
        val type = if (useAccessibilityOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val added = runCatching { windowManager.addView(overlay, params) }.isSuccess
        if (added) {
            view = overlay
        } else if (type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) {
            // נפילה ל-TYPE_APPLICATION_OVERLAY אם סוג הנגישות נכשל על המכשיר הנעול
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            runCatching { windowManager.addView(overlay, params) }
                .onSuccess { view = overlay }
        }
    }

    fun update(labels: List<RenderedLabel>) {
        view?.setLabels(labels)
    }

    fun clear() {
        view?.clear()
    }

    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
    }
}
