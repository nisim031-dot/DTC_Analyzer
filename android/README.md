# בוט תרגום עברית ל-XTOOL D7BT

אפליקציית אנדרואיד שמציגה **תרגום עברי בזמן אמת מעל ממשק האבחון של XTOOL D7BT**.
היא קוראת את הטקסט מהמסך (שירות Accessibility, ובמקרה הצורך נפילה ל-OCR),
מתרגמת לעברית (מילון DTC קבוע + ML Kit אופליין), ומציירת תוויות RTL מעל הטקסט המקורי.

> ⚠️ **תוכנת ה-XTOOL עצמה סגורה ולא ניתנת לשינוי.** זוהי אפליקציה **חיצונית** נפרדת
> שמותקנת על המכשיר (sideload) ופועלת *מעל* תוכנת האבחון — לא בתוכה.

## דרישות בנייה
- Android Studio (או Android SDK + `platform-34`, `build-tools`)
- JDK 17+
- `minSdk 29 / targetSdk 29` (תואם ל-D7BT, אנדרואיד 10)

## בנייה
```bash
cd android
./gradlew assembleDebug
# הפלט: app/build/outputs/apk/debug/app-debug.apk
```

## עדכון מילון ה-DTC
המילון נוצר מ-`KNOWLEDGE_BASE` שבקובץ `dtc_analyzer.py` שבשורש הפרויקט:
```bash
python tools/export_kb.py   # מרענן את android/app/src/main/assets/dtc_dictionary.json
```
מונחי ממשק כלליים נמצאים ב-`app/src/main/assets/term_glossary.json` (עריכה ידנית).

## ⚠️ Phase 0 — בדיקת היתכנות על D7BT אמיתי (חובה לפני הכל)
ה-D7BT הוא טאבלט נעול. לפני שמסתמכים על האפליקציה צריך לאמת על המכשיר:
1. **Sideload** — האם ניתן להתקין APK? (`adb install app-debug.apk`, או מנהל קבצים + "מקורות לא ידועים").
2. **Accessibility** — האם "תרגום XTOOL" מופיע בהגדרות נגישות וניתן להפעלה?
3. **Overlay** — האם ה-overlay נשאר גלוי *מעל תוכנת האבחון*? (הסיכון הגבוה ביותר.)
4. **חשיפת טקסט** — כשתוכנת xtool פתוחה, האם השירות קורא צמתי טקסט? אם לא → נסמך על OCR.
5. **MediaProjection** — האם לכידת המסך עובדת ולא קורסת בזיכרון (2GB)?

מסך האונבורדינג של האפליקציה משמש גם כ-probe: אם הצלחת להתקין, להעניק נגישות,
ולראות overlay — שערי ההיתכנות עברו. **אם sideload או overlay חסומים — הגישה אינה ישימה**,
ויש לחזור לפייפליין החיצוני (תרגום דוח ה-PDF דרך `garage_bot_v2.py`).

## אונבורדינג (חד-פעמי, על Wi-Fi)
פתח את האפליקציה ועבור על השלבים: הפעלת נגישות → הצגה מעל אפליקציות → לכידת מסך →
**הורדת מודל עברית** (חובה אינטרנט פעם אחת; אחריו הכל אופליין) → ביטול אופטימיזציית סוללה.

## מבנה
```
app/src/main/java/com/garage/xtooltranslate/
  service/XtoolAccessibilityService.kt  — מקור הטקסט + אירוח overlay + תזמור
  overlay/OverlayView.kt + OverlayController.kt — ציור עברית RTL
  capture/ScreenCaptureService.kt + OcrEngine.kt — נפילת OCR
  translate/  — DtcDictionary, MlKitTranslator, TranslationCache, TranslationRepository
  ui/OnboardingActivity.kt — אשף הרשאות בעברית
```

## מה לא נבדק כאן
הקוד נכתב אך **לא קומפל בסביבת הפיתוח הזו** (אין Android SDK). יש לבנות ולבדוק
על המכשיר/אמולטור. בדיקות שחייבות מכשיר אמיתי: כל Phase 0, דיוק קואורדינטות,
ביצועי OCR ב-1.5GHz/2GB, והאם xtool חושפת צמתי טקסט.
