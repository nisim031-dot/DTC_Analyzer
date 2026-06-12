package com.xtool.translator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {

    private val overlayReq = 1001
    private val mediaReq = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. הרשאת Overlay (בועה צפה)
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ),
                overlayReq
            )
            return
        }
        proceed()
    }

    private fun proceed() {
        // 2. הרשאת Accessibility — פתיחת המסך, המשתמש מפעיל ידנית
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "הפעל את \"xTool מתרגם\" ברשימת הנגישות", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // 3. הרשאת לכידת מסך → מפעיל את שירות הבועה + OCR
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), mediaReq)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            overlayReq -> if (Settings.canDrawOverlays(this)) proceed()
            mediaReq -> if (resultCode == RESULT_OK && data != null) {
                val intent = Intent(this, LiveTranslateService::class.java).apply {
                    putExtra("code", resultCode)
                    putExtra("data", data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                finish()
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return flat.contains("HybridReaderService")
    }
}
