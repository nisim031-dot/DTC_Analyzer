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

/** לכידת מסך מתמשכת → OCR → תרגום → הצגה בבועה. */
class LiveTranslateService : Service() {

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var lastRun = 0L
    @Volatile private var busy = false

    private val ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        FloatingBubble.show(this)
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
            if (busy || now - lastRun < 1200) {
                img.close()
                return@setOnImageAvailableListener
            }
            lastRun = now
            busy = true
            val bmp = toBitmap(img)
            img.close()
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

    private fun runOcr(bmp: Bitmap) {
        ocr.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { vt ->
                for (block in vt.textBlocks) {
                    for (line in block.lines) {
                        val t = line.text.trim()
                        if (t.isNotEmpty()) {
                            TranslationEngine.resolve(t) { r -> FloatingBubble.append(t, r.text, r.severity) }
                        }
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
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
