package com.xtool.translator

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import java.util.concurrent.ConcurrentHashMap

/** תוצאת תרגום — טקסט בעברית + חומרה (RED/YELLOW/GREEN) אם זה קוד DTC ידוע. */
data class TransResult(val text: String, val severity: String? = null)

/**
 * מנוע "היברידי": קוד DTC ידוע → מבסיס הידע (הסבר+חומרה+פעולה),
 * מונח סורק → מהמילון המקצועי, וכל השאר → ML Kit Translate (אופליין).
 */
object TranslationEngine {

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HEBREW)
            .build()
    )

    private val cache = ConcurrentHashMap<String, String>()

    fun warmUp() {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
    }

    fun resolve(src: String, callback: (TransResult) -> Unit) {
        // 1. קוד DTC ידוע → הסבר + חומרה + פעולה
        Knowledge.findDtc(src)?.let { (code, info) ->
            callback(TransResult("$code — ${info.titleHe}\n↪ ${info.actionHe}", info.severity))
            return
        }

        // 2. מונח סורק במילון המקצועי (התאמה מדויקת, חסר תלות ברישיות)
        val key = src.trim().lowercase()
        Knowledge.GLOSSARY[key]?.let {
            callback(TransResult(it))
            return
        }

        // 3. כבר תורגם → מהמטמון
        cache[src]?.let {
            callback(TransResult(it))
            return
        }

        // 4. נפילה לתרגום מכונה — תמיד מחזיר callback (גם בכישלון) כדי שמוני
        //    ה-pending בצד הקורא לא ייתקעו ושכבת ה-AR תמיד תתרנדר.
        translator.translate(src)
            .addOnSuccessListener { he ->
                cache[src] = he
                callback(TransResult(he))
            }
            .addOnFailureListener {
                callback(TransResult(src))   // נשאיר את הטקסט המקורי אם התרגום נכשל
            }
    }
}
