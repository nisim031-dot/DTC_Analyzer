package com.garage.xtooltranslate.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.garage.xtooltranslate.R
import com.garage.xtooltranslate.capture.MediaProjectionHolder
import com.garage.xtooltranslate.capture.ScreenCaptureService
import com.garage.xtooltranslate.service.XtoolAccessibilityService
import com.garage.xtooltranslate.translate.MlKitTranslator
import android.media.projection.MediaProjectionManager
import kotlinx.coroutines.launch

/**
 * אשף הרשאות בעברית. כל שלב כולל כפתור "פתח הגדרות" ובדיקת סטטוס חיה.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var accessibilityStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var captureStatus: TextView
    private lateinit var modelStatus: TextView

    private val captureLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                MediaProjectionHolder.store(result.resultCode, data)
                ScreenCaptureService.start(this)
                refreshStatuses()
            } else {
                toast("לכידת המסך לא אושרה")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        setContentView(buildLayout())
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.onboarding_title)
            textSize = 22f
            setPadding(0, 0, 0, 32)
        })

        accessibilityStatus = addStep(
            root, R.string.step_accessibility, R.string.open_settings,
        ) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

        overlayStatus = addStep(
            root, R.string.step_overlay, R.string.open_settings,
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
        }

        captureStatus = addStep(
            root, R.string.step_capture, R.string.open_settings,
        ) { requestScreenCapture() }

        modelStatus = addStep(
            root, R.string.step_model, R.string.download_model,
        ) { downloadModel() }

        addStep(
            root, R.string.step_battery, R.string.open_settings,
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName"),
                )
            )
        }

        return root
    }

    private fun addStep(
        parent: LinearLayout,
        labelRes: Int,
        buttonRes: Int,
        onClick: () -> Unit,
    ): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }
        val label = TextView(this).apply {
            text = getString(labelRes)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val status = TextView(this).apply {
            text = getString(R.string.status_pending)
            textSize = 16f
        }
        val button = Button(this).apply {
            text = getString(buttonRes)
            setOnClickListener { onClick() }
        }
        row.addView(label)
        row.addView(status)
        parent.addView(row)
        parent.addView(button)
        return status
    }

    private fun requestScreenCapture() {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(mgr.createScreenCaptureIntent())
    }

    private fun downloadModel() {
        modelStatus.text = getString(R.string.model_downloading)
        lifecycleScope.launch {
            val translator = MlKitTranslator()
            try {
                translator.ensureModelDownloaded(requireWifi = true)
                toast("מודל העברית הורד")
            } catch (e: Exception) {
                toast("הורדת המודל נכשלה: ${e.message}")
            } finally {
                translator.close()
                refreshStatuses()
            }
        }
    }

    private fun refreshStatuses() {
        accessibilityStatus.text = statusText(isAccessibilityEnabled())
        overlayStatus.text = statusText(Settings.canDrawOverlays(this))
        captureStatus.text = statusText(MediaProjectionHolder.hasPermission())
    }

    private fun statusText(done: Boolean): String =
        getString(if (done) R.string.status_done else R.string.status_pending)

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val me = XtoolAccessibilityService::class.java.name
        return enabled.any { it.id?.contains(me) == true || it.resolveInfo?.serviceInfo?.name == me }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
