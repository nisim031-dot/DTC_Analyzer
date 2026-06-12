package com.xtool.translator

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/** הבועה הצפה: אייקון קטן + פאנל תרגום מתעדכן בעברית (RTL). */
object FloatingBubble {

    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var panel: TextView? = null
    private val lines = linkedMapOf<String, String>()

    fun show(ctx: Context) {
        if (root != null) return
        val context = ctx.applicationContext
        val w = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = w

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val icon = TextView(context).apply {
            text = "🌐"
            textSize = 22f
            setPadding(28, 18, 28, 18)
            setBackgroundColor(0xCC1565C0.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val tv = TextView(context).apply {
            setPadding(28, 18, 28, 18)
            setBackgroundColor(0xEE222222.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
            visibility = View.GONE
            textDirection = View.TEXT_DIRECTION_RTL
        }

        container.addView(icon)
        container.addView(tv)
        root = container
        panel = tv

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 220
        }

        // לחיצה = פתח/סגור פאנל ; גרירה = הזזה
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        icon.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = e.rawX; touchY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX - touchX).toInt()
                    lp.y = startY + (e.rawY - touchY).toInt()
                    w.updateViewLayout(container, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(e.rawX - touchX) + abs(e.rawY - touchY)
                    if (moved < 20) {
                        tv.visibility = if (tv.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        w.addView(container, lp)
    }

    /** מוסיף/מעדכן שורת תרגום ומציג בפאנל (מונע כפילויות, שומר 12 אחרונות). */
    fun append(src: String, he: String) {
        val tv = panel ?: return
        if (lines[src] == he) return
        lines[src] = he
        while (lines.size > 12) lines.remove(lines.keys.first())
        val text = lines.values.joinToString("\n")
        tv.post { tv.text = text }
    }
}
