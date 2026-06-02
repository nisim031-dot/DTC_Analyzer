package com.garage.xtooltranslate.model

import android.graphics.Rect

/** מקור הטקסט שזוהה — קובע אמינות וכיול קואורדינטות. */
enum class TextSource { NODE, OCR }

/** מחרוזת שזוהתה על המסך יחד עם מיקומה בקואורדינטות מסך. */
data class DetectedText(
    val text: String,
    val bounds: Rect,
    val source: TextSource,
)

/** תווית מוכנה לציור: הטקסט העברי ומיקום המקור על המסך. */
data class RenderedLabel(
    val hebrew: String,
    val bounds: Rect,
)
