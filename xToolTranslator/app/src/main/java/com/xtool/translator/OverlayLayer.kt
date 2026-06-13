package com.xtool.translator

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/** פריט תרגום צמוד: מלבן המקור (בקואורדינטות מסך) + הטקסט העברי + חומרה. */
data class OverlayItem(val rect: Rect, val he: String, val severity: String?)

/**
 * שכבת תרגום שקופה מעל כל המסך (סגנון Google Lens).
 * חלון לא-מגיב למגע — המגע עובר לאפליקציית הסורק שמתחת.
 */
object OverlayLayer {

    private const val DEFAULT_BG = 0xE6101820.toInt()
    private val SEV_BG = mapOf(
        "RED" to 0xF2B71C1C.toInt(),
        "YELLOW" to 0xF2B8860B.toInt(),
        "GREEN" to 0xF21B7A3D.toInt()
    )

    private var wm: WindowManager? = null
    private var frame: FrameLayout? = null
    private var lastKey: String? = null   // מונע ציור מחדש של תוכן זהה (אנטי-הבהוב)

    fun show(ctx: Context) {
        if (frame != null) return
        val context = ctx.applicationContext
        val w = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = w
        val f = FrameLayout(context)
        frame = f

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        w.addView(f, lp)
    }

    /** מצייר את כל התרגומים במיקום המקורי שלהם על המסך. */
    fun render(items: List<OverlayItem>) {
        val f = frame ?: return
        val key = items.joinToString("|") { "${it.rect.left},${it.rect.top}:${it.he}" }
        f.post {
            if (key == lastKey) return@post   // תוכן זהה למה שכבר מצויר → אל תצייר מחדש
            lastKey = key
            f.removeAllViews()
            for (it in items) {
                val tv = TextView(f.context).apply {
                    text = it.he
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 15f
                    setPadding(14, 6, 14, 6)
                    setBackgroundColor(SEV_BG[it.severity] ?: DEFAULT_BG)
                    textDirection = View.TEXT_DIRECTION_RTL
                    maxLines = 1
                }
                val lp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                lp.leftMargin = it.rect.left.coerceAtLeast(0)
                lp.topMargin = it.rect.top.coerceAtLeast(0)
                f.addView(tv, lp)
            }
        }
    }

    fun clear() {
        val f = frame ?: return
        f.post {
            if (lastKey == null && f.childCount == 0) return@post
            lastKey = null
            f.removeAllViews()
        }
    }

    fun hide() {
        frame?.let { wm?.removeView(it) }
        frame = null
        lastKey = null
    }
}
