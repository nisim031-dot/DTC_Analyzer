package com.garage.xtooltranslate.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * תרגום אופליין EN→HE באמצעות ML Kit On-Device Translation.
 * חובה להוריד את מודל העברית פעם אחת (באונבורדינג, על Wi-Fi) — אחריו עובד ללא אינטרנט.
 */
class MlKitTranslator {

    private val translator: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HEBREW)
            .build()
    )

    /** מוריד את מודל העברית אם חסר. דורש אינטרנט. */
    suspend fun ensureModelDownloaded(requireWifi: Boolean = true) =
        suspendCancellableCoroutine<Unit> { cont ->
            val conditions = DownloadConditions.Builder().apply {
                if (requireWifi) requireWifi()
            }.build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun translate(text: String): String =
        suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    fun close() = translator.close()
}
