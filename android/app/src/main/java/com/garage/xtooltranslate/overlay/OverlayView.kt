package com.garage.xtooltranslate.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.garage.xtooltranslate.model.RenderedLabel
import kotlin.math.min

/**
 * View שקוף מלא-מסך שמצייר תוויות עברית RTL מעל הטקסט המקורי.
 * ציור ב-Canvas (לא TextViews) — זול בזיכרון, חשוב על מכשיר 2GB.
 */
class OverlayView(context: Context) : View(context) {

    private var labels: List<RenderedLabel> = emptyList()
    private val maxLabels = 20  // תקרת תוויות לשמירה על ביצועים

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 20, 20, 20)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT  // עברית מיושרת לימין
    }

    fun setLabels(newLabels: List<RenderedLabel>) {
        labels = newLabels.take(maxLabels)
        invalidate()
    }

    fun clear() {
        if (labels.isEmpty()) return
        labels = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        for (label in labels) {
            val b = label.bounds
            if (b.width() <= 0 || b.height() <= 0) continue

            // גודל גופן מותאם לגובה ה-bounds המקורי, מוגבל לטווח קריא
            textPaint.textSize = min(b.height() * 0.7f, 42f).coerceAtLeast(18f)

            val pad = 6f
            val textWidth = textPaint.measureText(label.hebrew)
            val right = b.right.toFloat()
            val left = (right - textWidth - pad * 2).coerceAtLeast(0f)
            val top = b.top.toFloat()
            val bottom = (b.bottom.toFloat()).coerceAtLeast(top + textPaint.textSize + pad)

            canvas.drawRoundRect(RectF(left, top, right, bottom), 8f, 8f, bgPaint)

            val baseline = bottom - pad - textPaint.descent()
            canvas.drawText(label.hebrew, right - pad, baseline, textPaint)
        }
    }
}
