package com.garage.xtooltranslate.capture

import android.graphics.Bitmap
import android.graphics.Rect
import com.garage.xtooltranslate.model.DetectedText
import com.garage.xtooltranslate.model.TextSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * זיהוי טקסט מתמונת לכידת מסך באמצעות ML Kit (אופליין).
 * מחזיר תיבות בקואורדינטות מסך לאחר שערוך לפי scale factor של הלכידה.
 */
class OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * @param scaleX יחס המרה מפיקסל-לכידה לפיקסל-מסך (רוחב מסך / רוחב לכידה)
     * @param scaleY כנ"ל לגובה
     */
    suspend fun recognize(
        bitmap: Bitmap,
        scaleX: Float,
        scaleY: Float,
    ): List<DetectedText> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val out = ArrayList<DetectedText>()
                for (block in result.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        val scaled = Rect(
                            (box.left * scaleX).toInt(),
                            (box.top * scaleY).toInt(),
                            (box.right * scaleX).toInt(),
                            (box.bottom * scaleY).toInt(),
                        )
                        out.add(DetectedText(line.text, scaled, TextSource.OCR))
                    }
                }
                cont.resume(out)
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    fun close() = recognizer.close()
}
