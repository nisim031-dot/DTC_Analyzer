"""
split_manual.py — מחלץ טקסט מ-PDF גדול ומפצל לקבצים קטנים
"""

import fitz  # pymupdf (מותקן: 1.27.2)
import os
import sys

PDF_PATH       = r"E:\NISIM_PROJECTS\Nisim Car Diagnostic\manuals\Report_Aioniq.pdf.pdf"
OUT_DIR        = r"E:\NISIM_PROJECTS\Nisim Car Diagnostic\manuals\manual_parts"
PAGES_PER_FILE = 50

def main():
    sys.stdout.reconfigure(encoding="utf-8")
    if not os.path.exists(PDF_PATH):
        print(f"שגיאה: קובץ לא נמצא:\n  {PDF_PATH}")
        sys.exit(1)

    size_mb = os.path.getsize(PDF_PATH) / 1024 / 1024
    print(f"קובץ: {os.path.basename(PDF_PATH)}  ({size_mb:.1f} MB)")

    os.makedirs(OUT_DIR, exist_ok=True)

    doc   = fitz.open(PDF_PATH)
    total = len(doc)
    print(f"סה\"כ עמודים: {total}")
    print(f"יוצרים {(total + PAGES_PER_FILE - 1) // PAGES_PER_FILE} קבצים...\n")

    files_created = 0
    for start in range(0, total, PAGES_PER_FILE):
        end  = min(start + PAGES_PER_FILE, total)
        text = f"=== Report_Aioniq | עמודים {start+1}–{end} ===\n"

        for page_num in range(start, end):
            page_text = doc[page_num].get_text().strip()
            if page_text:
                text += f"\n--- עמוד {page_num + 1} ---\n{page_text}\n"

        out_file = os.path.join(OUT_DIR, f"part_{start+1:04d}-{end:04d}.txt")
        with open(out_file, "w", encoding="utf-8") as f:
            f.write(text)

        kb = os.path.getsize(out_file) // 1024
        print(f"  ✓ {os.path.basename(out_file)}  ({end - start} עמודים, {kb} KB)")
        files_created += 1

    doc.close()
    print(f"\nסיום! {files_created} קבצים נשמרו ב:\n  {OUT_DIR}")


if __name__ == "__main__":
    main()
