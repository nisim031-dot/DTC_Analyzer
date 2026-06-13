package com.xtool.translator

/** מידע על קוד DTC ידוע — מתוך בסיס הידע של DTC Analyzer. */
data class DtcInfo(
    val severity: String,   // RED / YELLOW / GREEN
    val titleHe: String,
    val actionHe: String
)

/**
 * בסיס ידע + מילון מונחים מקצועי.
 * resolve() בודק קודם קוד DTC, אחר כך מילון מונחים, ורק אז נופל לתרגום מכונה.
 */
object Knowledge {

    /** קודי DTC ידועים (KIA Sportage / Hyundai IONIQ) — הסבר, חומרה ופעולה בעברית. */
    val DTC: Map<String, DtcInfo> = mapOf(
        "C136081" to DtcInfo(
            "RED",
            "שגיאה בלתי הפיכה במערכת יציבות ESP (SCC/AEB)",
            "מחק קודים ונסע; אם חוזר — בדוק 4 חישני מהירות גלגלים. קשור לתקלת EPB."
        ),
        "C165208" to DtcInfo(
            "YELLOW",
            "שגיאת תקשורת CAN בין ESP לבלם חניה חשמלי (EPB)",
            "תקן קודם את C241701 (מנוע EPB) — תקלה זו צפויה להיעלם מעצמה."
        ),
        "C241701" to DtcInfo(
            "RED",
            "קצר/נתק במנוע בלם חניה חשמלי — אחורי ימין (EPB RH)",
            "בדוק חוטים למנוע; מדוד התנגדות (תקין 1-5Ω). אם תקול — החלף קליפר + כיול."
        ),
        "B28CD13" to DtcInfo(
            "GREEN",
            "אנטנת eCall מנותקת (מעגל פתוח)",
            "בישראל ניתן להתעלם — ללא השפעה על נסיעה. לתיקון: בדוק חיבור אנטנת eCall."
        )
    )

    /** מילון מונחי סורק EN→HE — תרגום מקצועי, לא מילולי. */
    val GLOSSARY: Map<String, String> = mapOf(
        "special function" to "פונקציות מיוחדות",
        "vgt relearn" to "כיול טורבו VGT",
        "start-stop reset" to "איפוס סטארט-סטופ",
        "throttle" to "מצערת",
        "throttle relearn" to "כיול מצערת",
        "injector coding" to "קידוד מזרקים",
        "egr relearn" to "כיול שסתום EGR",
        "dpf regeneration" to "התחדשות מסנן חלקיקים DPF",
        "airbag reset" to "איפוס כרית אוויר",
        "a/c relearn" to "כיול מזגן",
        "abs bleeding" to "סינון בלמים ABS",
        "tire size setting" to "הגדרת מידת צמיג",
        "window initialization" to "אתחול חלונות",
        "bms reset" to "איפוס ניהול מצבר BMS",
        "oil reset" to "איפוס שמן",
        "steering angle" to "זווית היגוי",
        "battery" to "מצבר",
        "brake" to "בלם",
        "engine" to "מנוע",
        "transmission" to "תיבת הילוכים",
        "diagnosis" to "אבחון",
        "live data" to "נתונים חיים",
        "read codes" to "קריאת קודים",
        "clear" to "מחיקה",
        "erase" to "מחיקה",
        "trouble code" to "קוד תקלה",
        "calibration" to "כיול",
        "coding" to "קידוד",
        "reset" to "איפוס",
        "settings" to "הגדרות"
    )

    // קוד DTC: אות מערכת (P/B/C/U) + 4-6 ספרות הקס (תומך גם P0420 וגם C241701)
    private val DTC_RE = Regex("[PBCU][0-9A-F]{4,6}", RegexOption.IGNORE_CASE)

    /** מחזיר קוד DTC ידוע אם קיים בטקסט, אחרת null. */
    fun findDtc(text: String): Pair<String, DtcInfo>? {
        val m = DTC_RE.find(text.uppercase()) ?: return null
        val code = m.value.uppercase()
        val info = DTC[code] ?: return null
        return code to info
    }
}
