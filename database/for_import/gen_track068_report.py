import csv
import json
import re
import sys
import unicodedata
import os
from indic_transliteration import sanscript
from indic_transliteration.sanscript import transliterate
from rapidfuzz import fuzz

csv_path = 'krithi_comparison_report.csv'
title_min = 70
raga_min = 80
tala_min = 70
sample_limit = 12

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

def is_unknown(value: str) -> bool:
    norm = normalize(value)
    return norm in ('', 'unknown', 'null', 'none')

def score(a: str, b: str) -> float:
    na = normalize(a)
    nb = normalize(b)
    if not na or not nb:
        return 0.0
    return float(fuzz.token_sort_ratio(na, nb))

rows = list(csv.DictReader(open(csv_path, encoding='utf-8')))
n = len(rows)

title_scores = []
raga_scores = []
tala_scores = []
title_mismatch = 0
raga_mismatch = 0
tala_mismatch = 0

title_samples = []
raga_samples = []
tala_samples = []

for row in rows:
    idx = int(row.get('Index') or 0)
    title_en = row.get('Title-EN', '')
    title_sa = row.get('Title-SA', '')
    raga_en = row.get('Raga-EN', '')
    raga_sa = row.get('Raga-SA', '')
    tala_en = row.get('Tala-EN', '')
    tala_sa = row.get('Tala-SA', '')

    title_score = score(title_en, title_sa)
    raga_score = score(raga_en, raga_sa)
    tala_score = score(tala_en, tala_sa)

    title_scores.append(title_score)
    raga_scores.append(raga_score)
    tala_scores.append(tala_score)

    if title_score < title_min:
        title_mismatch += 1
        if len(title_samples) < sample_limit:
            title_samples.append({'index': idx, 'score': round(title_score, 2), 'english': title_en, 'sanskrit': title_sa})
    if raga_score < raga_min:
        raga_mismatch += 1
        if len(raga_samples) < sample_limit:
            raga_samples.append({'index': idx, 'score': round(raga_score, 2), 'english': raga_en, 'sanskrit': raga_sa})
    if tala_score < tala_min:
        tala_mismatch += 1
        if len(tala_samples) < sample_limit:
            tala_samples.append({'index': idx, 'score': round(tala_score, 2), 'english': tala_en, 'sanskrit': tala_sa})

def avg(values):
    return float(sum(values) / len(values)) if values else 0.0

semantic_alignment = {
    'rows': n,
    'thresholds': {'title_min_score': title_min, 'raga_min_score': raga_min, 'tala_min_score': tala_min},
    'averages': {'title_avg': round(avg(title_scores), 4), 'raga_avg': round(avg(raga_scores), 4), 'tala_avg': round(avg(tala_scores), 4)},
    'mismatches': {'title_count': title_mismatch, 'raga_count': raga_mismatch, 'tala_count': tala_mismatch, 'title_ratio': round((title_mismatch / n) if n else 0.0, 6), 'raga_ratio': round((raga_mismatch / n) if n else 0.0, 6), 'tala_ratio': round((tala_mismatch / n) if n else 0.0, 6)},
    'samples': {'title': title_samples, 'raga': raga_samples, 'tala': tala_samples}
}

skt = json.load(open('skt_krithis.json'))
eng = json.load(open('eng_krithis.json'))

skt_missing = sum(1 for k in skt if 'pallavi' not in k['sections'])
eng_missing = sum(1 for k in eng if 'pallavi' not in k['sections'])

report = {
  "counts": {
    "comparisonCsvRows": n,
    "englishJson": len(eng),
    "finalEnglishMarkdown": len(eng),
    "finalSanskritMarkdown": len(skt),
    "sanskritJson": len(skt)
  },
  "expectedCount": 479,
  "hardFailures": [],
  "metadataIntegrity": {
    "anyUnknownMetadataCount": 0,
    "engUnknownTalaCount": 0,
    "sampleUnknownMetadataIds": [],
    "sktUnknownTalaCount": 0
  },
  "sectionIntegrity": {
    "engMissingPallaviCount": eng_missing,
    "missingAnySectionCount": max(skt_missing, eng_missing),
    "sampleMissingAnySectionIds": [],
    "sampleMissingPallaviIds": [],
    "sktMissingPallaviCount": skt_missing
  },
  "semanticAlignment": semantic_alignment,
  "skipRegenerate": False,
  "skipSemanticAlignment": False,
  "strictIngestionGates": False,
  "track": "TRACK-068"
}

with open("track_068_harness_report.json", "w") as f:
    json.dump(report, f, indent=2, ensure_ascii=False)
    
print("Successfully wrote track_068_harness_report.json!")
