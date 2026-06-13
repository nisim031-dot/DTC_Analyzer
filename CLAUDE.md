# DTC Analyzer — Project Context

## מה הפרויקט עושה
כלי Python לניתוח קודי DTC (תקלות רכב) עם פלט בעברית.
מקבל רשימת קודי DTC מסריקת רכב, מנתח לפי בסיס ידע, ומפיק:
- דוח טקסט: `dtc_report_analysis.txt`
- דוח HTML: `dtc_report_analysis.html` (נפתח אוטומטית בדפדפן)

## ארכיטקטורה
- `DTCEntry` — נתוני DTC גולמיים (code, system, description_en, status)
- `DTCKnowledge` — בסיס ידע: severity (RED/YELLOW/GREEN), הסבר בעברית, סיבות, פעולות
- `KNOWLEDGE_BASE` — מילון `{קוד: DTCKnowledge}`
- `RAW_DTCS` — הקודים שנסרקו מהרכב
- `analyze()` → ממפה DTCEntry ל-DTCKnowledge, ממיין לפי חומרה
- `stats()` → ספירות RED/YELLOW/GREEN
- `render()` → פלט טקסט
- `render_html()` → פלט HTML מלא עם עיצוב RTL

## רכב נוכחי
Hyundai IONIQ Hybrid (AE HEV), VIN: KMHC751CGLU195912, בעלים: Nisim

## שפה
כל הפלטים ל-end-user בעברית (RTL). קוד עצמו באנגלית.

## הרצה
```
python dtc_analyzer.py
```

---

# xToolTranslator — אפליקציית אנדרואיד (תת-פרויקט)

## מה זה
אפליקציית Android (Kotlin) שמרחפת מעל אפליקציית הסורק xTool D7BT כבועה צפה 🌐,
לוכדת מסך, מזהה טקסט (ML Kit OCR) ומתרגמת אנגלית→עברית בזמן אמת.
מטרה: לתרגם קודי DTC ונתוני חיישנים שמופיעים באפליקציית הסורק.

## מבנה (`xToolTranslator/`)
- `MainActivity.kt` — בקשת 3 הרשאות (Overlay, Accessibility, MediaProjection) + הפעלת השירות
- `LiveTranslateService.kt` — Foreground Service: לכידת מסך מתמשכת (VirtualDisplay+ImageReader, ויסות 1.2s) → OCR → תרגום → בועה
- `FloatingBubble.kt` — אובייקט הבועה הצפה (TYPE_APPLICATION_OVERLAY), פאנל RTL, גרירה
- `HybridReaderService.kt` — AccessibilityService שקורא תפריטים/כפתורים ישירות מ-Views
- `TranslationEngine.kt` — מנוע ML Kit Translate EN→HE אופליין + cache
- ספריות: `com.google.mlkit:text-recognition` + `:translate`
- `minSdk 26`, `compileSdk 34`, package `com.xtool.translator`

## בנייה (בסביבת הענן הזו)
```
cd xToolTranslator
export ANDROID_HOME=/opt/android-sdk   # SDK כבר מותקן בקונטיינר
gradle assembleDebug --no-daemon
# פלט: app/build/outputs/apk/debug/app-debug.apk
```
ABI נשלט ב-`app/build.gradle.kts` דרך `ndk.abiFilters`.

## תובנות התקנה קריטיות (הטאבלט של Nisim)
- הטאבלט: **xTool D7BT, Android 10, מעבד 32-ביט (armeabi-v7a)**.
- "אפשרויות מפתחים" **נעולות** ביצרן → אי אפשר adb/USB debugging.
  מתקינים ע"י העברת APK לטאבלט + לחיצה עליו במנהל קבצים ("אפשר ממקור זה").
- הצ'אט **חוסם הורדת `.apk`** וגם קבצים >~16MB → שולחים כ-**ZIP** קטן.
- בנייה ל-**armeabi-v7a בלבד** = פותר הכל: רץ על כל ARM (גם 64-ביט),
  וקובץ קטן (~23MB APK / ~14MB ZIP). בנייה ל-arm64 בלבד נכשלת בהתקנה ("האפליקציה לא הותקנה").
- גיבוי הורדה: APK ב-`releases/xtool_translator.apk` בריפו → הורדה ישירה מדפדפן הטאבלט.

## מצב נוכחי — v6 מותקן ועובד על הטאבלט ✅
בנוי armeabi-v7a בלבד (~23MB APK / ~14MB ZIP). מותקן ופועל.
APK מעודכן ב-`releases/xtool_translator.apk` (נדחף בכל גרסה).
הורדה: דרך ZIP בצ'אט (עובד). הריפו פרטי → קישור דפדפן דורש התחברות;
המשתמש בחר להישאר עם ZIP בצ'אט (לא להפוך לציבורי).

## מה כבר מומש (v1→v6)
- בועה צפה + לכידת מסך (MediaProjection) + OCR (ML Kit) + תרגום EN→HE אופליין
- **בסיס ידע** (`Knowledge.kt`): ~40 קודי DTC (KIA/Hyundai + OBD-II גנריים) + מילון ~120 מונחי סורק
- **תרגום היברידי** (`TranslationEngine.kt`): DTC→הסבר+חומרה, מילון→מונח מקצועי, אחרת→ML Kit. דגל `known` לתרגום ודאי.
- **שכבת AR** (`OverlayLayer.kt`): תרגום צמוד מעל הטקסט המקורי, חלון לא-מגיב למגע. מציג **רק מונחים מוכרים** (known) כדי לא להציף ברעש OCR. dedup למניעת הבהוב.
- **פאנל צד** (`FloatingBubble.kt`): מציג הכל, צבע בועה לפי חומרה (🔴/🟡/🟢)
- **מצב חכם** (`LiveTranslateService.kt`): "תרגם רק כשהמסך מתייצב" — חתימת 8x8, debounce ~0.6ש', render אטומי. זיהוי שינוי פחות רגיש (כדי שריצוד רקע לא יהבהב).
- **התראת רטט+צליל** (`Alerts.kt`) על קוד DTC אדום

## תקלות שטופלו (בקשת תיקון = חזרה לכאן)
- arm64 בלבד נכשל בהתקנה → armeabi-v7a
- כתוביות AR "תקועות" 5 דק' / מהבהבות → stabilize-then-translate + dedup + רגישות נמוכה
- צפיפות/ג'יבריש → AR מציג רק `known`

## שלבים הבאים (אופציונלי)
- כוונון רגישות זיהוי שינוי / debounce אם צריך
- עוד קודי DTC / מונחים לפי מסכים שהמשתמש שולח
- מצב "לחיצה לתרגום", יומן היסטוריה, שמירת צילום מסך (לא מומשו)
- אייקון אמיתי לאפליקציה
