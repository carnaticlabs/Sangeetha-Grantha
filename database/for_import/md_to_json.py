"""Parse mdeng.md → eng_krithis.json with correct section structure.

This script replaces the broken manual JSON generation step (the "MANUAL /
UNKNOWN PROCESS" in the pipeline diagram). It:

  1. Reads mdeng.md (produced by the fixed extract_eng_pymupdf.py) which now
     contains ## Section: <key> markers for pallavi / anupallavi / charanam /
     madhyamakala boundaries.
  2. Splits the document into per-entry blocks.
  3. Applies a Velthuis-ASCII → precomposed IAST Unicode decoder so section
     text is human-readable (same encoding as the original hand-written JSON).
  4. Reads krithi_comparison_matched.csv to get authoritative IAST titles,
     raga names, tala names per entry index.
  5. Writes eng_krithis.json with canonical section key ordering and no
     base64 / image artefacts.

Usage:
    python md_to_json.py [mdeng.md] [krithi_comparison_matched.csv] [eng_krithis.json]

Defaults are relative to database/for_import/ when run from repo root.

Ref: TRACK-070 Phase 2 (2.1 – 2.4)
"""

from __future__ import annotations

import csv
import json
import logging
import re
import sys
import os
from typing import Optional

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Velthuis-ASCII → IAST Unicode decoder
# ---------------------------------------------------------------------------
#
# mdeng.pdf uses a TeX Velthuis-style ASCII encoding for IAST diacritics.
# Extracted text has patterns like:
#   ¯a  → ā     (U+00AF MACRON + a)
#   n.  → ṇ     (n followed by FULL STOP)
#   ´s  → ś     (U+00B4 ACUTE ACCENT + s)
#   ˙m  → ṃ     (U+02D9 DOT ABOVE + m)
#   ˜n  → ñ     (U+02DC SMALL TILDE + n)
#
# Order matters: apply longer / more specific patterns first.

_VELTHUIS_SUBS: list[tuple[str, str]] = [
    # ── Dot-above prefix (˙ U+02D9) ──
    (r'˙m', 'ṃ'),   # anusvara
    (r'˙M', 'Ṃ'),
    (r'˙n', 'ṅ'),
    (r'˙N', 'Ṅ'),
    (r'˙s', 'ṡ'),   # rare
    # ── Tilde prefix (˜ U+02DC) ──
    (r'˜n', 'ñ'),
    (r'˜N', 'Ñ'),
    # ── Acute accent prefix (´ U+00B4) ──
    (r'´s', 'ś'),
    (r'´S', 'Ś'),
    # ── Macron prefix (¯ U+00AF) + vowel ──
    # Must be before bare-vowel rules
    (r'¯a', 'ā'),
    (r'¯A', 'Ā'),
    (r'¯\u0131', 'ī'),   # ¯ı (dotless i U+0131)
    (r'¯i', 'ī'),
    (r'¯I', 'Ī'),
    (r'¯u', 'ū'),
    (r'¯U', 'Ū'),
    (r'¯e', 'ē'),
    (r'¯o', 'ō'),
    (r'¯r', 'r̄'),   # rare
    # ── Subscript dot suffix (.) on consonants ──
    # Longer patterns first to avoid partial matches
    (r'n\.', 'ṇ'),
    (r'N\.', 'Ṇ'),
    (r't\.', 'ṭ'),
    (r'T\.', 'Ṭ'),
    (r'd\.', 'ḍ'),
    (r'D\.', 'Ḍ'),
    (r's\.', 'ṣ'),
    (r'S\.', 'Ṣ'),
    (r'l\.', 'ḷ'),
    (r'L\.', 'Ḷ'),
    (r'r\.', 'ṛ'),
    (r'R\.', 'Ṝ'),
    (r'm\.', 'ṃ'),
    (r'M\.', 'Ṃ'),
    (r'h\.', 'ḥ'),
    (r'H\.', 'Ḥ'),
    # ── Subscript circle / dot on vowels ──
    (r'\.a', 'a'),   # sometimes decorative, drop the dot
]

# Compile substitution list once.
_COMPILED_SUBS = [(re.compile(pat), repl) for pat, repl in _VELTHUIS_SUBS]

# Ornamental / cross symbols and similar debris from PDF section dividers.
_RE_ORNAMENTAL = re.compile(r'[✠†‡✦✧✩✪◆◇▪▫■□●○]')
# Remaining artefacts: stray macrons/dots after decoding.
_RE_STRAY_MACRON = re.compile(r'¯(?=[^a-zA-Z]|$)')
_RE_STRAY_DOT = re.compile(r'\.(?=\s|$)')   # trailing dot on non-word boundary
# Base64 / image just in case they slipped through.
_RE_IMAGE = re.compile(r'!\[.*?\]\(data:[^)]+\)', re.DOTALL)
# TOC entry lines: text followed by lots of dots and a page number.
_RE_TOC_LINE = re.compile(r'^.+\.{5,}\s*\d+\s*$')
# Metadata line: "rāgam: ... tālam: ..."
_RE_METADATA = re.compile(r'r\s*[aā]\s*ga\s*[mṃ]?\s*:', re.IGNORECASE)
# Arabic number at start of lyric line (entry/footnote number).
_RE_LEADING_NUM = re.compile(r'^\s*\d{1,3}\s+', re.MULTILINE)
# Devanagari leading numeral.
_RE_DEVA_NUM = re.compile(r'^[०-९]+\s+', re.MULTILINE)
# 3+ blank lines.
_RE_MULTI_BLANK = re.compile(r'\n{3,}')


def decode_velthuis(text: str) -> str:
    """Convert Velthuis-ASCII IAST notation to precomposed Unicode IAST."""
    for pattern, replacement in _COMPILED_SUBS:
        text = pattern.sub(replacement, text)
    return text


# Pre-decode: remove spaces that split Velthuis multi-char tokens across PDF spans.
# e.g. "n. " + "d." → "n.d." before decoding so ṇ + ḍ are adjacent.
# Pattern: letter + period (subscript marker) + space + next Velthuis char.
_RE_PRE_DOT_SPACE = re.compile(r'([a-zA-Z])\. +([a-zA-Z¯´˙˜])')
# Prefix diacritic + space before its base letter: "¯ a" → "¯a", "˙ m" → "˙m"
_RE_PRE_DIAC_SPACE = re.compile(r'([¯´˙˜]) +([a-zA-Z])')
# Anusvara (after decoding) + space + consonant starting next syllable (span break).
# Only collapse if the space is a SINGLE space (double space = word boundary).
_RE_POST_ANUSVARA = re.compile(r'([ṃṁ]) ([a-zA-Zāīūṭḍṇṣśḥ])')


def clean_section_text(text: str) -> str:
    """Strip artefacts and decode Velthuis encoding."""
    text = _RE_IMAGE.sub('', text)
    text = _RE_ORNAMENTAL.sub('', text)
    # Fix intra-token spaces BEFORE decoding.
    text = _RE_PRE_DOT_SPACE.sub(r'\1. \2', text)   # normalise: keep dot, remove extra space
    text = _RE_PRE_DOT_SPACE.sub(r'\1.\2', text)    # second pass: now "n." + "d" → "n.d"
    text = _RE_PRE_DIAC_SPACE.sub(r'\1\2', text)    # "¯ a" → "¯a"
    text = decode_velthuis(text)
    # Post-decode: collapse single-space anusvara breaks (PDF span boundary artefact).
    # Double spaces or newlines indicate true word boundaries — leave those alone.
    text = _RE_POST_ANUSVARA.sub(r'\1\2', text)
    text = _RE_LEADING_NUM.sub('', text)
    text = _RE_DEVA_NUM.sub('', text)
    text = _RE_MULTI_BLANK.sub('\n\n', text)
    # Remove stray parentheses left from madhyamakala label extraction
    text = re.sub(r'^\s*[()]\s*$', '', text, flags=re.MULTILINE)
    return text.strip()


# ---------------------------------------------------------------------------
# Raga / tala name normalisation  (2.2)
# ---------------------------------------------------------------------------

def normalise_raga(name: str) -> str:
    """Strip melakarta suffixes and double-vowel prefix OCR artefacts."""
    name = name.strip()
    # Strip "(28)"-style suffixes
    name = re.sub(r'\s*\([^)]*\)\s*$', '', name).strip()
    # Fix double-ā prefix like "āarabhi" → "ārabhi"
    name = re.sub(r'^([āĀ])a', r'\1', name)
    name = re.sub(r'^aa', 'ā', name)
    return name


# ---------------------------------------------------------------------------
# Canonical section ordering  (2.4)
# ---------------------------------------------------------------------------

CANONICAL_ORDER = [
    "pallavi",
    "anupallavi",
    "charanam",
    "samashti_charanam",
    "madhyamakala",
    "chittaswaram",
    "swara_sahitya",
]


def ordered_sections(raw: dict[str, str]) -> dict[str, str]:
    """Return sections dict with keys in canonical musical order."""
    result: dict[str, str] = {}
    for key in CANONICAL_ORDER:
        if key in raw:
            result[key] = raw[key]
    for key, val in raw.items():
        if key not in result:
            result[key] = val
    return result


# ---------------------------------------------------------------------------
# Parse mdeng.md into entry blocks
# ---------------------------------------------------------------------------

_RE_SECTION_MARKER = re.compile(r'^## Section:\s*(\w+)\s*$', re.IGNORECASE)
# The entry header metadata line: "r¯aga ˙m: <raga> t¯al.a ˙m: <tala>"
_RE_ENTRY_METADATA = re.compile(r'r¯aga\s*˙m\s*:.*t¯al')
# Entry divider ornament (✠ ✠ ✠ ✠ ✠) — closes current section without starting new entry.
_RE_ENTRY_DIVIDER = re.compile(r'✠')


def parse_entries_from_md(md_text: str, max_entries: Optional[int] = None) -> list[dict[str, str]]:
    """Split mdeng.md into a list of section dicts, one per krithi entry.

    Each entry dict maps canonical section key → raw Velthuis text.

    The mdeng.pdf book prints all 219 entries TWICE (first set = IAST
    transliteration, second set = a duplicate of the same). We detect a new
    entry by the metadata line (r¯aga ˙m: ... t¯al.a ˙m: ...) which
    immediately precedes the first ## Section: pallavi of each entry.  We stop
    after max_entries entries to avoid ingesting the duplicate second half.

    Returns list of dicts in document order (index 0 = krithi #1).
    """
    entries: list[dict[str, str]] = []
    current_sections: dict[str, str] = {}
    current_key: Optional[str] = None
    current_lines: list[str] = []
    in_entry = False   # True once we've seen the first entry metadata line

    def flush_section() -> None:
        if current_key and current_lines:
            text = "\n".join(current_lines).strip()
            if current_key in current_sections:
                # Append da-capo / repeated pallavi text to the existing section
                existing = current_sections[current_key]
                if text and text not in existing:
                    current_sections[current_key] = existing  # ignore da-capo repeat
            else:
                current_sections[current_key] = text

    def start_new_entry() -> None:
        nonlocal current_sections, current_key, current_lines, in_entry
        flush_section()
        if current_sections:
            entries.append(dict(current_sections))
        current_sections = {}
        current_key = None
        current_lines = []
        in_entry = True

    for line in md_text.splitlines():
        # Stop if we've collected enough entries (avoid second half duplicates).
        if max_entries and len(entries) >= max_entries:
            break

        # Entry metadata line = entry boundary marker.
        if _RE_ENTRY_METADATA.search(line):
            start_new_entry()
            continue  # metadata itself is not section content

        # Entry divider (✠ ✠ ✠ ✠ ✠) — flush and close current section.
        # Text after this divider until the next metadata line is variant/footnote,
        # not lyric content of the current entry.
        if _RE_ENTRY_DIVIDER.search(line):
            flush_section()
            current_key = None
            current_lines = []
            continue

        m = _RE_SECTION_MARKER.match(line)
        if m:
            flush_section()
            current_lines = []
            current_key = m.group(1).lower()
            continue

        if not in_entry:
            continue  # skip pre-content (TOC etc.)

        # Skip TOC lines (lots of dots + page number).
        if _RE_TOC_LINE.match(line):
            continue

        if current_key:
            current_lines.append(line)

    # Flush last section / entry.
    flush_section()
    if current_sections:
        entries.append(dict(current_sections))

    return entries


# ---------------------------------------------------------------------------
# Load CSV metadata
# ---------------------------------------------------------------------------

def load_csv(csv_path: str) -> list[dict]:
    """Return list of CSV rows in index order (row 0 = krithi #1)."""
    rows: list[dict] = []
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return rows


# ---------------------------------------------------------------------------
# Validation  (1.5)
# ---------------------------------------------------------------------------

def validate_entry(entry_idx: int, sections: dict[str, str]) -> list[str]:
    warnings: list[str] = []
    if "pallavi" not in sections or not sections["pallavi"].strip():
        warnings.append(f"Entry {entry_idx + 1}: pallavi is missing or empty")
    for key, text in sections.items():
        if "data:image" in text or "base64" in text:
            warnings.append(f"Entry {entry_idx + 1} section '{key}': contains image data")
        if len(text) > 2000:
            warnings.append(
                f"Entry {entry_idx + 1} section '{key}': {len(text)} chars — possible page bleed"
            )
        if len(text.strip()) < 5 and text.strip():
            warnings.append(f"Entry {entry_idx + 1} section '{key}': suspiciously short: {text!r}")
    return warnings


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    md_path = "database/for_import/mdeng.md"
    csv_path = "database/for_import/krithi_comparison_matched.csv"
    out_path = "database/for_import/eng_krithis.json"

    if len(sys.argv) >= 2:
        md_path = sys.argv[1]
    if len(sys.argv) >= 3:
        csv_path = sys.argv[2]
    if len(sys.argv) >= 4:
        out_path = sys.argv[3]

    for path in (md_path, csv_path):
        if not os.path.exists(path):
            logger.error("File not found: %s", path)
            sys.exit(1)

    logger.info("Reading CSV from %s", csv_path)
    csv_rows = load_csv(csv_path)
    logger.info("Found %d rows in CSV", len(csv_rows))

    logger.info("Reading %s", md_path)
    with open(md_path, "r", encoding="utf-8") as f:
        md_text = f.read()

    logger.info("Parsing entries from markdown (first set only)...")
    # The book prints 219 entries twice; stop after the CSV row count.
    raw_entries = parse_entries_from_md(md_text, max_entries=len(csv_rows))
    logger.info("Found %d entries in markdown", len(raw_entries))

    if len(raw_entries) != len(csv_rows):
        logger.warning(
            "Entry count mismatch: %d markdown entries vs %d CSV rows. "
            "Entries will be matched by min(count).",
            len(raw_entries), len(csv_rows),
        )

    all_warnings: list[str] = []
    output: list[dict] = []

    count = min(len(raw_entries), len(csv_rows))
    for idx in range(count):
        raw_sections = raw_entries[idx]
        csv_row = csv_rows[idx]

        # Decode Velthuis and clean each section.
        cleaned_sections: dict[str, str] = {}
        for key, text in raw_sections.items():
            cleaned_sections[key] = clean_section_text(text)

        # Canonical ordering.
        sections = ordered_sections(cleaned_sections)

        # Normalise metadata from CSV (IAST, already correct Unicode).
        title_en = csv_row.get("Title-EN", "").strip()
        raga_en = normalise_raga(csv_row.get("Raga-EN", "").strip())
        tala_en = csv_row.get("Tala-EN", "").strip()

        entry = {
            "id": idx + 1,
            "title": title_en,
            "title_is_numbered": True,
            "raga": raga_en,
            "tala": tala_en,
            "sections": sections,
        }
        output.append(entry)

        warnings = validate_entry(idx, sections)
        all_warnings.extend(warnings)

    # Write output.
    logger.info("Writing %d entries to %s", len(output), out_path)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    logger.info("Done.")

    if all_warnings:
        logger.warning("%d validation warnings:", len(all_warnings))
        for w in all_warnings[:30]:  # Cap output to first 30
            logger.warning("  %s", w)
        if len(all_warnings) > 30:
            logger.warning("  ... and %d more", len(all_warnings) - 30)
    else:
        logger.info("All entries passed validation.")


if __name__ == "__main__":
    main()
