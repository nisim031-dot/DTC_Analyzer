package com.garage.xtooltranslate

import android.app.Application
import com.garage.xtooltranslate.util.CrashLog

/** נקודת כניסה ליישום. מתקין מטפל קריסות גלובלי לאבחון. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            CrashLog.log(applicationContext, "uncaught:${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }
}
