package com.xtool.translator

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import java.util.concurrent.ConcurrentHashMap

/** מנוע תרגום אנגלית→עברית (On-Device, אופליין) משותף לשני השירותים. */
object TranslationEngine {

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HEBREW)
            .build()
    )

    private val cache = ConcurrentHashMap<String, String>()

    /** מוריד את מודל התרגום פעם אחת (גם בלי Wi-Fi). */
    fun warmUp() {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
    }

    fun translate(src: String, callback: (String) -> Unit) {
        val cached = cache[src]
        if (cached != null) {
            callback(cached)
            return
        }
        translator.translate(src)
            .addOnSuccessListener { he ->
                cache[src] = he
                callback(he)
            }
    }
}
