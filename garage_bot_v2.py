"""
garage_bot_v2.py — בוט מוסך ניסים (גרסה 2)
זרימה: מייל → PDF → חילוץ קודים → KNOWLEDGE_BASE + Gemini → דוח HTML → טלגרם
"""

import os, sys, imaplib, email, re, time, pdfplumber
import telebot
import google.generativeai as genai
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import Chroma
from datetime import datetime

sys.stdout.reconfigure(encoding="utf-8")

# טעינת משתני סביבה מקובץ .env (אם python-dotenv מותקן)
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

# ─────────────────────────────────────────
# ייבוא מ-DTC Analyzer
# ─────────────────────────────────────────
sys.path.insert(0, r'E:\NISIM_PROJECTS\DTC_Analyzer')
from dtc_analyzer import (
    DTCEntry, DTCKnowledge, KNOWLEDGE_BASE,
    analyze, stats, render, render_html,
)

# ─────────────────────────────────────────
# הגדרות
# ─────────────────────────────────────────
TELEGRAM_TOKEN = os.getenv('TELEGRAM_TOKEN')
CHAT_ID        = os.getenv('CHAT_ID')
GEMINI_KEY     = os.getenv('GEMINI_KEY')
GMAIL_USER     = os.getenv('GMAIL_USER')
GMAIL_PASS     = os.getenv('GMAIL_PASS')

BASE_DIR     = r'E:\NISIM_PROJECTS\Nisim Car Diagnostic'
REPORTS_DIR  = os.path.join(BASE_DIR, 'REPORTS_BY_VIN')
VECTOR_DB_DIR = os.path.join(BASE_DIR, 'vector_db_gemini')

genai.configure(api_key=GEMINI_KEY)
bot = telebot.TeleBot(TELEGRAM_TOKEN)


# ─────────────────────────────────────────
# Gemini — רק לקודים שאינם ב-KNOWLEDGE_BASE
# ─────────────────────────────────────────
def analyze_with_gemini(code: str, manual_context: str = "") -> str:
    model = genai.GenerativeModel('gemini-1.5-flash')
    prompt = (
        f"אתה רב בוחן רכב במוסך. נתח את קוד התקלה {code}.\n"
        f"מידע מספר המוסך: {manual_context}\n"
        "כתוב בעברית קצרה ומקצועית: הסבר על התקלה ומה לבדוק."
    )
    try:
        return genai.GenerativeModel('gemini-1.5-flash').generate_content(prompt).text.strip()
    except Exception as e:
        return f"שגיאת AI — בדוק ידנית: {code}"


# ─────────────────────────────────────────
# בניית רשימת DTCEntry + העשרת KNOWLEDGE_BASE
# ─────────────────────────────────────────
def enrich_and_build(codes: list[str], vectorstore) -> list[DTCEntry]:
    """
    לכל קוד: אם קיים ב-KNOWLEDGE_BASE → משתמש בו.
    אחרת → שולח ל-Gemini וממלא ב-KNOWLEDGE_BASE זמנית.
    """
    entries = []
    local_kb = dict(KNOWLEDGE_BASE)  # עותק כדי לא לשנות את המקורי

    for code in codes:
        entries.append(DTCEntry(
            code=code,
            system="סריקה אוטומטית",
            description_en=code,
            status="History",
        ))

        if code not in local_kb:
            manual_context = ""
            if vectorstore:
                try:
                    docs = vectorstore.similarity_search(code, k=1)
                    manual_context = docs[0].page_content if docs else ""
                except Exception:
                    pass
                time.sleep(2)  # מניעת חסימת API

            gemini_text = analyze_with_gemini(code, manual_context)
            local_kb[code] = DTCKnowledge(
                code=code,
                severity="YELLOW",
                title_he=f"קוד לא מוכר: {code}",
                explanation_he=gemini_text,
                cause_he="ניתוח נוצר ע\"י Gemini AI — ראה למעלה",
                action_he="פנה לטכנאי עם הקוד המדויק",
                safety_impact="לא ידוע — בדוק ידנית",
            )

    # analyze() עובד עם KNOWLEDGE_BASE הגלובלי — עדכן אותו זמנית
    KNOWLEDGE_BASE.update(local_kb)
    return entries


# ─────────────────────────────────────────
# עיבוד + שליחת דוח
# ─────────────────────────────────────────
def process_and_send(vin: str, codes: list[str], scan_date: str = ""):
    print(f"\n📄 מכין דוח עבור VIN: {vin}  ({len(codes)} קודים)")

    vehicle = {
        "make":      "HYUNDAI",
        "model":     "IONIQ Hybrid (AE HEV)",
        "vin":       vin,
        "plate":     "—",
        "mileage":   "—",
        "scan_date": scan_date or datetime.now().strftime("%Y-%m-%d"),
        "scan_tool": "XTOOL",
        "owner":     "Nisim",
    }

    vin_dir = os.path.join(REPORTS_DIR, vin)
    os.makedirs(vin_dir, exist_ok=True)

    # אתחול vector DB (אופציונלי)
    vectorstore = None
    try:
        emb = GoogleGenerativeAIEmbeddings(
            model="models/gemini-embedding-001", google_api_key=GEMINI_KEY
        )
        vectorstore = Chroma(persist_directory=VECTOR_DB_DIR, embedding_function=emb)
    except Exception:
        pass

    dtc_entries = enrich_and_build(codes, vectorstore)
    results = analyze(dtc_entries)
    s = stats(results)

    # שמירת קבצים
    html_path = os.path.join(vin_dir, f"DTC_Report_{vin}.html")
    txt_path  = os.path.join(vin_dir, f"DTC_Report_{vin}.txt")

    with open(html_path, "w", encoding="utf-8") as f:
        f.write(render_html(vehicle, results, s))
    with open(txt_path, "w", encoding="utf-8") as f:
        f.write(render(vehicle, results, s))

    print(f"  ✓ HTML: {html_path}")
    print(f"  ✓ TXT:  {txt_path}")

    # שליחה לטלגרם
    summary = (
        f"✅ *דוח מוכן — VIN: {vin}*\n"
        f"🔴 דחוף: {s['counts']['RED']}  "
        f"🟡 בינוני: {s['counts']['YELLOW']}  "
        f"🟢 נמוך: {s['counts']['GREEN']}\n"
        f"סה\"כ: {s['total']} תקלות ({s['unique']} ייחודיות)"
    )
    bot.send_message(CHAT_ID, summary, parse_mode="Markdown")

    with open(html_path, "rb") as f:
        bot.send_document(CHAT_ID, f, caption=f"📋 דוח HTML – {vin}")

    print("  ✓ נשלח לטלגרם")


# ─────────────────────────────────────────
# מעקב מייל
# ─────────────────────────────────────────
def check_mail():
    try:
        mail = imaplib.IMAP4_SSL("imap.gmail.com")
        mail.login(GMAIL_USER, GMAIL_PASS)
        mail.select("inbox")

        _, messages = mail.search(None, '(UNSEEN)')
        if not messages[0]:
            return

        for num in messages[0].split():
            _, msg_data = mail.fetch(num, "(RFC822)")
            msg = email.message_from_bytes(msg_data[0][1])

            for part in msg.walk():
                fname = part.get_filename() or ""
                if fname.lower().endswith('.pdf'):
                    print(f"📥 PDF נמצא: {fname}")
                    pdf_path = os.path.join(BASE_DIR, fname)
                    with open(pdf_path, 'wb') as f:
                        f.write(part.get_payload(decode=True))

                    with pdfplumber.open(pdf_path) as pdf_reader:
                        text = '\n'.join(
                            p.extract_text() for p in pdf_reader.pages if p.extract_text()
                        )

                    vin_match = re.search(r'VIN[:\s]+([A-Z0-9]{17})', text)
                    vin = vin_match.group(1) if vin_match else "UNKNOWN_VIN"

                    text_clean = text.replace(vin, "")
                    codes = sorted(set(re.findall(r'\b[PCBU][0-9A-F]{4,6}\b', text_clean)))

                    if codes:
                        process_and_send(vin, codes)
                    else:
                        bot.send_message(CHAT_ID, f"✅ סריקה נקייה! לא נמצאו תקלות — VIN: {vin}")

                    os.remove(pdf_path)
        mail.logout()

    except Exception as e:
        print(f"⚠️ שגיאת מייל: {e}")


# ─────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────
if __name__ == "__main__":
    os.makedirs(REPORTS_DIR, exist_ok=True)
    print("🚀 בוט מוסך ניסים v2 הופעל!", flush=True)
    print("📧 ממתין לדוחות סריקה במייל...", flush=True)
    print(f"📚 KNOWLEDGE_BASE טעון עם {len(KNOWLEDGE_BASE)} קודים\n", flush=True)

    while True:
        check_mail()
        time.sleep(15)
