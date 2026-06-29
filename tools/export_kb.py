"""
export_kb.py — מייצא את KNOWLEDGE_BASE מ-dtc_analyzer.py לקובץ JSON
שאפליקציית האנדרואיד טוענת כ-asset לתרגום קודי DTC לעברית.

הרצה:
    python tools/export_kb.py

פלט:
    android/app/src/main/assets/dtc_dictionary.json
"""

import json
import os
import sys
from dataclasses import asdict

# מאפשר לייבא את dtc_analyzer מתיקיית השורש של הפרויקט
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from dtc_analyzer import KNOWLEDGE_BASE  # noqa: E402

OUTPUT = os.path.join(
    ROOT, "android", "app", "src", "main", "assets", "dtc_dictionary.json"
)


def build_dictionary() -> dict:
    """ממיר את KNOWLEDGE_BASE למילון JSON-friendly לפי קוד DTC."""
    out = {}
    for code, k in KNOWLEDGE_BASE.items():
        entry = asdict(k)
        # הקוד כבר מופיע כמפתח — אין צורך לשכפל אותו בערך
        entry.pop("code", None)
        out[code] = entry
    return out


def main() -> None:
    data = build_dictionary()
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"נכתבו {len(data)} קודי DTC אל {OUTPUT}")


if __name__ == "__main__":
    main()
