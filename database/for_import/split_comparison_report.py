import csv
import sys
import unicodedata
import os
import re
from indic_transliteration import sanscript
from indic_transliteration.sanscript import transliterate
from rapidfuzz import fuzz

csv_path = 'krithi_comparison_report.csv'
matched_csv_path = 'krithi_comparison_matched.csv'
mismatched_csv_path = 'krithi_comparison_mismatched.csv'

title_min = 70
raga_min = 80
tala_min = 70

def has_devanagari(text: str) -> bool:
    return any('\u0900' <= ch <= '\u097F' for ch in text)

def dev_to_iast(text: str) -> str:
    text = (text or '').strip()
    if not text:
        return ''
    if has_devanagari(text):
        try:
            return transliterate(text, sanscript.DEVANAGARI, sanscript.IAST)
        except Exception:
            return text
    return text

def normalize(text: str) -> str:
    text = dev_to_iast((text or '').strip().lower())
    text = unicodedata.normalize('NFKD', text)
    text = ''.join(ch for ch in text if not unicodedata.combining(ch))
    text = re.sub(r'\([^)]*\)', ' ', text)
    text = re.sub(r'[^a-z0-9\s]', ' ', text)
    text = re.sub(r'\b(sync|synced|unknown|null|none)\b', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text

def score(a: str, b: str) -> float:
    na = normalize(a)
    nb = normalize(b)
    if not na or not nb:
        return 0.0
    return float(fuzz.token_sort_ratio(na, nb))

def main():
    if not os.path.exists(csv_path):
        print(f"Error: {csv_path} not found.")
        sys.exit(1)

    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)

    matched_rows = []
    mismatched_rows = []

    for row in rows:
        title_en = row.get('Title-EN', '')
        title_sa = row.get('Title-SA', '')
        raga_en = row.get('Raga-EN', '')
        raga_sa = row.get('Raga-SA', '')
        tala_en = row.get('Tala-EN', '')
        tala_sa = row.get('Tala-SA', '')

        t_score = score(title_en, title_sa)
        r_score = score(raga_en, raga_sa)
        tl_score = score(tala_en, tala_sa)

        if t_score >= title_min and r_score >= raga_min and tl_score >= tala_min:
            matched_rows.append(row)
        else:
            # We can optionally tag the row with the reason it failed if we had extra columns, but for now just route it
            row['Mismatch-Reason'] = f"Scores: T={t_score:.1f}, R={r_score:.1f}, TL={tl_score:.1f}"
            mismatched_rows.append(row)

    if fieldnames:
        mismatched_fieldnames = list(fieldnames) + ['Mismatch-Reason']
        
        with open(matched_csv_path, 'w', encoding='utf-8', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(matched_rows)

        with open(mismatched_csv_path, 'w', encoding='utf-8', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=mismatched_fieldnames)
            writer.writeheader()
            writer.writerows(mismatched_rows)

    print(f"Total rows: {len(rows)}")
    print(f"Matched rows: {len(matched_rows)} saved to {matched_csv_path}")
    print(f"Mismatched rows: {len(mismatched_rows)} saved to {mismatched_csv_path}")

if __name__ == '__main__':
    main()
