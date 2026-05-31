"""
DTC Report Analyzer - Hyundai IONIQ Hybrid (AE HEV)
מנתח דוח תקלות רכב עם רמות חומרה והסבר מלא בעברית
"""

from dataclasses import dataclass
from typing import List
from datetime import datetime


# ─────────────────────────────────────────────
# DATA STRUCTURES
# ─────────────────────────────────────────────

@dataclass
class DTCEntry:
    code: str
    system: str
    description_en: str
    status: str


@dataclass
class DTCKnowledge:
    code: str
    severity: str        # RED / YELLOW / GREEN
    title_he: str
    explanation_he: str
    cause_he: str
    action_he: str
    safety_impact: str
    manual_he: str = ""  # נתונים מהמנואל הרשמי


# ─────────────────────────────────────────────
# VEHICLE DATA  (מתוך הדוח שנסרק)
# ─────────────────────────────────────────────

VEHICLE_INFO = {
    "make":        "HYUNDAI",
    "model":       "IONIQ Hybrid (AE HEV)",
    "vin":         "KMHC751CGLU195912",
    "plate":       "סיון",
    "mileage":     "148,700 ק\"מ",
    "scan_date":   "2025-10-07",
    "scan_tool":   "XTOOL V5.7.48_11.65",
    "owner":       "Nisim",
}

RAW_DTCS: List[DTCEntry] = [
    DTCEntry("C136081", "SCC/AEB",                "ESP irreversible error",    "History"),
    DTCEntry("C165208", "ESP/AHB",                "CAN Signal Error EPB",      "History"),
    DTCEntry("C241701", "Electric Parking Brake",  "Motor short or open - RH", "History"),
    DTCEntry("B28CD13", "E-Call (EUROPE)",         "eCall modem antenna open",  "History"),
    DTCEntry("B28CD13", "E-Call (RUSSIA)",         "eCall modem antenna open",  "History"),
]


# ─────────────────────────────────────────────
# KNOWLEDGE BASE  –  הסבר מלא לכל קוד DTC
# ─────────────────────────────────────────────

KNOWLEDGE_BASE: dict = {

    "C136081": DTCKnowledge(
        code="C136081",
        severity="RED",
        title_he="שגיאה בלתי הפיכה במערכת יציבות ESP – דרך SCC/AEB",

        explanation_he=(
            "מערכת ה-SCC (Smart Cruise Control) וה-AEB (Automatic Emergency Braking)\n"
            "קיבלו דיווח על שגיאה קריטית ממערכת יציבות הנסיעה (ESP).\n"
            "המילה 'irreversible' מציינת שה-ESP זיהה כשל פנימי שדורש\n"
            "אתחול מחדש של המערכת — ייתכן לאחר אירוע בלימה חריג,\n"
            "חישן גלגל פגום, או תקלה ב-ABS Modulator."
        ),
        cause_he=(
            "• חישן מהירות גלגל (Wheel Speed Sensor) תקול או מנותק\n"
            "• תקלה ב-ABS/ESP Hydraulic Modulator\n"
            "• נפילת מתח חולפת שגרמה ל-ESP להיכנס למצב נעילה\n"
            "• עדכון תוכנה לא שלם ב-ECU של ה-ESP\n"
            "• בלימת חירום קיצונית שהפעילה מנגנון נעילה מגן"
        ),
        action_he=(
            "1. קרא מחדש את ה-DTC ובדוק אם פעיל (Active) או היסטורי.\n"
            "2. מחק קודים, נסע, ובדוק אם חוזר.\n"
            "3. בדוק את 4 חישני מהירות הגלגלים.\n"
            "4. בצע Live Data לערכי מהירות הגלגלים בנסיעה.\n"
            "5. אם חוזר — שקול שיפוץ/החלפת ABS Modulator."
        ),
        safety_impact="גבוה — ESP/AEB עשויים לא לפעול כראוי במצב חירום",
        manual_he=(
            "📖 מנואל רשמי — בדיקת חישן מהירות גלגל:\n"
            "• מדוד מתח בין טרמינל החישן לגוף הרכב עם נגד 100Ω בסדרה\n"
            "• V_Low תקין: 0.59V – 0.84V\n"
            "• V_High תקין: 1.18V – 1.68V\n"
            "• תדר תקין: 1 – 2,500 Hz\n"
            "• מומנט הידוק חישן קדמי: 7.8 – 11.8 N.m\n"
            "• החישן האחורי משולב ב-Hub Bearing — מומנט: 88.2 – 107.8 N.m"
        ),
    ),

    "C165208": DTCKnowledge(
        code="C165208",
        severity="YELLOW",
        title_he="שגיאת תקשורת CAN בין ESP לבלם החניה החשמלי (EPB)",
        explanation_he=(
            "מערכת ה-ESP/AHB לא הצליחה לקבל את אות ה-CAN Bus\n"
            "מיחידת בלם החניה החשמלי (EPB).\n"
            "CAN Bus הוא הרשת הפנימית שדרכה מדברות כל מערכות הרכב.\n"
            "שגיאה זו מציינת נתק תקשורת — לא בהכרח כשל פיזי עצמאי.\n"
            "סביר שהיא תוצר ישיר של תקלת מנוע ה-EPB (C241701)."
        ),
        cause_he=(
            "• תוצר ישיר של C241701 — ה-EPB כבה ולכן ה-ESP לא 'שומע' אותו\n"
            "• חיבור CAN Bus רופף בין ESP ל-EPB\n"
            "• חוט פגום/חתוך בקו ה-CAN\n"
            "• בעיית מתח/הארקה ביחידת ה-EPB\n"
            "• תקלה ב-Gateway המרכזי של הרכב (נדיר)"
        ),
        action_he=(
            "1. תקן תחילה את C241701 (EPB Motor) — קוד זה עשוי להיעלם מעצמו.\n"
            "2. בדוק מתח ספקה ליחידת ה-EPB.\n"
            "3. בדוק רצף ושלמות חיבורי CAN High/Low.\n"
            "4. אם נמשך לאחר תיקון ה-EPB — בצע אבחון CAN Bus מלא."
        ),
        safety_impact="בינוני — בלם חניה עשוי לא להשתחרר/להינעל אוטומטית כצפוי",
        manual_he=(
            "📖 מנואל רשמי — תלוי ישירות ב-C241701:\n"
            "• המנואל מציין: EPB System → CAN Failure כשרצף תקלות\n"
            "• תקן את C241701 — תקלה זו תיעלם אוטומטית"
        ),
    ),

    "C241701": DTCKnowledge(
        code="C241701",
        severity="RED",
        title_he="קצר/נתק במנוע בלם החניה החשמלי — צד ימין אחורי (EPB RH)",
        explanation_he=(
            "יחידת בקרת ה-EPB זיהתה שמנוע הבלם החשמלי בצד ימין אחורי\n"
            "נמצא במצב קצר (short circuit) או מעגל פתוח (open circuit).\n"
            "כלומר: כבל מנותק, מנוע שרוף, או קצר לשלדה.\n"
            "תקלה זו עלולה למנוע שחרור או הפעלה של בלם החניה —\n"
            "וגוררת את שתי התקלות האחרות (C136081, C165208)."
        ),
        cause_he=(
            "• מנוע EPB שרוף בצד ימין אחורי\n"
            "• חיבורים חשמליים חלודים/מנותקים למנוע\n"
            "• כבל שנחתך/נלחץ (שכיח לאחר עבודה בתחתית הרכב)\n"
            "• קצר לשלדה בחוט החיובי של המנוע\n"
            "• בעיה ב-EPB ECU עצמו (נדיר)"
        ),
        action_he=(
            "1. בדוק חוטים וחיבורים למנוע EPB בגלגל אחורי ימין.\n"
            "2. מדוד התנגדות המנוע (תקין: 1-5 אוהם).\n"
            "   0 אוהם = קצר | אין סוף = מעגל פתוח\n"
            "3. בדוק האם מתח מגיע למחבר בעת פקודת הפעלה.\n"
            "4. אם המנוע תקול — החלף קליפר EPB שלם (המנוע משולב בקליפר).\n"
            "5. לאחר תיקון — בצע כיול EPB (Calibration) בדיאגנוסטיקה."
        ),
        safety_impact="גבוה — בלם חניה עלול שלא לפעול בצד ימין, סכנה בחניה במדרון",
        manual_he=(
            "📖 מנואל רשמי — פינוי חשמלי ב-EPB Control Module (40 פינים):\n"
            "• פין 35 = EPB Actuator RH (+)\n"
            "• פין 37 = EPB Actuator RH (−)\n"
            "• בדוק התנגדות בין פינים 35↔37: תקין = 1–5 אוהם\n"
            "  (0 אוהם = קצר | ∞ = מעגל פתוח)\n"
            "\n"
            "כיול חובה לאחר החלפת קליפר (Bedding in Process):\n"
            "1. הבע מנוע\n"
            "2. דרוך בלם פדל 2× תוך 10 שניות — השאר לחוץ\n"
            "3. הפעל מתג EPB 4× + 3 שחרורים תוך 10 שניות\n"
            "4. בצע 6 בלימות דינמיות מ-30–35 קמ\"ש עד עצירה\n"
            "5. לאחר התקנת מודול — לחץ מתג EPB 3× לאימות תקינות"
        ),
    ),

    "B28CD13": DTCKnowledge(
        code="B28CD13",
        severity="GREEN",
        title_he="אנטנת מודם eCall מנותקת / מעגל פתוח",
        explanation_he=(
            "מערכת ה-eCall (חיוג חירום אוטומטי לרשויות באירופה) זיהתה\n"
            "שאנטנת ה-GSM/LTE שלה מנותקת (open circuit).\n"
            "eCall היא מערכת חובה של האיחוד האירופי שמחייגת אוטומטית\n"
            "לשירותי חירום בתאונה. בישראל המערכת אינה פעילה ברשת.\n"
            "תקלה זו אינה משפיעה על נסיעת הרכב כלל.\n"
            "מדווחת פעמיים (EUROPE + RUSSIA) — אותה בעיה פיזית אחת."
        ),
        cause_he=(
            "• אנטנת eCall מנותקת (לרוב ממוקמת בתקרה/שמשה קדמית)\n"
            "• כבל אנטנה נחתך או נלחץ\n"
            "• יחידת ה-eCall עצמה תקולה\n"
            "• לרכבים שנמכרו לישראל — המערכת לא הופעלה מלכתחילה"
        ),
        action_he=(
            "1. בישראל — ניתן להתעלם, אין השפעה מעשית.\n"
            "2. לתיקון: בדוק חיבור פיזי של כבל האנטנה ליחידת ה-eCall.\n"
            "3. מדוד המשכיות כבל האנטנה.\n"
            "4. שים לב: מדווח פעמיים (EUROPE + RUSSIA) — תיקון אחד יפתור שניהם."
        ),
        safety_impact="נמוך — ללא השפעה על נסיעה, מערכת חירום בלבד",
        manual_he=(
            "📖 מנואל רשמי — eCall Unit (ממוקם בקונסול המרכזי):\n"
            "• אם LED אדום דולק: בדוק אנטנת eCall + אנטנת גג\n"
            "• הסרה: נתק מינוס בטרייה → הסר כיסויי Crash Pad →\n"
            "  נתק קונקטור יחידה (A) + קונקטורי אנטנה (B) → שחרר בורגים\n"
            "• לאחר התקנה — חובה לבצע 'eCall Parameter Download' בכלי GDS\n"
            "• בישראל: המערכת לא פעילה ברשת — ניתן להתעלם"
        ),
    ),
}


# ─────────────────────────────────────────────
# SEVERITY CONFIG
# ─────────────────────────────────────────────

SEVERITY_DISPLAY = {
    "RED":    {"label": "אדום  🔴", "note": "דחוף – טיפול מיידי",    "line": "="},
    "YELLOW": {"label": "צהוב  🟡", "note": "בינוני – טיפול בהקדם", "line": "-"},
    "GREEN":  {"label": "ירוק  🟢", "note": "נמוך – ניתן לתכנן",    "line": "."},
}

SEVERITY_ORDER = {"RED": 0, "YELLOW": 1, "GREEN": 2}

W = 70  # report width


# ─────────────────────────────────────────────
# HELPERS
# ─────────────────────────────────────────────

def hline(char="="):
    return char * W


def section_header(text, char="="):
    return f"{char*2} {text} {char * max(0, W - len(text) - 4)}"


# ─────────────────────────────────────────────
# ANALYSIS
# ─────────────────────────────────────────────

def analyze(dtcs: List[DTCEntry]) -> List[tuple]:
    results = []
    for dtc in dtcs:
        k = KNOWLEDGE_BASE.get(dtc.code) or DTCKnowledge(
            code=dtc.code,
            severity="YELLOW",
            title_he=f"קוד לא מוכר: {dtc.code}",
            explanation_he=f"תיאור מקורי: {dtc.description_en}",
            cause_he="לא ידוע – נדרש מחקר נוסף",
            action_he="פנה לטכנאי Hyundai מוסמך עם הקוד המדויק.",
            safety_impact="לא ידוע",
        )
        results.append((dtc, k))
    results.sort(key=lambda x: SEVERITY_ORDER[x[1].severity])
    return results


def stats(results: List[tuple]) -> dict:
    counts = {"RED": 0, "YELLOW": 0, "GREEN": 0}
    codes = set()
    for dtc, k in results:
        counts[k.severity] += 1
        codes.add(dtc.code)
    return {"counts": counts, "unique": len(codes), "total": len(results)}


# ─────────────────────────────────────────────
# RENDER
# ─────────────────────────────────────────────

def render(vehicle: dict, results: List[tuple], s: dict) -> str:
    out = []

    # ── Header ────────────────────────────────
    out += [
        hline("="),
        section_header("דוח ניתוח תקלות רכב – Hyundai IONIQ Hybrid"),
        hline("="),
        "",
        f"  יצרן         : {vehicle['make']} {vehicle['model']}",
        f"  VIN          : {vehicle['vin']}",
        f"  לוחית        : {vehicle['plate']}",
        f"  קילומטראז'   : {vehicle['mileage']}",
        f"  תאריך סריקה  : {vehicle['scan_date']}",
        f"  כלי סריקה    : {vehicle['scan_tool']}",
        "",
    ]

    # ── Summary ───────────────────────────────
    out += [
        hline("-"),
        section_header("סיכום ממצאים", "-"),
        hline("-"),
        "",
        f"  סה\"כ תקלות שנמצאו  : {s['total']}   (קודים ייחודיים: {s['unique']})",
        f"  🔴 דחוף  (אדום)    : {s['counts']['RED']}",
        f"  🟡 בינוני (צהוב)   : {s['counts']['YELLOW']}",
        f"  🟢 נמוך  (ירוק)    : {s['counts']['GREEN']}",
        "",
        "  * כל הקודים בסטטוס HISTORY — התרחשו בעבר, אינם פעילים כרגע.",
        "    מחק קודים, נסע, ובדוק אם חוזרים.",
        "",
    ]

    # ── Per-DTC ───────────────────────────────
    for idx, (dtc, k) in enumerate(results, 1):
        sev = SEVERITY_DISPLAY[k.severity]
        b = sev["line"]

        out += [
            hline(b),
            section_header(
                f"תקלה {idx}/{s['total']}  |  {sev['label']}  |  {sev['note']}", b
            ),
            hline(b),
            "",
            f"  קוד DTC      : {dtc.code}",
            f"  מערכת        : {dtc.system}",
            f"  תיאור מקורי  : {dtc.description_en}",
            f"  סטטוס         : {dtc.status}",
            f"  כותרת         : {k.title_he}",
            "",
            "  >> מה זה אומר?",
        ]
        for ln in k.explanation_he.split("\n"):
            out.append(f"     {ln}")

        out += ["", "  >> סיבות אפשריות:"]
        for ln in k.cause_he.split("\n"):
            out.append(f"     {ln}")

        out += ["", "  >> מה לעשות?"]
        for ln in k.action_he.split("\n"):
            out.append(f"     {ln}")

        out += ["", f"  >> השפעה על בטיחות: {k.safety_impact}"]

        if k.manual_he:
            out += [""]
            for ln in k.manual_he.split("\n"):
                out.append(f"     {ln}")

        out += [""]

    # ── Action Plan ───────────────────────────
    out += [
        hline("="),
        section_header("תוכנית פעולה מומלצת לפי סדר עדיפויות"),
        hline("="),
        "",
        "  1. 🔴 C241701  — בדוק / החלף קליפר EPB אחורי ימין.",
        "                    זוהי הסיבה הסבירה לשרשרת כל התקלות.",
        "",
        "  2. 🔴 C136081  — לאחר תיקון ה-EPB, מחק קודים ונסע.",
        "                    אם חוזר — בדוק חישני מהירות גלגלים.",
        "",
        "  3. 🟡 C165208  — צפוי להיעלם לאחר תיקון ה-EPB.",
        "                    אם נמשך — בדוק חיבורי CAN Bus.",
        "",
        "  4. 🟢 B28CD13  — אנטנת eCall. לא דחוף, ללא השפעה על נסיעה.",
        "                    תיקון אחד יפתור גם EUROPE וגם RUSSIA.",
        "",
        hline("="),
        f"  דוח הופק: {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        "",
        f"  🔧 מוסך {vehicle['owner']}",
        hline("="),
    ]

    return "\n".join(out)


# ─────────────────────────────────────────────
# HTML RENDER
# ─────────────────────────────────────────────

SEV_COLOR = {"RED": "#c0392b", "YELLOW": "#d4a017", "GREEN": "#27ae60"}
SEV_BG    = {"RED": "#fff0f0", "YELLOW": "#fffbe6", "GREEN": "#f0fff4"}

def render_html(vehicle: dict, results: List[tuple], s: dict) -> str:
    now = datetime.now().strftime("%Y-%m-%d %H:%M")

    def esc(t): return t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")

    rows = ""
    for idx, (dtc, k) in enumerate(results, 1):
        color  = SEV_COLOR[k.severity]
        bg     = SEV_BG[k.severity]
        label  = {"RED":"🔴 אדום","YELLOW":"🟡 צהוב","GREEN":"🟢 ירוק"}[k.severity]
        note   = {"RED":"דחוף – טיפול מיידי","YELLOW":"בינוני – טיפול בהקדם","GREEN":"נמוך – ניתן לתכנן"}[k.severity]

        def lines(txt): return "<br>".join(esc(ln) for ln in txt.split("\n"))

        rows += f"""
        <div class="dtc-card" style="border-right:6px solid {color}; background:{bg};">
          <div class="dtc-header" style="color:{color};">
            תקלה {idx}/{s['total']} &nbsp;|&nbsp; {label} &nbsp;|&nbsp; {note}
          </div>
          <table class="info-table">
            <tr><td>קוד DTC</td><td><b>{esc(dtc.code)}</b></td></tr>
            <tr><td>מערכת</td><td>{esc(dtc.system)}</td></tr>
            <tr><td>תיאור מקורי</td><td>{esc(dtc.description_en)}</td></tr>
            <tr><td>סטטוס</td><td>{esc(dtc.status)}</td></tr>
            <tr><td>כותרת</td><td><b>{esc(k.title_he)}</b></td></tr>
          </table>
          <div class="section-title">מה זה אומר?</div>
          <div class="section-body">{lines(k.explanation_he)}</div>
          <div class="section-title">סיבות אפשריות:</div>
          <div class="section-body">{lines(k.cause_he)}</div>
          <div class="section-title">מה לעשות?</div>
          <div class="section-body">{lines(k.action_he)}</div>
          <div class="safety">⚠️ השפעה על בטיחות: {esc(k.safety_impact)}</div>
          {"" if not k.manual_he else f'<div class="manual-box"><div class="manual-title">📖 מנואל רשמי Hyundai</div>{lines(k.manual_he)}</div>'}
        </div>"""

    action_items = [
        ("RED",    "C241701", "בדוק / החלף קליפר EPB אחורי ימין. זוהי הסיבה הסבירה לשרשרת כל התקלות."),
        ("RED",    "C136081", "לאחר תיקון ה-EPB, מחק קודים ונסע. אם חוזר — בדוק חישני מהירות גלגלים."),
        ("YELLOW", "C165208", "צפוי להיעלם לאחר תיקון ה-EPB. אם נמשך — בדוק חיבורי CAN Bus."),
        ("GREEN",  "B28CD13", "אנטנת eCall. לא דחוף, ללא השפעה על נסיעה. תיקון אחד יפתור גם EUROPE וגם RUSSIA."),
    ]
    action_html = ""
    for i, (sev, code, text) in enumerate(action_items, 1):
        icon = {"RED":"🔴","YELLOW":"🟡","GREEN":"🟢"}[sev]
        action_html += f'<li><b style="color:{SEV_COLOR[sev]}">{icon} {code}</b> — {esc(text)}</li>'

    return f"""<!DOCTYPE html>
<html lang="he" dir="rtl">
<head>
<meta charset="UTF-8">
<title>דוח תקלות – {esc(vehicle['model'])}</title>
<style>
  body {{ font-family: Arial, sans-serif; direction: rtl; background:#f4f6f8;
          color:#222; margin:0; padding:20px; font-size:15px; }}
  .page {{ max-width:860px; margin:auto; background:#fff; border-radius:10px;
           box-shadow:0 2px 12px #0002; padding:30px; }}
  h1 {{ font-size:22px; color:#1a3a5c; border-bottom:3px solid #1a3a5c;
        padding-bottom:8px; margin-bottom:18px; }}
  .vehicle-table td {{ padding:3px 10px; }}
  .vehicle-table td:first-child {{ color:#555; font-weight:bold; width:140px; }}
  .summary {{ display:flex; gap:16px; margin:16px 0; flex-wrap:wrap; }}
  .badge {{ padding:8px 18px; border-radius:20px; font-weight:bold; font-size:14px; }}
  .badge-red    {{ background:#fde; color:#c0392b; }}
  .badge-yellow {{ background:#fff3cd; color:#856404; }}
  .badge-green  {{ background:#d4edda; color:#155724; }}
  .dtc-card {{ border-radius:8px; padding:16px 20px; margin:18px 0;
               border-right:6px solid #ccc; }}
  .dtc-header {{ font-size:16px; font-weight:bold; margin-bottom:10px; }}
  .info-table {{ border-collapse:collapse; margin-bottom:10px; width:100%; }}
  .info-table td {{ padding:3px 8px; vertical-align:top; }}
  .info-table td:first-child {{ color:#555; font-weight:bold; width:130px; white-space:nowrap; }}
  .section-title {{ font-weight:bold; color:#1a3a5c; margin:10px 0 4px; }}
  .section-body {{ padding-right:12px; line-height:1.7; }}
  .safety {{ margin-top:10px; font-weight:bold; color:#7d3c00; }}
  .manual-box {{ margin-top:12px; background:#f5f0ff; border-right:4px solid #6c3483;
                 border-radius:6px; padding:10px 14px; font-size:13px; color:#2c1654; }}
  .manual-box .manual-title {{ font-weight:bold; color:#6c3483; margin-bottom:6px; }}
  .action-plan {{ background:#eaf4ff; border-radius:8px; padding:16px 20px; margin-top:24px; }}
  .action-plan h2 {{ color:#1a3a5c; font-size:17px; margin:0 0 12px; }}
  .action-plan ol {{ padding-right:20px; line-height:2; margin:0; }}
  .footer {{ text-align:center; color:#999; font-size:12px; margin-top:24px; }}
  .stamp {{ text-align:center; margin-top:18px; padding:12px;
            border:2px solid #1a3a5c; border-radius:8px; display:inline-block;
            color:#1a3a5c; font-weight:bold; font-size:16px; letter-spacing:1px; }}
  .stamp-wrap {{ text-align:center; margin-top:16px; }}
  @media print {{ body {{ background:white; }} .page {{ box-shadow:none; }} }}
</style>
</head>
<body>
<div class="page">
  <h1>🚗 דוח ניתוח תקלות רכב – {esc(vehicle['make'])} {esc(vehicle['model'])}</h1>
  <table class="vehicle-table">
    <tr><td>VIN</td><td>{esc(vehicle['vin'])}</td></tr>
    <tr><td>לוחית</td><td>{esc(vehicle['plate'])}</td></tr>
    <tr><td>קילומטראז'</td><td>{esc(vehicle['mileage'])}</td></tr>
    <tr><td>תאריך סריקה</td><td>{esc(vehicle['scan_date'])}</td></tr>
    <tr><td>כלי סריקה</td><td>{esc(vehicle['scan_tool'])}</td></tr>
  </table>

  <div class="summary">
    <span class="badge badge-red">🔴 דחוף: {s['counts']['RED']}</span>
    <span class="badge badge-yellow">🟡 בינוני: {s['counts']['YELLOW']}</span>
    <span class="badge badge-green">🟢 נמוך: {s['counts']['GREEN']}</span>
    <span class="badge" style="background:#eee;color:#333;">סה"כ: {s['total']} תקלות ({s['unique']} ייחודיות)</span>
  </div>
  <p style="color:#555;font-size:13px;">* כל הקודים בסטטוס HISTORY — התרחשו בעבר, אינם פעילים כרגע.</p>

  {rows}

  <div class="action-plan">
    <h2>📋 תוכנית פעולה מומלצת לפי סדר עדיפויות</h2>
    <ol>{action_html}</ol>
  </div>

  <div class="footer">דוח הופק: {now}</div>
  <div class="stamp-wrap">
    <div class="stamp">🔧 מוסך {esc(vehicle['owner'])}</div>
  </div>
</div>
</body>
</html>"""


# ─────────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────────

def main():
    import webbrowser, sys
    sys.stdout.reconfigure(encoding="utf-8")
    results = analyze(RAW_DTCS)
    s = stats(results)
    report = render(VEHICLE_INFO, results, s)

    print(report)

    txt_path = r"E:\NISIM_PROJECTS\DTC_Analyzer\dtc_report_analysis.txt"
    with open(txt_path, "w", encoding="utf-8") as f:
        f.write(report)
    print(f"\n  [שמור] טקסט: {txt_path}")

    html_path = r"E:\NISIM_PROJECTS\DTC_Analyzer\dtc_report_analysis.html"
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(render_html(VEHICLE_INFO, results, s))
    print(f"  [שמור] HTML:  {html_path}")
    webbrowser.open(f"file:///{html_path}")
    print("  [פתוח] הדוח נפתח בדפדפן — Ctrl+P להדפסה / שמירה כ-PDF")


if __name__ == "__main__":
    main()
