package com.xtool.translator

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** התראת רטט+צליל כשמזוהה קוד DTC בחומרה אדומה (RED). מווסת כדי לא להציף. */
object Alerts {

    @Volatile private var last = 0L

    fun redAlert(ctx: Context) {
        val now = System.currentTimeMillis()
        if (now - last < 4000) return
        last = now

        try {
            val v = ctx.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(250)
            }
        } catch (_: Exception) {}

        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (_: Exception) {}
    }
}
