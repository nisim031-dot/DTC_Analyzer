package com.garage.xtooltranslate.util

import android.graphics.Rect
import com.garage.xtooltranslate.model.DetectedText

/**
 * היוריסטיקות להחלטה אילו מחרוזות לתרגם והאם לעבור ל-OCR.
 */
object TextHeuristics {

    private val LATIN = Regex("[A-Za-z]")
    private val HEBREW = Regex("[\\u0590-\\u05FF]")

    /** מחרוזת ראויה לתרגום: יש בה אותיות לטיניות, אינה כבר בעברית, ובאורך סביר. */
    fun isLikelyTranslatable(text: String): Boolean {
        val t = text.trim()
        if (t.length < 2) return false
        if (HEBREW.containsMatchIn(t)) return false        // כבר עברית
        if (!LATIN.containsMatchIn(t)) return false        // מספרים/סמלים בלבד
        return true
    }

    /**
     * מחליט אם עץ הנגישות "אטום" ויש לעבור ל-OCR.
     * אטום = כמעט אין צמתי טקסט שמכסים שטח משמעותי של המסך.
     */
    fun shouldFallbackToOcr(
        nodes: List<DetectedText>,
        screen: Rect,
        minNodes: Int = 2,
        minCoverageRatio: Float = 0.02f,
    ): Boolean {
        val translatable = nodes.filter { isLikelyTranslatable(it.text) }
        if (translatable.size >= minNodes) return false
        val screenArea = screen.width().toLong() * screen.height().toLong()
        if (screenArea <= 0) return translatable.isEmpty()
        val covered = translatable.sumOf { it.bounds.width().toLong() * it.bounds.height().toLong() }
        return covered.toFloat() / screenArea.toFloat() < minCoverageRatio
    }
}
