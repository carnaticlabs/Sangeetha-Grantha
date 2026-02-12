import psycopg
import re
import sys
import os

# Copy logic from diacritic_normalizer.py
_RULES = [
    (re.compile(r"\u00AF\s*a"), "ā"),
    (re.compile(r"\u00AF\s*[iı]"), "ī"),
    (re.compile(r"\u00AF\s*u"), "ū"),
    (re.compile(r"\s*\u02D9\s*m"), "ṁ"),
    (re.compile(r"\s*\u02D9\s*n"), "ṅ"),
    (re.compile(r"\s*\u02D9\s*r"), "ṛ"),
    (re.compile(r"\u00B4\s*s"), "ś"),
    (re.compile(r"\u02DC\s*n"), "ñ"),
    (re.compile(r"\u2720"), ""),
]

_CONSONANT_DOT_RULES = [
    (re.compile(r"(?<![A-Z])s\.\s*"), "ṣ"),
    (re.compile(r"(?<![A-Z])n\.\s*"), "ṇ"),
    (re.compile(r"(?<![A-Z])d\.\s*"), "ḍ"),
    (re.compile(r"(?<![A-Z])t\.\s*"), "ṭ"),
    (re.compile(r"(?<![A-Z])l\.\s*"), "ḷ"),
    (re.compile(r"(?<![A-Z])r\.\s*"), "ṛ"),
    (re.compile(r"(?<![A-Z])h\.\s*"), "ḥ"),
]

def normalize(text):
    if not text: return text
    result = text
    for pattern, replacement in _RULES:
        result = pattern.sub(replacement, result)
    for pattern, replacement in _CONSONANT_DOT_RULES:
        result = pattern.sub(replacement, result)
    # Strip trailing numbers
    result = re.sub(r"\n\s*\d+\s*$", "\n", result, flags=re.MULTILINE)
    return result

conn_str = "postgresql://postgres:postgres@localhost:5432/sangita_grantha"
with psycopg.connect(conn_str) as conn:
    with conn.cursor() as cur:
        # Fetch garbled rows
        cur.execute("SELECT id, lyrics FROM krithi_lyric_variants WHERE lyrics ~ '[\\u00AF\\u02D9\\u00B4\\u02DC]';")
        rows = cur.fetchall()
        print(f"Fixing {len(rows)} rows...")
        for row_id, lyrics in rows:
            clean = normalize(lyrics)
            cur.execute("UPDATE krithi_lyric_variants SET lyrics = %s WHERE id = %s", (clean, row_id))
            
            # Also fix sections for this variant
            cur.execute("SELECT id, text FROM krithi_lyric_sections WHERE lyric_variant_id = %s", (row_id,))
            sections = cur.fetchall()
            for s_id, s_text in sections:
                s_clean = normalize(s_text)
                cur.execute("UPDATE krithi_lyric_sections SET text = %s WHERE id = %s", (s_clean, s_id))
        
        conn.commit()
        print("Done!")
