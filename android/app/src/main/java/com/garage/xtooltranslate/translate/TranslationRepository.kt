package com.garage.xtooltranslate.translate

import android.content.Context
import com.garage.xtooltranslate.model.DetectedText
import com.garage.xtooltranslate.model.RenderedLabel
import com.garage.xtooltranslate.util.TextHeuristics

/**
 * מתזמר את שכבת התרגום: מילון → מטמון → ML Kit.
 * מקבל רשימת טקסטים שזוהו ומחזיר תוויות עברית מוכנות לציור.
 */
class TranslationRepository(
    private val dictionary: DtcDictionary,
    // עשוי להיות null אם ML Kit נכשל באתחול — אז עובדים במצב מילון-בלבד
    private val translator: MlKitTranslator?,
    private val cache: TranslationCache = TranslationCache(),
) {
    /** תרגום מחרוזת בודדת לפי סדר העדיפויות. */
    suspend fun translate(text: String): String? {
        val source = text.trim()
        if (!TextHeuristics.isLikelyTranslatable(source)) return null

        // 1) מילון DTC/מונחים — מדויק ומיידי (עובד גם בלי ML Kit)
        dictionary.lookup(source)?.let { return it }

        // 2) מטמון
        cache.get(source)?.let { return it }

        // 3) ML Kit אופליין (אם זמין)
        val t = translator ?: return null
        return try {
            val hebrew = t.translate(source)
            cache.put(source, hebrew)
            hebrew
        } catch (e: Throwable) {
            null
        }
    }

    /** תרגום אצווה של טקסטים שזוהו לתוויות מוכנות לציור. */
    suspend fun translateBatch(detected: List<DetectedText>): List<RenderedLabel> =
        detected.mapNotNull { dt ->
            translate(dt.text)?.let { RenderedLabel(it, dt.bounds) }
        }

    companion object {
        fun create(context: Context, translator: MlKitTranslator?): TranslationRepository =
            TranslationRepository(DtcDictionary.load(context), translator)
    }
}
