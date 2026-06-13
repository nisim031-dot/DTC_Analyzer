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

/** הבועה הצפה: אייקון (צבעו לפי חומרת DTC) + פאנל תרגום בעברית (RTL). */
object FloatingBubble {

    private const val BLUE = 0xCC1565C0.toInt()
    private val SEV_COLOR = mapOf(
        "RED" to 0xCCC0392B.toInt(),
        "YELLOW" to 0xCCD4A017.toInt(),
        "GREEN" to 0xCC27AE60.toInt()
    )
    private val SEV_RANK = mapOf("RED" to 0, "YELLOW" to 1, "GREEN" to 2)

    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var panel: TextView? = null
    private var icon: TextView? = null
    private val lines = linkedMapOf<String, String>()
    private var worst: String? = null

    fun show(ctx: Context) {
        if (root != null) return
        val context = ctx.applicationContext
        val w = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = w

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val ic = TextView(context).apply {
            text = "🌐"
            textSize = 22f
            setPadding(28, 18, 28, 18)
            setBackgroundColor(BLUE)
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

        container.addView(ic)
        container.addView(tv)
        root = container
        panel = tv
        icon = ic

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
        ic.setOnTouchListener { _, e ->
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

    /**
     * מוסיף שורת תרגום ומציג בפאנל. אם severity לא null (קוד DTC ידוע),
     * צובע את אייקון הבועה לפי החומרה הגבוהה ביותר שזוהתה.
     */
    fun append(src: String, he: String, severity: String? = null) {
        val tv = panel ?: return
        if (lines[src] != he) {
            lines[src] = he
            while (lines.size > 14) lines.remove(lines.keys.first())
            val text = lines.values.joinToString("\n")
            tv.post { tv.text = text }
        }
        if (severity != null) {
            val ic = icon ?: return
            val better = worst == null ||
                (SEV_RANK[severity] ?: 9) < (SEV_RANK[worst] ?: 9)
            if (better) {
                worst = severity
                val color = SEV_COLOR[severity] ?: BLUE
                ic.post { ic.setBackgroundColor(color) }
            }
        }
    }
}
