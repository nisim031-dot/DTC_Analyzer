package com.garage.xtooltranslate.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * רושם קריסות/שגיאות לקובץ נגיש כדי שאפשר לשלוח אותו לאבחון.
 * מיקום: Internal Storage/Android/data/com.garage.xtooltranslate/files/crash.log
 */
object CrashLog {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun file(context: Context): File? {
        val dir = context.getExternalFilesDir(null) ?: return null
        return File(dir, "crash.log")
    }

    fun log(context: Context, where: String, t: Throwable) {
        try {
            val f = file(context) ?: return
            f.appendText("[${fmt.format(Date())}] @$where\n${t.stackTraceToString()}\n\n")
        } catch (_: Exception) {
            // אסור שרישום הלוג עצמו יקרוס
        }
    }
}
