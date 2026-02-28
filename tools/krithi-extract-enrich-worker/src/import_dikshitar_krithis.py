"""Import matched CSV + JSON into PostgreSQL.

Fixes applied (TRACK-070 Phase 3):
  3.1  primary_language hardcoded 'te' → detect from composer ('sa' for Dikshitar)
  3.2  Section ordering: canonical musical order instead of dict.keys()
  3.4  Sanskrit section fuzzy key matching (normalised key lookup)
  3.5  clean_lyric_text() applied to all section text before INSERT
  3.6  en_lyrics_full / sa_lyrics_full built in canonical order
  3.7  Raga name: strip melakarta suffixes + double-vowel artefacts + .strip()
  3.8  Tala name normalised to lowercase
  3.9  AUDIT_LOG INSERT per krithi (project requirement)
  3.10 logger.info moved outside inner section loop (was firing once per section)

Ref: application_documentation/04-database/schema.md
"""

from __future__ import annotations

import csv
import json
import logging
import re
import sys
import uuid
from datetime import datetime, timezone
from typing import Any

import psycopg
from psycopg.rows import dict_row

from .config import ExtractorConfig
from .db import ExtractionQueueDB

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Canonical section ordering  (3.2)
# ---------------------------------------------------------------------------

CANONICAL_ORDER: list[str] = [
    "pallavi",
    "anupallavi",
    "charanam",
    "samashti_charanam",
    "madhyamakala",
    "chittaswaram",
    "swara_sahitya",
]

# Map JSON key → DB section_type enum value
_SECTION_TYPE_MAP: dict[str, str] = {
    "pallavi": "PALLAVI",
    "anupallavi": "ANUPALLAVI",
    "charanam": "CHARANAM",
    "samashti_charanam": "SAMASHTI_CHARANAM",
    "samashti charanam": "SAMASHTI_CHARANAM",
    "madhyamakala": "MADHYAMA_KALA",
    "chittaswaram": "CHITTASWARAM",
    "swara_sahitya": "SWARA_SAHITYA",
}


def _normalise_section_key(key: str) -> str:
    """Normalise a raw JSON section key to a canonical string."""
    return key.lower().strip().replace(" ", "_").replace("-", "_")


def _section_type(raw_key: str) -> str:
    norm = _normalise_section_key(raw_key)
    return _SECTION_TYPE_MAP.get(norm, _SECTION_TYPE_MAP.get(raw_key.lower(), "OTHER"))


def ordered_sections(sections: dict) -> list[tuple[str, str]]:
    """Return (raw_key, text) pairs in canonical musical order.

    Unknown keys are appended after the canonical set.
    """
    normalised = {_normalise_section_key(k): (k, v) for k, v in sections.items()}
    result: list[tuple[str, str]] = []
    seen: set[str] = set()
    for canonical_key in CANONICAL_ORDER:
        if canonical_key in normalised:
            raw_key, text = normalised[canonical_key]
            result.append((raw_key, text))
            seen.add(canonical_key)
    # Append anything not in the canonical list
    for norm_key, (raw_key, text) in normalised.items():
        if norm_key not in seen:
            result.append((raw_key, text))
    return result


# ---------------------------------------------------------------------------
# Sanskrit section fuzzy matching  (3.4)
# ---------------------------------------------------------------------------

def find_sa_section(sa_sections: dict, canonical_key: str) -> str:
    """Return Sanskrit text for canonical_key using normalised lookup."""
    # Direct match first
    if canonical_key in sa_sections:
        return sa_sections[canonical_key]
    # Normalised match
    for k, v in sa_sections.items():
        if _normalise_section_key(k) == canonical_key:
            return v
    return ""


# ---------------------------------------------------------------------------
# Text cleaning  (3.5)
# ---------------------------------------------------------------------------

_RE_IMAGE = re.compile(r'!\[.*?\]\(data:[^)]+\)', re.DOTALL)
_RE_DEVANAGARI_LEADING = re.compile(r'^[०-९]+\s+', re.MULTILINE)
_RE_ARABIC_LINE = re.compile(r'^\s*\d{1,3}\s*$', re.MULTILINE)
_RE_MULTI_BLANK = re.compile(r'\n{3,}')


def clean_lyric_text(text: str) -> str:
    """Remove extraction artefacts from lyric text before DB insert."""
    text = _RE_IMAGE.sub('', text)
    text = _RE_DEVANAGARI_LEADING.sub('', text)
    text = _RE_ARABIC_LINE.sub('', text)
    text = _RE_MULTI_BLANK.sub('\n\n', text)
    return text.strip()


# ---------------------------------------------------------------------------
# Name normalisation helpers  (3.7, 3.8)
# ---------------------------------------------------------------------------

_RE_MELAKARTA_SUFFIX = re.compile(r'\s*\([^)]+\)\s*$')
# Catches "āarabhi" → "ārabhi": macron-a followed by plain a at word start
_RE_MACRON_PLAIN_A = re.compile(r'^(ā)a', re.IGNORECASE)


def normalise_raga_name(name: str) -> str:
    """Strip melakarta numbers, double-vowel OCR artefacts, and whitespace."""
    name = name.strip()
    # Strip trailing parenthetical e.g. "jujāvanti (28)" → "jujāvanti"
    name = _RE_MELAKARTA_SUFFIX.sub('', name).strip()
    # Strip Devanagari suffix parenthetical e.g. "जुजावन्ति (२८)"
    name = re.sub(r'\s*（[^）]*）\s*$', '', name).strip()
    name = re.sub(r'\s*\([^)]*\)\s*$', '', name).strip()
    # Fix "āa..." OCR artefact (macron-a followed by redundant plain a)  (3.7)
    name = _RE_MACRON_PLAIN_A.sub(r'\1', name)
    return name


def normalise_tala_name(name: str) -> str:
    """Lowercase and strip whitespace for tala lookup."""
    return name.strip().lower()


# ---------------------------------------------------------------------------
# Language detection  (3.1)
# ---------------------------------------------------------------------------

_DIKSHITAR_NAMES = {
    "muthuswami dikshitar",
    "muthuswami dikshithar",
    "dikshitar",
    "dikshithar",
}


def detect_primary_language(composer_name: str) -> str:
    """Return ISO 639-1 language code based on composer convention."""
    if composer_name.strip().lower() in _DIKSHITAR_NAMES:
        return "sa"  # Dikshitar composed exclusively in Sanskrit
    # Default: Telugu for Tyagaraja; others can be extended here
    return "te"


# ---------------------------------------------------------------------------
# Main import
# ---------------------------------------------------------------------------

def main() -> None:
    logging.basicConfig(level=logging.INFO)

    csv_path = "../../database/for_import/krithi_comparison_matched.csv"

    config = ExtractorConfig()
    db = ExtractionQueueDB(config)
    db.connect()

    try:
        with open(csv_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            rows = list(reader)

        with open("../../database/for_import/eng_krithis.json", "r", encoding="utf-8") as f:
            eng_data = json.load(f)
            eng_dict = {k["title"].strip(): k.get("sections", {}) for k in eng_data}

        with open("../../database/for_import/skt_krithis.json", "r", encoding="utf-8") as f:
            skt_data = json.load(f)
            skt_dict = {k["title"].strip(): k.get("sections", {}) for k in skt_data}

        logger.info("Loaded %d krithis from %s", len(rows), csv_path)

        with db.conn.transaction():
            cur = db.conn.cursor()

            # Resolve composer
            cur.execute("SELECT id, name FROM composers WHERE name = 'Muthuswami Dikshitar'")
            composer_row = cur.fetchone()
            if not composer_row:
                raise Exception("Muthuswami Dikshitar composer not found in seed data")
            composer_id = composer_row["id"]
            composer_name = composer_row["name"]
            primary_language = detect_primary_language(composer_name)  # 3.1

            for index, row in enumerate(rows):
                title_en = row.get("Title-EN", "").strip()
                title_sa = row.get("Title-SA", "").strip()
                raga_en_raw = row.get("Raga-EN", "").strip()
                tala_en_raw = row.get("Tala-EN", "").strip()

                raga_en = normalise_raga_name(raga_en_raw)   # 3.7
                tala_en = normalise_tala_name(tala_en_raw)   # 3.8

                sections_en_raw = eng_dict.get(title_en, {})
                sections_sa_raw = skt_dict.get(title_sa, {})

                # Ensure Raga
                raga_norm = raga_en.lower()
                cur.execute("SELECT id FROM ragas WHERE name_normalized = %s", (raga_norm,))
                raga_row = cur.fetchone()
                if raga_row:
                    raga_id = raga_row["id"]
                elif raga_en:
                    raga_id = str(uuid.uuid4())
                    cur.execute(
                        "INSERT INTO ragas (id, name, name_normalized, created_at, updated_at)"
                        " VALUES (%s, %s, %s, NOW(), NOW())",
                        (raga_id, raga_en, raga_norm),
                    )
                else:
                    raga_id = None

                # Ensure Tala
                tala_norm = tala_en
                cur.execute("SELECT id FROM talas WHERE name_normalized = %s", (tala_norm,))
                tala_row = cur.fetchone()
                if tala_row:
                    tala_id = tala_row["id"]
                elif tala_en:
                    tala_id = str(uuid.uuid4())
                    cur.execute(
                        "INSERT INTO talas (id, name, name_normalized, created_at, updated_at)"
                        " VALUES (%s, %s, %s, NOW(), NOW())",
                        (tala_id, tala_en, tala_norm),
                    )
                else:
                    tala_id = None

                # Auto-populate incipit from first line of pallavi (6.3)
                pallavi_text = sections_en_raw.get("pallavi", "")
                incipit = pallavi_text.split("\n")[0].strip()[:120] if pallavi_text else ""

                # 1. Create the Krithi record
                krithi_id = str(uuid.uuid4())
                title_normalized = title_en.lower()

                cur.execute(
                    """
                    INSERT INTO krithis (
                        id, title, title_normalized, composer_id, primary_raga_id, tala_id,
                        workflow_state, musical_form, is_ragamalika, primary_language,
                        created_at, updated_at
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s,
                        'draft', 'KRITHI', false, %s,
                        NOW(), NOW()
                    )
                    """,
                    (
                        krithi_id, title_en, title_normalized,
                        composer_id, raga_id, tala_id,
                        primary_language,  # 3.1 — no longer hardcoded
                    ),
                )

                # 1b. Populate krithi_ragas junction table from primary_raga_id.
                # The search API joins through this table to return ragas in the listing.
                if raga_id:
                    cur.execute(
                        """
                        INSERT INTO krithi_ragas (krithi_id, raga_id, order_index)
                        VALUES (%s, %s, 0)
                        ON CONFLICT DO NOTHING
                        """,
                        (krithi_id, raga_id),
                    )

                # 2. Extract canonical sections in musical order  (3.2)
                sections_ordered = ordered_sections(sections_en_raw)

                krithi_section_ids: dict[str, str] = {}
                for order_idx, (raw_key, _text) in enumerate(sections_ordered, start=1):
                    ks_id = str(uuid.uuid4())
                    section_type = _section_type(raw_key)
                    cur.execute(
                        """
                        INSERT INTO krithi_sections
                            (id, krithi_id, section_type, order_index, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, NOW(), NOW())
                        """,
                        (ks_id, krithi_id, section_type, order_idx),
                    )
                    krithi_section_ids[raw_key] = ks_id

                # 3. Create English Lyric Variant
                var_en_id = str(uuid.uuid4())
                en_lyrics_full = "\n\n".join(          # 3.6 — canonical order
                    clean_lyric_text(text)
                    for _, text in sections_ordered
                    if text.strip()
                )
                cur.execute(
                    """
                    INSERT INTO krithi_lyric_variants (
                        id, krithi_id, language, script, is_primary,
                        source_reference, lyrics, created_at, updated_at
                    ) VALUES (%s, %s, 'en', 'latin', true, 'Matched CSV Import', %s, NOW(), NOW())
                    """,
                    (var_en_id, krithi_id, en_lyrics_full),
                )

                # 4. Attach English Lyric Sections  (3.5 clean applied)
                for raw_key, ks_id in krithi_section_ids.items():
                    if raw_key in sections_en_raw:
                        cleaned = clean_lyric_text(sections_en_raw[raw_key])
                        if cleaned:
                            cur.execute(
                                """
                                INSERT INTO krithi_lyric_sections
                                    (lyric_variant_id, section_id, text, created_at, updated_at)
                                VALUES (%s, %s, %s, NOW(), NOW())
                                """,
                                (var_en_id, ks_id, cleaned),
                            )

                # 5. Create Sanskrit Lyric Variant
                var_sa_id = str(uuid.uuid4())
                # Build full Sanskrit text using canonical section order  (3.6)
                sa_texts_ordered = [
                    clean_lyric_text(find_sa_section(sections_sa_raw, _normalise_section_key(raw_key)))  # 3.4
                    for raw_key, _ in sections_ordered
                ]
                sa_lyrics_full = "\n\n".join(t for t in sa_texts_ordered if t)
                cur.execute(
                    """
                    INSERT INTO krithi_lyric_variants (
                        id, krithi_id, language, script, is_primary,
                        source_reference, lyrics, created_at, updated_at
                    ) VALUES (%s, %s, 'sa', 'devanagari', false, 'Matched CSV Import', %s, NOW(), NOW())
                    """,
                    (var_sa_id, krithi_id, sa_lyrics_full),
                )

                # 6. Attach Sanskrit Lyric Sections  (3.4 fuzzy match + 3.5 clean)
                for raw_key, ks_id in krithi_section_ids.items():
                    canonical_key = _normalise_section_key(raw_key)
                    sa_text = clean_lyric_text(find_sa_section(sections_sa_raw, canonical_key))
                    if sa_text:
                        cur.execute(
                            """
                            INSERT INTO krithi_lyric_sections
                                (lyric_variant_id, section_id, text, created_at, updated_at)
                            VALUES (%s, %s, %s, NOW(), NOW())
                            """,
                            (var_sa_id, ks_id, sa_text),
                        )

                # 7. AUDIT_LOG entry  (3.9)
                cur.execute(
                    """
                    INSERT INTO audit_log (
                        action, entity_table, entity_id, changed_at, metadata
                    ) VALUES ('IMPORT', 'krithis', %s, NOW(), %s)
                    """,
                    (
                        krithi_id,
                        json.dumps({
                            "title": title_en,
                            "composer": composer_name,
                            "raga": raga_en,
                            "tala": tala_en,
                            "source": "import_matched_csv",
                        }),
                    ),
                )

                # 3.10: logger outside inner section loop
                logger.info("Imported krithi [%d/%d]: %s (%s)", index + 1, len(rows), title_en, krithi_id)

        logger.info("Import completed successfully")

    except Exception as e:
        logger.error("Failed to import: %s", e, exc_info=True)
    finally:
        db.close()


if __name__ == "__main__":
    main()
