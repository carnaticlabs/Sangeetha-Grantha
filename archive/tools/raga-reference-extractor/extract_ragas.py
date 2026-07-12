#!/usr/bin/env python3
"""
Raga Reference Data Extractor
==============================
Extracts comprehensive raga data from:
  1. Built-in 72 melakarta raga definitions (Venkatamakhi scheme)
  2. Wikipedia "List of Janya ragas" — janya ragas per melakarta
  3. karnatik.com — canonical names and cross-reference

Outputs an idempotent SQL seed file for the Sangeetha Grantha database.

Usage:
    python extract_ragas.py [--output output/raga_reference_data.sql]
"""

import argparse
import re
import sys
import time
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path

import requests
from bs4 import BeautifulSoup

# ---------------------------------------------------------------------------
# Swara notation helpers
# ---------------------------------------------------------------------------
# Project convention: S R1 G1 M1 P D1 N1 S (numbered swarasthanas)
# Wikipedia often uses: S R₁ G₁ M₁ P D₁ N₁ Ṡ  or  Sa Ri Ga Ma Pa Da Ni
# karnatik.com uses: S R1 G1 M1 P D1 N1 S

# Mapping from common Wikipedia/web notations to project convention
SWARA_NORMALIZE_MAP = {
    # Unicode subscript digits
    "₁": "1", "₂": "2", "₃": "3",
    # Common alternate spellings
    "Sa": "S", "Ri": "R", "Ga": "G", "Ma": "M",
    "Pa": "P", "Da": "D", "Ni": "N",
    "sa": "S", "ri": "R", "ga": "G", "ma": "M",
    "pa": "P", "da": "D", "ni": "N",
    # Dotted S for upper octave
    "Ṡ": "S", "ṡ": "S", "S'": "S", "Ś": "S",
    # Lowercase swaras
    "s": "S", "r": "R", "g": "G", "m": "M",
    "p": "P", "d": "D", "n": "N",
}


def normalize_swara_notation(raw: str) -> str:
    """Convert various swara notations to project convention (S R2 G3 M1 P D2 N3 S)."""
    if not raw:
        return ""
    text = raw.strip()
    # Replace known tokens
    for old, new in SWARA_NORMALIZE_MAP.items():
        text = text.replace(old, new)
    # Remove commas, extra whitespace
    text = text.replace(",", " ").replace(";", " ")
    text = re.sub(r"\s+", " ", text).strip()
    return text


def normalize_name(name: str) -> str:
    """ASCII-normalize a raga name for the name_normalized column."""
    # Remove diacritics
    nfkd = unicodedata.normalize("NFKD", name)
    ascii_name = "".join(c for c in nfkd if not unicodedata.combining(c))
    # Lowercase, strip, collapse whitespace
    ascii_name = re.sub(r"\s+", " ", ascii_name.strip().lower())
    return ascii_name


def sql_escape(s: str) -> str:
    """Escape single quotes for SQL string literals."""
    if s is None:
        return "NULL"
    return "'" + s.replace("'", "''") + "'"


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------

@dataclass
class Raga:
    name: str
    name_normalized: str = ""
    melakarta_number: int | None = None
    parent_melakarta: int | None = None  # melakarta number of parent (for janyas)
    arohanam: str = ""
    avarohanam: str = ""
    notes: str = ""
    source: str = ""  # "melakarta", "wikipedia", "karnatik"

    def __post_init__(self):
        if not self.name_normalized:
            self.name_normalized = normalize_name(self.name)


# ---------------------------------------------------------------------------
# 72 Melakarta Ragas (Venkatamakhi / Katapayadi scheme)
# ---------------------------------------------------------------------------
# These are the foundational 72 parent scales of Carnatic music.
# Each has a fixed, complete arohanam and avarohanam using all 7 swaras.
#
# The swarasthana mapping for melakartas follows the Katapayadi formula:
#   Chakra 1-6: M1 (Shuddha Madhyamam)
#   Chakra 7-12: M2 (Prati Madhyamam)
#   Within each chakra (6 ragas), Ri-Ga and Da-Ni cycle through:
#     R1 G1, R1 G2, R1 G3, R2 G2, R2 G3, R3 G3
#   Same pattern for Da-Ni: D1 N1, D1 N2, D1 N3, D2 N2, D2 N3, D3 N3

MELAKARTA_NAMES = {
    1: "Kanakangi", 2: "Ratnangi", 3: "Ganamurti",
    4: "Vanaspati", 5: "Manavati", 6: "Tanarupi",
    7: "Senavati", 8: "Hanumatodi", 9: "Dhenuka",
    10: "Natakapriya", 11: "Kokilapriya", 12: "Rupavati",
    13: "Gayakapriya", 14: "Vakulabharanam", 15: "Mayamalavagowla",
    16: "Chakravakam", 17: "Suryakantam", 18: "Hatakambari",
    19: "Jhankaradhwani", 20: "Natabhairavi", 21: "Kiravani",
    22: "Kharaharapriya", 23: "Gowrimanohari", 24: "Varunapriya",
    25: "Mararanjani", 26: "Charukesi", 27: "Sarasangi",
    28: "Harikambhoji", 29: "Dheerasankaraabharanam", 30: "Naganandini",
    31: "Yagapriya", 32: "Ragavardhini", 33: "Gangeyabhushani",
    34: "Vagadheeswari", 35: "Shulini", 36: "Chalanata",
    37: "Salagam", 38: "Jalarnavam", 39: "Jhalavarali",
    40: "Navaneetam", 41: "Pavani", 42: "Raghupriya",
    43: "Gavambhodi", 44: "Bhavapriya", 45: "Shubhapantuvarali",
    46: "Shadvidamargini", 47: "Suvarnangi", 48: "Divyamani",
    49: "Dhavalambari", 50: "Namanarayani", 51: "Kamavardhini",
    52: "Ramapriya", 53: "Gamanashrama", 54: "Vishwambhari",
    55: "Shamalangi", 56: "Shanmukhapriya", 57: "Simhendramadhyamam",
    58: "Hemavati", 59: "Dharmavati", 60: "Neetimati",
    61: "Kantamani", 62: "Rishabhapriya", 63: "Latangi",
    64: "Vachaspati", 65: "Mechakalyani", 66: "Chitrambari",
    67: "Sucharitra", 68: "Jyotiswarupini", 69: "Dhatuvardhini",
    70: "Nasikabhushani", 71: "Kosalam", 72: "Rasikapriya",
}

# Swarasthana patterns per position within a chakra (0-5)
_RI_GA_PATTERNS = [
    ("R1", "G1"), ("R1", "G2"), ("R1", "G3"),
    ("R2", "G2"), ("R2", "G3"), ("R3", "G3"),
]
_DA_NI_PATTERNS = [
    ("D1", "N1"), ("D1", "N2"), ("D1", "N3"),
    ("D2", "N2"), ("D2", "N3"), ("D3", "N3"),
]


def melakarta_scale(number: int) -> tuple[str, str]:
    """Compute arohanam and avarohanam for a melakarta raga by number (1-72).

    Katapayadi formula:
      half_idx  = (N-1) // 36  → 0 (M1) or 1 (M2)
      within_half = (N-1) % 36
      ri_ga_idx = within_half // 6  → selects Ri-Ga pair (0-5)
      da_ni_idx = within_half % 6   → selects Da-Ni pair (0-5)
    """
    idx = number - 1
    ma = "M1" if idx < 36 else "M2"

    within_half = idx % 36
    ri_ga_idx = within_half // 6
    da_ni_idx = within_half % 6

    ri, ga = _RI_GA_PATTERNS[ri_ga_idx]
    da, ni = _DA_NI_PATTERNS[da_ni_idx]

    arohanam = f"S {ri} {ga} {ma} P {da} {ni} S"
    avarohanam = f"S {ni} {da} P {ma} {ga} {ri} S"
    return arohanam, avarohanam


def build_melakarta_ragas() -> list[Raga]:
    """Build all 72 melakarta ragas with computed scales."""
    ragas = []
    for num in range(1, 73):
        name = MELAKARTA_NAMES[num]
        arohanam, avarohanam = melakarta_scale(num)
        ragas.append(Raga(
            name=name,
            melakarta_number=num,
            arohanam=arohanam,
            avarohanam=avarohanam,
            source="melakarta",
        ))
    return ragas


# ---------------------------------------------------------------------------
# Wikipedia scraping — List of Janya ragas
# ---------------------------------------------------------------------------

WIKIPEDIA_URL = "https://en.wikipedia.org/wiki/List_of_Janya_ragas"
USER_AGENT = "SangeethaGrantha-RagaExtractor/1.0 (Educational; seshadri@sangitagrantha.org)"


def fetch_wikipedia_janya_ragas() -> list[Raga]:
    """Scrape janya raga tables from Wikipedia.

    The page has one large table under "Scales" with ~979 rows.
    Melakarta ragas have their number as a prefix in the name column
    (e.g., "1Kanakāngi (Janaka raga)"). Janya ragas follow without
    a number prefix until the next melakarta row.
    """
    print(f"Fetching {WIKIPEDIA_URL} ...")
    headers = {"User-Agent": USER_AGENT}
    resp = requests.get(WIKIPEDIA_URL, headers=headers, timeout=30)
    resp.raise_for_status()

    soup = BeautifulSoup(resp.text, "html.parser")
    ragas = []

    # Find the large table (the one with 3-column header: Raga Name, Ascending, Descending)
    big_table = None
    for table in soup.find_all("table"):
        rows = table.find_all("tr")
        if len(rows) > 100:
            big_table = table
            break

    if not big_table:
        print("  WARNING: Could not find the main raga table on Wikipedia")
        return ragas

    current_melakarta_num = None
    rows = big_table.find_all("tr")
    print(f"  Processing {len(rows)} rows...")

    for row in rows[1:]:  # skip header row
        cells = row.find_all(["td", "th"])
        if len(cells) < 3:
            continue

        name_text = cells[0].get_text(strip=True)
        arohanam_raw = cells[1].get_text(strip=True)
        avarohanam_raw = cells[2].get_text(strip=True)

        if not name_text:
            continue

        # Clean footnote references like [1], [2]
        name_text = re.sub(r"\[.*?\]", "", name_text).strip()

        # Check if this is a melakarta row (starts with a number like "1Kanakāngi")
        melakarta_match = re.match(r"^(\d{1,2})(.+)", name_text)
        if melakarta_match:
            num = int(melakarta_match.group(1))
            if 1 <= num <= 72:
                current_melakarta_num = num
                # This is the melakarta itself — we already have it from build_melakarta_ragas()
                # But capture Wikipedia's arohanam/avarohanam for cross-reference
                raga_name = melakarta_match.group(2).strip()
                # Remove parenthetical suffixes like "(Janaka raga)", "(Sumadhyuti)"
                raga_name = re.sub(r"\s*\(.*?\)\s*", "", raga_name).strip()
                # Store as a wikipedia melakarta entry (will be merged later)
                arohanam = normalize_swara_notation(arohanam_raw)
                avarohanam = normalize_swara_notation(avarohanam_raw)
                ragas.append(Raga(
                    name=raga_name,
                    melakarta_number=num,
                    arohanam=arohanam,
                    avarohanam=avarohanam,
                    source="wikipedia",
                ))
                continue

        # This is a janya raga
        if current_melakarta_num is None:
            continue

        # Remove parenthetical notes like "(Raga created by ...)"
        clean_name = re.sub(r"\(.*?\)", "", name_text).strip()
        if not clean_name:
            continue

        arohanam = normalize_swara_notation(arohanam_raw)
        avarohanam = normalize_swara_notation(avarohanam_raw)

        ragas.append(Raga(
            name=clean_name,
            parent_melakarta=current_melakarta_num,
            arohanam=arohanam,
            avarohanam=avarohanam,
            source="wikipedia",
        ))

    n_janyas = sum(1 for r in ragas if r.melakarta_number is None)
    n_melas = sum(1 for r in ragas if r.melakarta_number is not None)
    print(f"  Found {n_melas} melakarta + {n_janyas} janya = {len(ragas)} ragas from Wikipedia")
    return ragas


# ---------------------------------------------------------------------------
# karnatik.com scraping
# ---------------------------------------------------------------------------

KARNATIK_INDEX_URL = "https://www.karnatik.com/ragas.shtml"
KARNATIK_BASE = "https://www.karnatik.com"


def fetch_karnatik_raga_index() -> dict[str, str]:
    """Fetch the raga index from karnatik.com → {normalized_name: detail_url}."""
    print(f"Fetching {KARNATIK_INDEX_URL} ...")
    headers = {"User-Agent": USER_AGENT}
    resp = requests.get(KARNATIK_INDEX_URL, headers=headers, timeout=30)
    resp.raise_for_status()

    soup = BeautifulSoup(resp.text, "html.parser")
    index: dict[str, str] = {}

    # The page has alphabetical links to sub-pages like ragasa.shtml, ragasb.shtml, etc.
    alpha_links = []
    for a in soup.find_all("a", href=True):
        href = a["href"]
        if re.match(r"ragas[a-z]\.shtml", href):
            full_url = f"{KARNATIK_BASE}/{href}"
            if full_url not in alpha_links:
                alpha_links.append(full_url)

    print(f"  Found {len(alpha_links)} alphabetical index pages")

    for url in alpha_links:
        time.sleep(0.5)  # be polite
        try:
            resp = requests.get(url, headers=headers, timeout=30)
            resp.raise_for_status()
            page_soup = BeautifulSoup(resp.text, "html.parser")

            # Each raga is typically a link to a detail page like ragazXYZ.shtml
            for a in page_soup.find_all("a", href=True):
                href = a["href"]
                raga_name = a.get_text(strip=True)
                if re.match(r"ragaz?\w+\.shtml", href) and raga_name:
                    normalized = normalize_name(raga_name)
                    if normalized and len(normalized) > 1:
                        detail_url = f"{KARNATIK_BASE}/{href}" if not href.startswith("http") else href
                        index[normalized] = detail_url
        except Exception as e:
            print(f"  Warning: failed to fetch {url}: {e}")

    print(f"  Indexed {len(index)} ragas from karnatik.com")
    return index


def fetch_karnatik_raga_detail(url: str) -> dict:
    """Fetch a single raga's detail page from karnatik.com."""
    headers = {"User-Agent": USER_AGENT}
    try:
        resp = requests.get(url, headers=headers, timeout=30)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")

        text = soup.get_text()
        info = {}

        # Extract arohanam/avarohanam
        aro_match = re.search(r"[Aa]roha[nm]+[aem]*\s*[:：]\s*([^\n]+)", text)
        if aro_match:
            info["arohanam"] = normalize_swara_notation(aro_match.group(1))

        ava_match = re.search(r"[Aa]varoha[nm]+[aem]*\s*[:：]\s*([^\n]+)", text)
        if ava_match:
            info["avarohanam"] = normalize_swara_notation(ava_match.group(1))

        # Extract melakarta reference
        mela_match = re.search(r"[Mm]ela(?:karta)?\s*[:：]?\s*(\d+)", text)
        if mela_match:
            info["melakarta_ref"] = int(mela_match.group(1))

        return info
    except Exception:
        return {}


# ---------------------------------------------------------------------------
# Merge and deduplicate
# ---------------------------------------------------------------------------

def merge_ragas(
    melakartas: list[Raga],
    wiki_ragas: list[Raga],
    karnatik_index: dict[str, str] | None = None,
) -> list[Raga]:
    """Merge all sources, deduplicate. Melakartas matched by number, janyas by name."""
    # Index built melakartas by number
    mela_by_num: dict[int, Raga] = {}
    for r in melakartas:
        if r.melakarta_number:
            mela_by_num[r.melakarta_number] = r

    # Separate wiki results into melakartas and janyas
    wiki_mela = [r for r in wiki_ragas if r.melakarta_number is not None]
    wiki_janya = [r for r in wiki_ragas if r.melakarta_number is None]

    # Update built melakarta names from Wikipedia (more authoritative spellings)
    for wr in wiki_mela:
        if wr.melakarta_number in mela_by_num:
            existing = mela_by_num[wr.melakarta_number]
            # Prefer Wikipedia's canonical name (diacritics stripped)
            existing.name = wr.name
            existing.name_normalized = wr.name_normalized
            # Keep our computed scales (Katapayadi formula is definitive)

    by_normalized: dict[str, Raga] = {}

    # 1. Melakartas first
    for r in mela_by_num.values():
        by_normalized[r.name_normalized] = r

    # 2. Wikipedia janyas
    for r in wiki_janya:
        key = r.name_normalized
        if key in by_normalized:
            existing = by_normalized[key]
            if not existing.arohanam and r.arohanam:
                existing.arohanam = r.arohanam
            if not existing.avarohanam and r.avarohanam:
                existing.avarohanam = r.avarohanam
            if existing.parent_melakarta is None and r.parent_melakarta:
                existing.parent_melakarta = r.parent_melakarta
        else:
            by_normalized[key] = r

    # 3. Cross-reference with karnatik.com if available
    if karnatik_index:
        checked = 0
        for norm_name, raga in list(by_normalized.items()):
            if norm_name in karnatik_index and (not raga.arohanam or not raga.avarohanam):
                if checked >= 50:
                    break
                detail = fetch_karnatik_raga_detail(karnatik_index[norm_name])
                if detail.get("arohanam") and not raga.arohanam:
                    raga.arohanam = detail["arohanam"]
                if detail.get("avarohanam") and not raga.avarohanam:
                    raga.avarohanam = detail["avarohanam"]
                if detail.get("melakarta_ref") and raga.parent_melakarta is None and raga.melakarta_number is None:
                    raga.parent_melakarta = detail["melakarta_ref"]
                checked += 1
                time.sleep(0.3)

    return list(by_normalized.values())


# ---------------------------------------------------------------------------
# SQL generation
# ---------------------------------------------------------------------------

SQL_HEADER = """\
-- =============================================================================
-- Raga Reference Data — Comprehensive Carnatic Raga Dataset
-- =============================================================================
-- Sources:
--   1. 72 Melakarta ragas (Venkatamakhi/Katapayadi scheme)
--   2. Janya ragas from Wikipedia "List of Janya ragas"
--   3. Cross-referenced with karnatik.com for canonical names
--
-- Generated by: tools/raga-reference-extractor/extract_ragas.py
-- Idempotent: uses ON CONFLICT (name_normalized) DO UPDATE
-- =============================================================================

-- First: Insert all melakarta ragas (parent_raga_id = NULL)
-- Then:  Insert janya ragas with parent_raga_id referencing their melakarta
"""


def generate_sql(ragas: list[Raga]) -> str:
    """Generate idempotent SQL for all ragas."""
    lines = [SQL_HEADER]

    # Separate melakartas and janyas
    melakartas = sorted(
        [r for r in ragas if r.melakarta_number is not None],
        key=lambda r: r.melakarta_number,
    )
    janyas = sorted(
        [r for r in ragas if r.melakarta_number is None],
        key=lambda r: (r.parent_melakarta or 0, r.name_normalized),
    )

    # --- Melakarta ragas ---
    lines.append("-- ---------------------------------------------------------------------------")
    lines.append("-- 72 Melakarta Ragas")
    lines.append("-- ---------------------------------------------------------------------------")
    lines.append("")

    for r in melakartas:
        lines.append(f"-- Melakarta #{r.melakarta_number}: {r.name}")
        lines.append(
            f"INSERT INTO ragas (id, name, name_normalized, melakarta_number, arohanam, avarohanam, created_at, updated_at)"
        )
        lines.append(
            f"VALUES (gen_random_uuid(), {sql_escape(r.name)}, {sql_escape(r.name_normalized)}, "
            f"{r.melakarta_number}, {sql_escape(r.arohanam)}, {sql_escape(r.avarohanam)}, NOW(), NOW())"
        )
        lines.append(
            f"ON CONFLICT (name_normalized) DO UPDATE SET "
            f"name = EXCLUDED.name, "
            f"melakarta_number = EXCLUDED.melakarta_number, "
            f"arohanam = EXCLUDED.arohanam, "
            f"avarohanam = EXCLUDED.avarohanam, "
            f"updated_at = NOW();"
        )
        lines.append("")

    # Build lookup: melakarta number → actual normalized name from merged data
    mela_norm_by_num: dict[int, str] = {}
    mela_display_by_num: dict[int, str] = {}
    for r in melakartas:
        mela_norm_by_num[r.melakarta_number] = r.name_normalized
        mela_display_by_num[r.melakarta_number] = r.name

    # --- Janya ragas ---
    lines.append("")
    lines.append("-- ---------------------------------------------------------------------------")
    lines.append("-- Janya Ragas (grouped by parent melakarta)")
    lines.append("-- ---------------------------------------------------------------------------")

    current_parent = None
    for r in janyas:
        if r.parent_melakarta != current_parent:
            current_parent = r.parent_melakarta
            parent_name = mela_display_by_num.get(current_parent, "Unknown") if current_parent else "Unknown"
            lines.append("")
            lines.append(f"-- --- Janyas of Melakarta #{current_parent}: {parent_name} ---")
            lines.append("")

        parent_ref = "NULL"
        if r.parent_melakarta and r.parent_melakarta in mela_norm_by_num:
            parent_norm = mela_norm_by_num[r.parent_melakarta]
            parent_ref = f"(SELECT id FROM ragas WHERE name_normalized = {sql_escape(parent_norm)})"

        arohanam_sql = sql_escape(r.arohanam) if r.arohanam else "NULL"
        avarohanam_sql = sql_escape(r.avarohanam) if r.avarohanam else "NULL"

        lines.append(
            f"INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)"
        )
        lines.append(
            f"VALUES (gen_random_uuid(), {sql_escape(r.name)}, {sql_escape(r.name_normalized)}, "
            f"{parent_ref}, {arohanam_sql}, {avarohanam_sql}, NOW(), NOW())"
        )
        # For janyas: clear any stale melakarta_number and set parent
        lines.append(
            f"ON CONFLICT (name_normalized) DO UPDATE SET "
            f"name = EXCLUDED.name, "
            f"melakarta_number = NULL, "
            f"parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id), "
            f"arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam), "
            f"avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam), "
            f"updated_at = NOW();"
        )
        lines.append("")

    # Summary comment
    lines.append(f"-- Total: {len(melakartas)} melakarta + {len(janyas)} janya = {len(ragas)} ragas")
    lines.append("")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Extract comprehensive Carnatic raga reference data")
    parser.add_argument("--output", "-o", default="output/raga_reference_data.sql",
                        help="Output SQL file path")
    parser.add_argument("--skip-wikipedia", action="store_true",
                        help="Skip Wikipedia scraping (use only melakarta data)")
    parser.add_argument("--skip-karnatik", action="store_true",
                        help="Skip karnatik.com cross-referencing")
    args = parser.parse_args()

    print("=" * 60)
    print("Raga Reference Data Extractor")
    print("=" * 60)

    # 1. Build 72 melakartas
    print("\n[1/4] Building 72 melakarta ragas...")
    melakartas = build_melakarta_ragas()
    print(f"  Built {len(melakartas)} melakarta ragas")

    # 2. Scrape Wikipedia
    wiki_janyas = []
    if not args.skip_wikipedia:
        print("\n[2/4] Scraping Wikipedia janya ragas...")
        try:
            wiki_janyas = fetch_wikipedia_janya_ragas()
        except Exception as e:
            print(f"  WARNING: Wikipedia scraping failed: {e}")
            print("  Continuing with melakarta data only...")
    else:
        print("\n[2/4] Skipping Wikipedia (--skip-wikipedia)")

    # 3. Cross-reference karnatik.com
    karnatik_index = None
    if not args.skip_karnatik:
        print("\n[3/4] Fetching karnatik.com raga index...")
        try:
            karnatik_index = fetch_karnatik_raga_index()
        except Exception as e:
            print(f"  WARNING: karnatik.com scraping failed: {e}")
    else:
        print("\n[3/4] Skipping karnatik.com (--skip-karnatik)")

    # 4. Merge and generate SQL
    print("\n[4/4] Merging and generating SQL...")
    all_ragas = merge_ragas(melakartas, wiki_janyas, karnatik_index)
    sql = generate_sql(all_ragas)

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(sql, encoding="utf-8")

    n_melakartas = sum(1 for r in all_ragas if r.melakarta_number is not None)
    n_janyas = sum(1 for r in all_ragas if r.melakarta_number is None)

    print(f"\n{'=' * 60}")
    print(f"Done! Generated {output_path}")
    print(f"  Melakarta ragas: {n_melakartas}")
    print(f"  Janya ragas:     {n_janyas}")
    print(f"  Total:           {len(all_ragas)}")
    print(f"{'=' * 60}")


if __name__ == "__main__":
    main()
