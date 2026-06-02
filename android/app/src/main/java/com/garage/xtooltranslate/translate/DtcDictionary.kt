package com.garage.xtooltranslate.translate

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * מילון קודי DTC + מונחי ממשק, נטען מ-assets.
 * - dtc_dictionary.json: {code: {title_he, explanation_he, ...}} (מיוצא מ-KNOWLEDGE_BASE)
 * - term_glossary.json:  {english: hebrew}
 */
class DtcDictionary private constructor(
    private val dtcTitles: Map<String, String>,
    private val glossary: Map<String, String>,
) {
    private val dtcCodeRegex = Regex("\\b[PCBU][0-9A-F]{4,6}\\b", RegexOption.IGNORE_CASE)

    /** ניסיון תרגום מהיר ממילון בלבד. מחזיר null אם אין התאמה. */
    fun lookup(text: String): String? {
        val trimmed = text.trim()

        // 1) קוד DTC מדויק בתוך המחרוזת
        dtcCodeRegex.find(trimmed)?.let { m ->
            dtcTitles[m.value.uppercase()]?.let { return it }
        }

        // 2) מונח ממשק מדויק (case-insensitive)
        glossary[trimmed]?.let { return it }
        glossary.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }
            ?.let { return it.value }

        return null
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(context: Context): DtcDictionary {
            val dtcTitles = parseDtcTitles(readAsset(context, "dtc_dictionary.json"))
            val glossary = parseFlat(readAsset(context, "term_glossary.json"))
            return DtcDictionary(dtcTitles, glossary)
        }

        private fun readAsset(context: Context, name: String): String =
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }

        /** מחלץ רק את title_he מכל קוד — זה מה שמצויר ב-overlay. */
        private fun parseDtcTitles(raw: String): Map<String, String> {
            val root = json.parseToJsonElement(raw) as JsonObject
            return root.mapNotNull { (code, value) ->
                val title = (value as? JsonObject)?.get("title_he")?.jsonPrimitive?.contentOrNull
                if (title != null) code.uppercase() to title else null
            }.toMap()
        }

        private fun parseFlat(raw: String): Map<String, String> {
            val root = json.parseToJsonElement(raw) as JsonObject
            return root.mapValues { it.value.jsonPrimitive.content }
        }
    }
}
