package com.xtool.translator

/** מידע על קוד DTC ידוע — מתוך/בהשראת בסיס הידע של DTC Analyzer. */
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

    /** קודי DTC — ספציפיים לרכב (KIA/Hyundai) + קודי OBD-II גנריים נפוצים. */
    val DTC: Map<String, DtcInfo> = mapOf(
        // ── ספציפיים לרכב של Nisim (KIA Sportage / Hyundai IONIQ) ──
        "C136081" to DtcInfo("RED", "שגיאה בלתי הפיכה במערכת יציבות ESP (SCC/AEB)",
            "מחק קודים ונסע; אם חוזר — בדוק 4 חישני מהירות גלגלים. קשור לתקלת EPB."),
        "C165208" to DtcInfo("YELLOW", "שגיאת תקשורת CAN בין ESP לבלם חניה חשמלי (EPB)",
            "תקן קודם את C241701 (מנוע EPB) — תקלה זו צפויה להיעלם מעצמה."),
        "C241701" to DtcInfo("RED", "קצר/נתק במנוע בלם חניה חשמלי — אחורי ימין (EPB RH)",
            "בדוק חוטים למנוע; מדוד התנגדות (תקין 1-5Ω). אם תקול — החלף קליפר + כיול."),
        "B28CD13" to DtcInfo("GREEN", "אנטנת eCall מנותקת (מעגל פתוח)",
            "בישראל ניתן להתעלם — ללא השפעה על נסיעה. לתיקון: בדוק חיבור אנטנת eCall."),

        // ── מנוע / VVT / חיישנים ──
        "P0011" to DtcInfo("YELLOW", "עיתוי גל זיזה מתקדם מדי (Bank 1)",
            "בדוק שמן, שסתום VVT/OCV וחיווט. שמן מלוכלך גורם נפוץ."),
        "P0016" to DtcInfo("YELLOW", "אי-התאמה בין חישן ארכובה לגל זיזה",
            "בדוק מתיחות/קפיצת שרשרת תזמון וחישני Crank/Cam."),
        "P0101" to DtcInfo("YELLOW", "טווח/ביצועי חישן זרימת אוויר (MAF)",
            "נקה את חישן ה-MAF, בדוק דליפות אוויר וחיווט."),
        "P0106" to DtcInfo("YELLOW", "טווח/ביצועי חישן לחץ מנפולד (MAP)",
            "בדוק צינור ואקום וחיישן MAP."),
        "P0113" to DtcInfo("GREEN", "מתח גבוה בחישן טמפ' אוויר נכנס (IAT)",
            "בדוק חיווט וחיבור חישן IAT."),
        "P0118" to DtcInfo("YELLOW", "מתח גבוה בחישן טמפ' מנוע (ECT)",
            "בדוק חישן ECT וחיווט — קריאה שגויה משבשת תערובת דלק."),
        "P0122" to DtcInfo("YELLOW", "מתח נמוך בחישן מצערת (TPS)",
            "בדוק חיווט וחיבור גוף מצערת; ייתכן צורך בכיול מצערת."),
        "P0128" to DtcInfo("YELLOW", "טמפ' נוזל קירור מתחת לסף (תרמוסטט)",
            "כנראה תרמוסטט תקוע פתוח — החלף תרמוסטט."),

        // ── חישני חמצן / תערובת דלק ──
        "P0131" to DtcInfo("YELLOW", "מתח נמוך בחישן חמצן (B1S1)",
            "בדוק חישן O2 קדמי, דליפות אגזוז וחיווט."),
        "P0135" to DtcInfo("YELLOW", "תקלה בגוף חימום חישן חמצן (B1S1)",
            "בדוק נתיך וחימום חישן O2 — לרוב צריך החלפת חישן."),
        "P0137" to DtcInfo("GREEN", "מתח נמוך בחישן חמצן אחורי (B1S2)",
            "חישן אחרי הממיר — בדוק חיווט/חישן. לרוב לא דחוף."),
        "P0171" to DtcInfo("YELLOW", "תערובת רזה מדי (Bank 1)",
            "בדוק דליפות ואקום, MAF, לחץ דלק ומזרקים."),
        "P0172" to DtcInfo("YELLOW", "תערובת עשירה מדי (Bank 1)",
            "בדוק לחץ דלק, מזרקים דולפים וחישן MAF."),
        "P0174" to DtcInfo("YELLOW", "תערובת רזה מדי (Bank 2)",
            "בדוק דליפות ואקום ולחץ דלק."),

        // ── הצתות חסר (Misfire) ──
        "P0300" to DtcInfo("RED", "הצתות חסר אקראיות במספר צילינדרים",
            "בדוק מצתים, סלילים, מזרקים ולחץ דחיסה. נהיגה ממושכת מזיקה לממיר."),
        "P0301" to DtcInfo("RED", "הצתת חסר בצילינדר 1",
            "החלף מצת/סליל צילינדר 1; בדוק מזרק ודחיסה."),
        "P0302" to DtcInfo("RED", "הצתת חסר בצילינדר 2",
            "החלף מצת/סליל צילינדר 2; בדוק מזרק ודחיסה."),
        "P0303" to DtcInfo("RED", "הצתת חסר בצילינדר 3",
            "החלף מצת/סליל צילינדר 3; בדוק מזרק ודחיסה."),
        "P0304" to DtcInfo("RED", "הצתת חסר בצילינדר 4",
            "החלף מצת/סליל צילינדר 4; בדוק מזרק ודחיסה."),

        // ── ארכובה / גל זיזה / נקישה ──
        "P0327" to DtcInfo("YELLOW", "מתח נמוך בחישן נקישה (Knock)",
            "בדוק חישן נקישה וחיווט; קריאה שגויה מאחרת הצתה."),
        "P0335" to DtcInfo("RED", "תקלה בחישן מיקום ארכובה (CKP)",
            "בדוק חישן ארכובה וחיווט — עלול לגרום לעצירת מנוע."),
        "P0340" to DtcInfo("YELLOW", "תקלה בחישן מיקום גל זיזה (CMP)",
            "בדוק חישן גל זיזה וחיווט."),

        // ── EGR / ממיר קטליטי / EVAP ──
        "P0401" to DtcInfo("YELLOW", "זרימת EGR לא מספקת",
            "נקה שסתום EGR ונתיבים מפיח."),
        "P0420" to DtcInfo("YELLOW", "יעילות ממיר קטליטי מתחת לסף (B1)",
            "בדוק חישני O2 ודליפות אגזוז; ייתכן ממיר שחוק."),
        "P0430" to DtcInfo("YELLOW", "יעילות ממיר קטליטי מתחת לסף (B2)",
            "בדוק חישני O2 ודליפות אגזוז; ייתכן ממיר שחוק."),
        "P0442" to DtcInfo("GREEN", "דליפה קטנה במערכת אידוי דלק (EVAP)",
            "בדוק פקק דלק ואטמים — לרוב פקק דלק רופף."),
        "P0455" to DtcInfo("GREEN", "דליפה גדולה במערכת אידוי דלק (EVAP)",
            "בדוק פקק דלק, צנרת EVAP ושסתום Purge."),

        // ── מהירות / סרק / מתח / ECU ──
        "P0500" to DtcInfo("YELLOW", "תקלה בחישן מהירות רכב (VSS)",
            "בדוק חישן מהירות וחיווט."),
        "P0506" to DtcInfo("YELLOW", "סיבובי סרק נמוכים מהצפוי",
            "נקה גוף מצערת ובדוק דליפות ואקום."),
        "P0562" to DtcInfo("YELLOW", "מתח מערכת נמוך",
            "בדוק אלטרנטור, מצבר וחיבורי הארקה."),
        "P0606" to DtcInfo("RED", "תקלה במעבד יחידת בקרת מנוע (ECM)",
            "בדוק הזנות/הארקות ל-ECM; ייתכן צורך בהחלפה/תכנות."),

        // ── תיבת הילוכים ──
        "P0700" to DtcInfo("RED", "בקשת נורית תקלה ממערכת תיבת ההילוכים (TCM)",
            "סרוק את ה-TCM לקודים נוספים — קוד זה מצביע על תקלה בתיבה."),
        "P0715" to DtcInfo("YELLOW", "תקלה בחישן מהירות כניסה לתיבה",
            "בדוק חישן מהירות קלט וחיווט."),
        "P0741" to DtcInfo("YELLOW", "החלקה במצמד ממיר המומנט (TCC)",
            "בדוק שמן תיבה, סולנואיד TCC ולחצים."),

        // ── תקשורת רשת (U-codes) ──
        "U0100" to DtcInfo("RED", "אובדן תקשורת עם יחידת בקרת מנוע (ECM)",
            "בדוק הזנה/הארקה ל-ECM וקווי CAN — תקלה קריטית."),
        "U0121" to DtcInfo("YELLOW", "אובדן תקשורת עם יחידת ABS",
            "בדוק הזנה ל-ABS וקווי CAN."),
        "U0155" to DtcInfo("YELLOW", "אובדן תקשורת עם לוח שעונים",
            "בדוק הזנה ללוח השעונים וקווי CAN.")
    )

    /** מילון מונחי סורק EN→HE — תרגום מקצועי, לא מילולי. */
    val GLOSSARY: Map<String, String> = mapOf(
        // תפריטי סורק
        "special function" to "פונקציות מיוחדות",
        "special functions" to "פונקציות מיוחדות",
        "diagnosis" to "אבחון",
        "auto scan" to "סריקה אוטומטית",
        "scan" to "סריקה",
        "system" to "מערכת",
        "module" to "יחידה",
        "ecu information" to "מידע יחידת בקרה",
        "version information" to "מידע גרסה",
        "vehicle" to "רכב",
        "read codes" to "קריאת קודים",
        "read fault codes" to "קריאת קודי תקלה",
        "clear codes" to "מחיקת קודים",
        "erase codes" to "מחיקת קודים",
        "clear" to "מחיקה",
        "erase" to "מחיקה",
        "freeze frame" to "תמונת קפיאה",
        "live data" to "נתונים חיים",
        "data stream" to "זרם נתונים",
        "actuation test" to "בדיקת מפעילים",
        "active test" to "בדיקה אקטיבית",
        "trouble code" to "קוד תקלה",
        "fault code" to "קוד תקלה",
        "current" to "נוכחי",
        "history" to "היסטוריה",
        "pending" to "ממתין",
        "permanent" to "קבוע",
        "confirmed" to "מאומת",
        "status" to "מצב",
        // פונקציות מיוחדות
        "vgt relearn" to "כיול טורבו VGT",
        "start-stop reset" to "איפוס סטארט-סטופ",
        "throttle" to "מצערת",
        "throttle relearn" to "כיול מצערת",
        "throttle body" to "גוף מצערת",
        "injector coding" to "קידוד מזרקים",
        "egr relearn" to "כיול שסתום EGR",
        "dpf regeneration" to "התחדשות מסנן חלקיקים DPF",
        "airbag reset" to "איפוס כרית אוויר",
        "a/c relearn" to "כיול מזגן",
        "abs bleeding" to "סינון בלמים ABS",
        "tire size setting" to "הגדרת מידת צמיג",
        "window initialization" to "אתחול חלונות",
        "windows initialization" to "אתחול חלונות",
        "sunroof initialization" to "אתחול גג שמש",
        "clutch adaptation" to "התאמת מצמד",
        "bms reset" to "איפוס ניהול מצבר BMS",
        "battery reset" to "איפוס מצבר",
        "oil reset" to "איפוס שמן",
        "oil service reset" to "איפוס שירות שמן",
        "service reset" to "איפוס שירות",
        "steering angle" to "זווית היגוי",
        "steering angle reset" to "כיול זווית היגוי",
        "parking brake" to "בלם חניה",
        "electronic parking brake" to "בלם חניה חשמלי",
        "epb" to "בלם חניה חשמלי",
        "injector" to "מזרק",
        "immobilizer" to "אימובילייזר",
        "key learning" to "למידת מפתח",
        "speed limit" to "הגבלת מהירות",
        "seat calibration" to "כיול מושב",
        "headlight" to "פנס ראשי",
        "headlamp" to "פנס ראשי",
        "suspension" to "מתלים",
        "control reset" to "איפוס בקרה",
        "ecu reset" to "איפוס יחידת בקרה",
        "control unit" to "יחידת בקרה",
        "ecu" to "יחידת בקרה",
        "srs" to "כריות אוויר (SRS)",
        "abs" to "מערכת בלמים ABS",
        "eeprom" to "זיכרון EEPROM",
        "eprom" to "זיכרון EPROM",
        "rom" to "זיכרון ROM",
        "activate" to "הפעלה",
        "activation" to "הפעלה",
        "matching" to "התאמה",
        "learning" to "למידה",
        "registration" to "רישום",
        "replacement" to "החלפה",
        "version" to "גרסה",
        "information" to "מידע",
        "calibration" to "כיול",
        "coding" to "קידוד",
        "adaptation" to "התאמה",
        "reset" to "איפוס",
        "initialization" to "אתחול",
        "settings" to "הגדרות",
        "setting" to "הגדרה",
        // רכיבי רכב כלליים
        "battery" to "מצבר",
        "brake" to "בלם",
        "engine" to "מנוע",
        "transmission" to "תיבת הילוכים",
        "gearbox" to "תיבת הילוכים",
        "clutch" to "מצמד",
        "suspension" to "מתלים",
        "mileage" to "קילומטראז'",
        "odometer" to "מד מרחק",
        "fuel" to "דלק",
        "coolant" to "נוזל קירור",
        "temperature" to "טמפרטורה",
        "pressure" to "לחץ",
        "voltage" to "מתח",
        "speed" to "מהירות",
        "rpm" to "סל\"ד",
        "gear" to "הילוך",
        "sensor" to "חישן",
        "valve" to "שסתום",
        "pump" to "משאבה",
        "turbo" to "טורבו",
        "catalyst" to "ממיר קטליטי",
        "exhaust" to "פליטה"
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
