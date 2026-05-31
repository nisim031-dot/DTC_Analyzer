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
