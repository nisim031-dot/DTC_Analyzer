#!/usr/bin/env bash
# ============================================================
#   RUN_BOT — מפעיל מרכזי ל-DTC Analyzer + בוט תרגום XTOOL
#   הפעלה:  ./run_bot.sh    (Mac/Linux)
# ============================================================
set -u
cd "$(dirname "$0")"

APK="android/app/build/outputs/apk/debug/app-debug.apk"

refresh_dict() {
  echo "מרענן מילון DTC..."
  python3 tools/export_kb.py
}

build_apk() {
  refresh_dict
  echo "בונה APK..."
  ( cd android && ./gradlew assembleDebug )
}

while true; do
  cat <<'MENU'

============================================
   בוט DTC / תרגום XTOOL — תפריט הפעלה
============================================
  1 - בנה והתקן את אפליקציית התרגום על הטאבלט (Android)
  2 - בנה APK בלבד (בלי התקנה)
  3 - רענן את מילון ה-DTC מבסיס הידע
  4 - הרץ את בוט המייל-טלגרם (garage_bot_v2.py)
  5 - הרץ את מנתח הדוחות (dtc_analyzer.py)
  0 - יציאה
============================================
MENU
  read -rp "בחר מספר ואנטר: " choice
  case "$choice" in
    1)
      build_apk || { echo "❌ בנייה נכשלה"; continue; }
      echo "מתקין על הטאבלט (ודא ש-USB Debugging פעיל וחובר USB)..."
      adb install -r "$APK" && echo "✓ הותקן. פתח את 'תרגום XTOOL' והשלם אונבורדינג." \
        || echo "❌ התקנה נכשלה — בדוק adb / חיבור USB"
      ;;
    2) build_apk && echo "✓ נבנה: $APK" || echo "❌ בנייה נכשלה" ;;
    3) refresh_dict && echo "✓ המילון עודכן." ;;
    4) python3 garage_bot_v2.py ;;
    5) python3 dtc_analyzer.py ;;
    0) echo "ביי!"; exit 0 ;;
    *) echo "בחירה לא תקינה." ;;
  esac
done
