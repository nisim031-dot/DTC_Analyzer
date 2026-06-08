package com.garage.xtooltranslate.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Foreground service שמחזיק MediaProjection ומספק לכידת פריים בודד לפי דרישה.
 * לכידה ברזולוציה מוקטנת (חצי) כדי לחסוך זיכרון על מכשיר 2GB.
 */
class ScreenCaptureService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var captureWidth = 0
    private var captureHeight = 0
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // באנדרואיד 10 (Q) חובה להכריז על סוג mediaProjection אחרת getMediaProjection קורס
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        if (!MediaProjectionHolder.hasPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        instance = this
        try {
            setupProjection()
        } catch (e: Exception) {
            // אם הלכידה נכשלת — לא מפילים את כל האפליקציה, פשוט עוצרים את השירות
            instance = null
            stopSelf()
        }
        return START_STICKY
    }

    private fun setupProjection() {
        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val density = metrics.densityDpi

        // לכידה בחצי רזולוציה — חוסך זיכרון פי ~4
        captureWidth = (screenW / 2).coerceAtLeast(1)
        captureHeight = (screenH / 2).coerceAtLeast(1)
        scaleX = screenW.toFloat() / captureWidth
        scaleY = screenH.toFloat() / captureHeight

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(
            MediaProjectionHolder.resultCode,
            MediaProjectionHolder.resultData!!,
        )

        imageReader = ImageReader.newInstance(
            captureWidth, captureHeight, PixelFormat.RGBA_8888, 2,
        )
        virtualDisplay = projection?.createVirtualDisplay(
            "xtool-capture",
            captureWidth, captureHeight, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null,
        )
    }

    /** מחזיר את הפריים האחרון כ-Bitmap, או null אם אין. הקורא אחראי ל-recycle. */
    fun captureLatest(): Bitmap? {
        val reader = imageReader ?: return null
        val image = reader.acquireLatestImage() ?: return null
        return try {
            val plane = image.planes[0]
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val rowPadding = rowStride - pixelStride * captureWidth
            val bitmap = Bitmap.createBitmap(
                captureWidth + rowPadding / pixelStride,
                captureHeight, Bitmap.Config.ARGB_8888,
            )
            bitmap.copyPixelsFromBuffer(plane.buffer)
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            image.close()
        }
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "xtool_capture"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId, "לכידת מסך לתרגום",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("תרגום XTOOL פעיל")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 42
        @Volatile
        var instance: ScreenCaptureService? = null
            private set

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ScreenCaptureService::class.java))
        }
    }
}
