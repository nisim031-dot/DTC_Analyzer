package com.xtool.translator

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.abs

/** לכידת מסך חכמה (רק כשהמסך משתנה) → OCR → תרגום → פאנל + שכבת AR + התראה. */
class LiveTranslateService : Service() {

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var lastRun = 0L
    @Volatile private var busy = false
    private var lastSignature: IntArray? = null
    private var generation = 0   // מונע דריסת overlay מפריים ישן

    private val ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        OverlayLayer.show(this)   // שכבת AR מתחת
        FloatingBubble.show(this) // הבועה מעל
        TranslationEngine.warmUp()

        val code = intent?.getIntExtra("code", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>("data")
        if (code == Activity.RESULT_OK && data != null) {
            startCapture(code, data)
        }
        return START_STICKY
    }

    private fun startCapture(code: Int, data: Intent) {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = mpm.getMediaProjection(code, data)
        projection = p
        p.registerCallback(object : MediaProjection.Callback() {}, handler)

        val dm = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels

        val r = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader = r

        virtualDisplay = p.createVirtualDisplay(
            "live",
            width, height, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            r.surface, null, handler
        )

        r.setOnImageAvailableListener({ ir ->
            val img = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
            val now = System.currentTimeMillis()
            if (busy || now - lastRun < 500) {
                img.close()
                return@setOnImageAvailableListener
            }
            lastRun = now
            busy = true
            val bmp = toBitmap(img)
            img.close()

            // מצב חכם: לדלג אם המסך לא השתנה מהותית
            val sig = signature(bmp)
            if (!changed(sig, lastSignature)) {
                bmp.recycle()
                busy = false
                return@setOnImageAvailableListener
            }
            lastSignature = sig
            runOcr(bmp)
        }, handler)
    }

    private fun toBitmap(img: Image): Bitmap {
        val plane = img.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * img.width
        val bmp = Bitmap.createBitmap(
            img.width + rowPadding / pixelStride,
            img.height,
            Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(buffer)
        return bmp
    }

    /** חתימת 8x8 של בהירות — להשוואת "האם המסך השתנה". */
    private fun signature(bmp: Bitmap): IntArray {
        val small = Bitmap.createScaledBitmap(bmp, 8, 8, false)
        val px = IntArray(64)
        small.getPixels(px, 0, 8, 0, 0, 8, 8)
        small.recycle()
        for (i in px.indices) {
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            px[i] = (r * 30 + g * 59 + b * 11) / 100
        }
        return px
    }

    private fun changed(now: IntArray, prev: IntArray?): Boolean {
        if (prev == null) return true
        var diff = 0
        for (i in now.indices) if (abs(now[i] - prev[i]) > 24) diff++
        return diff > 3
    }

    private fun runOcr(bmp: Bitmap) {
        ocr.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { vt ->
                val lines = vt.textBlocks
                    .flatMap { it.lines }
                    .filter { it.text.trim().length >= 2 && it.boundingBox != null }
                if (lines.isEmpty()) {
                    OverlayLayer.clear()
                    return@addOnSuccessListener
                }
                val myGen = ++generation
                val items = ArrayList<OverlayItem>()
                var pending = lines.size
                for (line in lines) {
                    val t = line.text.trim()
                    val box = line.boundingBox!!
                    TranslationEngine.resolve(t) { r ->
                        items.add(OverlayItem(box, r.text, r.severity))
                        FloatingBubble.append(t, r.text, r.severity)
                        if (r.severity == "RED") Alerts.redAlert(this)
                        // רק הפריים העדכני ביותר מצייר — מונע דריסה בנתונים ישנים
                        if (--pending == 0 && myGen == generation) OverlayLayer.render(items)
                    }
                }
            }
            .addOnCompleteListener {
                bmp.recycle()
                busy = false
            }
    }

    private fun buildNotification(): Notification {
        val channelId = "xtool_live"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "xTool Live Translate", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("xTool מתרגם פעיל")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        reader?.close()
        projection?.stop()
        OverlayLayer.hide()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
