"""TOC-based extraction of Dikshitar Krithis from mdeng.pdf and mdskt.pdf.

Strategy:
1. Parse TOC (pages 2-16 in both PDFs) to get (number, title, page) for all 484 krithis
2. Use page ranges to extract each krithi's content
3. Parse raga/tala from metadata line
4. Detect section headers (pallavi, anupallavi, caranam, madhyamakala) via font style
5. Apply diacritic normalization for English PDF
6. Output eng_krithis.json, skt_krithis.json, krithi_comparison_matched.csv
"""

from __future__ import annotations

import csv
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

import fitz  # PyMuPDF

# ---------------------------------------------------------------------------
# Diacritic normalization for English/IAST text (Utopia font garbling)
# ---------------------------------------------------------------------------

# These patterns fix garbled IAST from Utopia-family fonts in mdeng.pdf
_DIACRITIC_RULES = [
    # FIRST: Dotless-i → i (must come before macron rules so ¯ı → ¯i → ī)
    (re.compile(r'ı'), 'i'),
    # Maltese Cross artifact
    (re.compile(r'✠'), ''),
    # Macron vowels
    (re.compile(r'¯a'), 'ā'), (re.compile(r'¯A'), 'Ā'),
    (re.compile(r'¯i'), 'ī'), (re.compile(r'¯I'), 'Ī'),
    (re.compile(r'¯u'), 'ū'), (re.compile(r'¯U'), 'Ū'),
    # Dot-above
    (re.compile(r'˙m'), 'ṁ'), (re.compile(r'˙n'), 'ṅ'),
    (re.compile(r'˙r'), 'ṛ'), (re.compile(r'˙M'), 'Ṁ'),
    # Acute
    (re.compile(r'´s'), 'ś'), (re.compile(r'´S'), 'Ś'),
    # Tilde
    (re.compile(r'˜n'), 'ñ'), (re.compile(r'˜N'), 'Ñ'),
]

# Consonant-dot patterns (applied after above rules)
_CONSONANT_DOT_RULES = [
    (re.compile(r'(?<![A-Z])s\.'), 'ṣ'),
    (re.compile(r'(?<![A-Z])n\.'), 'ṇ'),
    (re.compile(r'(?<![A-Z])d\.'), 'ḍ'),
    (re.compile(r'(?<![A-Z])t\.'), 'ṭ'),
    (re.compile(r'(?<![A-Z])l\.'), 'ḷ'),
    (re.compile(r'(?<![A-Z])r\.'), 'ṛ'),
    (re.compile(r'(?<![A-Z])h\.'), 'ḥ'),
    (re.compile(r'S\.'), 'Ṣ'),
    (re.compile(r'N\.'), 'Ṇ'),
    (re.compile(r'D\.'), 'Ḍ'),
    (re.compile(r'T\.'), 'Ṭ'),
    (re.compile(r'L\.'), 'Ḷ'),
    (re.compile(r'R\.'), 'Ṛ'),
    (re.compile(r'H\.'), 'Ḥ'),
]


def normalize_diacritics(text: str) -> str:
    """Fix garbled IAST diacritics from Utopia font extraction."""
    for pat, repl in _DIACRITIC_RULES:
        text = pat.sub(repl, text)
    for pat, repl in _CONSONANT_DOT_RULES:
        text = pat.sub(repl, text)
    # Collapse artifact spaces caused by Utopia font n./d./t. extraction:
    # Only collapse spaces between underdotted consonants and adjacent chars
    for _ in range(3):
        # "ṇḍ eśvari" -> "ṇḍeśvari" (underdot + space + letter)
        text = re.sub(r'([ṇṭḍṣḥ])\s+([a-zāīūṛḷṇṭḍṣḥśṁ])', r'\1\2', text)
        # "aṇ e" -> "aṇe" (letter + space + underdot)
        text = re.sub(r'([a-z])\s+([ṇṭḍṣḥ])', r'\1\2', text)
    # Collapse "mā ṁ" -> "māṁ" at word end (anusvara)
    text = re.sub(r'([a-zāīūṛḷeou])\s+ṁ(?=\s|$)', r'\1ṁ', text)
    # Collapse "saṁ pradāya" -> "saṁpradāya" (anusvara + space + consonant)
    text = re.sub(r'ṁ\s+(?=[bcdfghjklmnpqrstvwxyz])', 'ṁ', text)
    return text


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class TocEntry:
    number: int
    title: str
    page: int  # 1-based PDF page number


@dataclass
class KrithiSection:
    section_type: str  # pallavi, anupallavi, charanam, madhyamakala, samashti_charanam
    text: str


@dataclass
class ExtractedKrithi:
    number: int
    title: str
    raga: str = ""
    tala: str = ""
    sections: dict[str, str] = field(default_factory=dict)
    page_start: int = 0
    page_end: int = 0


# ---------------------------------------------------------------------------
# TOC Parsing
# ---------------------------------------------------------------------------

def _extract_page_num_from_line(line_text: str) -> int | None:
    """Extract trailing page number from a TOC line like 'title . . . 119'."""
    # Pure number
    if re.match(r'^\d+$', line_text.strip()):
        return int(line_text.strip())
    # Trailing number after dots
    m = re.search(r'(\d+)\s*$', line_text)
    if m:
        return int(m.group(1))
    return None


def _extract_title_from_line(line_text: str) -> str:
    """Extract title from a TOC line, stripping dots and trailing page number."""
    # Remove trailing page number
    text = re.sub(r'\s*\d+\s*$', '', line_text)
    # Remove dots
    text = re.sub(r'\s*[.\s]+$', '', text).strip()
    text = re.sub(r'^[.\s]+', '', text).strip()
    return text


def parse_toc_english(doc: fitz.Document) -> list[TocEntry]:
    """Parse TOC from pages 2-16 (0-indexed: 1-15) of English PDF.

    Block formats vary across pages:
    Format A (early pages): 3 lines — number / title+dots / page
    Format B (early pages): 4 lines — number / title / dots / page
    Format C (later pages): 2 lines — number / title+dots+page
    Format D (later pages): 3 lines — number / title / dots+page
    """
    entries = []
    for page_idx in range(1, 16):
        page = doc[page_idx]
        page_dict = page.get_text("dict")
        for block in page_dict["blocks"]:
            if "lines" not in block:
                continue
            block_lines = block["lines"]
            nlines = len(block_lines)

            if nlines < 2:
                continue

            # First line should always be the krithi number
            num_text = "".join(s["text"] for s in block_lines[0]["spans"]).strip()
            if not re.match(r'^\d+$', num_text):
                continue
            num = int(num_text)
            if num < 1 or num > 484:
                continue

            # Find the page number — it's either on its own last line,
            # or trailing at the end of the last line
            last_line = "".join(s["text"] for s in block_lines[-1]["spans"]).strip()
            page_num = _extract_page_num_from_line(last_line)

            if page_num is None or page_num < 1:
                continue

            # If last line is just the page number, title is in middle lines
            if re.match(r'^\d+$', last_line):
                # Format A/B: title is in lines 1..(n-1)
                title_parts = []
                for li in range(1, nlines - 1):
                    lt = "".join(s["text"] for s in block_lines[li]["spans"]).strip()
                    title_parts.append(_extract_title_from_line(lt))
                title = " ".join(p for p in title_parts if p)
            else:
                # Format C/D: page number at end of last line
                if nlines == 2:
                    # 2-line: number, title+dots+page
                    title = _extract_title_from_line(last_line)
                else:
                    # 3+ lines: number, title lines, dots+page
                    title_parts = []
                    for li in range(1, nlines - 1):
                        lt = "".join(s["text"] for s in block_lines[li]["spans"]).strip()
                        title_parts.append(_extract_title_from_line(lt))
                    # Also check if last line has title content before dots
                    last_title = _extract_title_from_line(last_line)
                    if last_title:
                        title_parts.append(last_title)
                    title = " ".join(p for p in title_parts if p)

            if title and page_num > 0:
                title_normalized = normalize_diacritics(title)
                entries.append(TocEntry(number=num, title=title_normalized, page=page_num))

    # Deduplicate by number
    seen = set()
    deduped = []
    for e in entries:
        if e.number not in seen:
            seen.add(e.number)
            deduped.append(e)
    return sorted(deduped, key=lambda e: e.number)


def parse_toc_sanskrit(doc: fitz.Document) -> list[TocEntry]:
    """Parse TOC from Sanskrit PDF.

    Same block structure logic as English. Numbers are Arabic.
    Velthuis font text is garbled - we use placeholder titles.
    """
    entries = []

    for page_idx in range(1, 16):
        page = doc[page_idx]
        page_dict = page.get_text("dict")
        for block in page_dict["blocks"]:
            if "lines" not in block:
                continue
            block_lines = block["lines"]
            nlines = len(block_lines)

            if nlines < 2:
                continue

            num_text = "".join(s["text"] for s in block_lines[0]["spans"]).strip()
            if not re.match(r'^\d+$', num_text):
                continue
            num = int(num_text)
            if num < 1 or num > 484:
                continue

            last_line = "".join(s["text"] for s in block_lines[-1]["spans"]).strip()
            page_num = _extract_page_num_from_line(last_line)

            if page_num is not None and page_num > 0 and 1 <= num <= 484:
                entries.append(TocEntry(number=num, title=f"krithi_{num}", page=page_num))

    seen = set()
    deduped = []
    for e in entries:
        if e.number not in seen:
            seen.add(e.number)
            deduped.append(e)
    return sorted(deduped, key=lambda e: e.number)


# ---------------------------------------------------------------------------
# Font-based text extraction with span-level metadata
# ---------------------------------------------------------------------------

@dataclass
class TextSpan:
    text: str
    font_size: float
    font_name: str
    is_bold: bool
    is_italic: bool
    bbox: tuple  # (x0, y0, x1, y1)
    page_num: int  # 0-indexed


def extract_spans_from_pages(doc: fitz.Document, start_page: int, end_page: int) -> list[TextSpan]:
    """Extract all text spans from a range of pages (0-indexed, inclusive)."""
    spans = []
    for page_idx in range(start_page, min(end_page + 1, len(doc))):
        page = doc[page_idx]
        page_dict = page.get_text("dict")
        for block in page_dict["blocks"]:
            if "lines" not in block:
                continue
            for line in block["lines"]:
                for span in line["spans"]:
                    text = span["text"]
                    if not text.strip():
                        continue
                    flags = span.get("flags", 0)
                    is_bold = bool(flags & (1 << 4)) or "Bold" in span.get("font", "")
                    is_italic = bool(flags & (1 << 1)) or "Italic" in span.get("font", "") or "Oblique" in span.get("font", "")
                    spans.append(TextSpan(
                        text=text,
                        font_size=round(span["size"], 1),
                        font_name=span.get("font", ""),
                        is_bold=is_bold,
                        is_italic=is_italic,
                        bbox=tuple(span["bbox"]),
                        page_num=page_idx,
                    ))
    return spans


# ---------------------------------------------------------------------------
# English Krithi Extraction
# ---------------------------------------------------------------------------

# Section header patterns
_SECTION_HEADERS = {
    "pallavi": "pallavi",
    "anupallavi": "anupallavi",
    "caranam": "charanam",
    "caraṇam": "charanam",
    "caraṇ am": "charanam",
    "charanam": "charanam",
    "charaṇam": "charanam",
    "samashticharanam": "samashti_charanam",
    "samasticaranam": "samashti_charanam",
    "samaṣṭicaraṇam": "samashti_charanam",
    "samaṣṭicaraṇ am": "samashti_charanam",
    "samashti charanam": "samashti_charanam",
    "vilomam": "vilomam",
    "nottusvara sāhityam": "pallavi",
    "noṭṭusvarasāhityam": "pallavi",
    "noṭṭusvara sāhityam": "pallavi",
    "notṭusvarasāhityam": "pallavi",
}

_MADHYAMAKALA_PATTERN = re.compile(r'madhyamak[āa]las[āa]hityam', re.IGNORECASE)
_RAGA_TALA_PATTERN = re.compile(
    r'(?:num\s+)?r[āa]ga\s*[ṁm]?\s*:?\s*(.+?)\s+t[āa][ḷl]a\s*[ṁm]?\s*:\s*(.+)',
    re.IGNORECASE
)
_RAGA_TALA_PATTERN2 = re.compile(
    r'(?:caturdaśa\s+)?r[āa]gam[āa]lik[āa]\s+t[āa][ḷl]a\s*[ṁm]?\s*:\s*(.+)',
    re.IGNORECASE
)

# Page number at bottom
_PAGE_NUM_PATTERN = re.compile(r'^\s*\d+\s*$')
# Footnote superscript number
_FOOTNOTE_PATTERN = re.compile(r'\d+\s*$')
# Maltese cross separator
_MALTESE_CROSS = re.compile(r'[✠✛✙✚]+')
# Footnote reference at page bottom
_FOOTNOTE_REF_PATTERN = re.compile(r'^\d+\s+\w')


def _clean_raga_name(name: str) -> str:
    """Clean raga name: strip melakarta number suffix, normalize."""
    name = name.strip()
    name = re.sub(r'\s*\(\d+\)\s*$', '', name)  # strip "(28)"
    name = re.sub(r'\s*\([^)]*\)\s*$', '', name)
    # Fix double-vowel OCR artefact: "āarabhi" -> "ārabhi"
    name = re.sub(r'^(ā)a', r'\1', name, flags=re.IGNORECASE)
    return name.strip()


def extract_krithi_english(doc: fitz.Document, toc_entry: TocEntry, next_page: int | None) -> ExtractedKrithi:
    """Extract a single krithi from English PDF using font metadata."""
    start_page_0 = toc_entry.page - 1  # Convert to 0-indexed
    if next_page is not None:
        end_page_0 = next_page - 2  # Page before next krithi starts
    else:
        end_page_0 = len(doc) - 1
    end_page_0 = min(end_page_0, len(doc) - 1)

    krithi = ExtractedKrithi(
        number=toc_entry.number,
        title=toc_entry.title,
        page_start=toc_entry.page,
        page_end=end_page_0 + 1,
    )

    # Extract all lines with font metadata
    lines = []
    for page_idx in range(start_page_0, end_page_0 + 1):
        page = doc[page_idx]
        page_dict = page.get_text("dict")
        for block in page_dict["blocks"]:
            if "lines" not in block:
                continue
            for line_data in block["lines"]:
                line_spans = line_data["spans"]
                raw_text = "".join(s["text"] for s in line_spans).strip()
                if not raw_text:
                    continue

                any_italic = any(
                    (bool(s.get("flags", 0) & (1 << 1)) or
                     "Slant" in s.get("font", "") or
                     "Italic" in s.get("font", "") or
                     "Oblique" in s.get("font", ""))
                    for s in line_spans if s["text"].strip()
                )
                any_bold = any(
                    (bool(s.get("flags", 0) & (1 << 4)) or
                     "Bold" in s.get("font", ""))
                    for s in line_spans if s["text"].strip()
                )
                max_font_size = max((s["size"] for s in line_spans if s["text"].strip()), default=0)
                # Check if all content spans use the MSAM font (Maltese crosses)
                all_msam = all("MSAM" in s.get("font", "") for s in line_spans if s["text"].strip())
                min_font_size = min((s["size"] for s in line_spans if s["text"].strip()), default=0)

                lines.append({
                    "raw": raw_text,
                    "text": normalize_diacritics(raw_text),
                    "italic": any_italic,
                    "bold": any_bold,
                    "font_size": max_font_size,
                    "min_font_size": min_font_size,
                    "all_msam": all_msam,
                    "page": page_idx,
                })

    # Parse lines
    current_section = None
    raga_found = False
    pending_tala = False  # tala may be on separate line

    for i, line_info in enumerate(lines):
        text = line_info["text"]
        raw = line_info["raw"]
        is_italic = line_info["italic"]
        is_bold = line_info["bold"]
        font_size = line_info["font_size"]
        min_fs = line_info["min_font_size"]

        # Skip Maltese cross lines
        if line_info["all_msam"]:
            continue

        # Skip page numbers (standalone number, 12pt regular, at bottom)
        if _PAGE_NUM_PATTERN.match(text) and font_size <= 12.5:
            continue

        # Skip footnote references (small font)
        if min_fs < 8 and re.match(r'^\d+\s+\w', text):
            continue

        # Parse raga/tala — may be on one or two lines
        if not raga_found:
            # Combined on one line: "rāgaṁ: jujāvanti (28)  tāḷaṁ: ādi"
            m = _RAGA_TALA_PATTERN.match(text)
            if m:
                krithi.raga = _clean_raga_name(m.group(1))
                krithi.tala = m.group(2).strip()
                raga_found = True
                continue

            # Ragamalika: "caturdaśa rāgamālikā  tāḷaṁ: ādi"
            m2 = _RAGA_TALA_PATTERN2.match(text)
            if m2:
                krithi.raga = "rāgamālikā"
                krithi.tala = m2.group(1).strip()
                raga_found = True
                continue

            # Ragamalika only (tala on next line): "aṣṭa(ṣaḍ)rāgamālikā"
            if re.search(r'r[āa]gam[āa]lik[āa]', text, re.IGNORECASE):
                krithi.raga = "rāgamālikā"
                pending_tala = True
                continue

            # Raga only on this line (tala on next): "num rāga ṁ: jujāvanti (28)"
            # Also handles "rāgam gīrvāṇi (43) :" format (colon after name)
            m3 = re.match(r'(?:num\s+)?r[āa]ga\s*[ṁm]?\s*:?\s+(.+?)(?:\s*:\s*)?$', text, re.IGNORECASE)
            if m3:
                krithi.raga = _clean_raga_name(m3.group(1).strip())
                pending_tala = True
                continue

            # Tala only: "tāḷa ṁ: ādi"
            if pending_tala:
                m4 = re.match(r't[āa][ḷl]a\s*[ṁm]?\s*:\s*(.+)', text, re.IGNORECASE)
                if m4:
                    krithi.tala = m4.group(1).strip()
                    raga_found = True
                    pending_tala = False
                    continue

            # Skip bold title lines before raga
            if is_bold:
                continue

            # Skip non-raga/tala lines before we find metadata
            continue

        # Detect section headers — italic text matching known section names
        text_stripped = text.strip()
        text_lower = text_stripped.lower()
        text_no_parens = text_lower.replace('(', '').replace(')', '').strip()

        # Madhyamakala header (italic, in parentheses)
        if _MADHYAMAKALA_PATTERN.search(text_no_parens):
            current_section = "madhyamakala"
            continue

        # Standard section headers (italic)
        if is_italic:
            # Normalize: collapse spaces and strip for matching
            text_collapsed = re.sub(r'\s+', '', text_no_parens)

            # Handle "pallavi", "anupallavi", "caraṇam", "vilomam"
            if text_no_parens in _SECTION_HEADERS:
                current_section = _SECTION_HEADERS[text_no_parens]
                continue
            # Also match collapsed (e.g. "caraṇ am" -> "caraṇam")
            if text_collapsed in _SECTION_HEADERS:
                current_section = _SECTION_HEADERS[text_collapsed]
                continue

            # Handle compound: "anupallavi ( samaṣṭicaraṇam)"
            parts = text_no_parens.split()
            if parts and (parts[0] in _SECTION_HEADERS or
                          re.sub(r'\s+', '', " ".join(parts[:2])) in _SECTION_HEADERS if len(parts) >= 2 else False):
                first_word = parts[0]
                if first_word in _SECTION_HEADERS:
                    current_section = _SECTION_HEADERS[first_word]
                rest = "".join(parts[1:])
                if "samashti" in rest or "samasti" in rest or "samaṣṭi" in rest or "samasṭi" in rest:
                    current_section = "samashti_charanam"
                continue

        # Skip bold lines after raga (these are title repetitions etc)
        if is_bold and font_size > 15:
            continue

        # For ragamalika with no standard section headers, default to "pallavi"
        if not current_section and raga_found and not is_bold and not is_italic:
            current_section = "pallavi"

        # Add lyric text to current section
        if current_section:
            # Strip trailing footnote numbers
            cleaned = re.sub(r'(?<=[a-zāīūṛḷṁṅñṇṭḍśṣḥ])\d{1,3}$', '', text_stripped).strip()
            if not cleaned:
                continue

            if current_section in krithi.sections:
                krithi.sections[current_section] += "\n" + cleaned
            else:
                krithi.sections[current_section] = cleaned

    return krithi


# ---------------------------------------------------------------------------
# Sanskrit Krithi Extraction (Velthuis-dvng fonts → Devanagari)
# ---------------------------------------------------------------------------

# Devanagari section headers
_DEVA_SECTION_MAP = {
    "पल्लवि": "pallavi",
    "पल्लवी": "pallavi",
    "अनुपल्लवि": "anupallavi",
    "अनुपल्लवी": "anupallavi",
    "चरणम्": "charanam",
    "चरणं": "charanam",
    "चरणम": "charanam",
    "समष्टिचरणम्": "samashti_charanam",
    "समष्टिचरणम": "samashti_charanam",
    "मध्यमकालसाहित्यम्": "madhyamakala",
    "मध्यमकालसाहित्यम": "madhyamakala",
    "विलोमम्": "vilomam",
    "विलोमम": "vilomam",
}

_DEVA_RAGA_PATTERN = re.compile(r'रागं?\s*[:：]\s*(.+?)\s+ताळं?\s*[:：]\s*(.+)', re.UNICODE)
_DEVA_RAGA_PATTERN2 = re.compile(r'राग\s*[:：]?\s*(.+?)\s*[-–—]\s*ताळ\s*[:：]?\s*(.+)', re.UNICODE)
_DEVA_RAGA_TALA_COMBINED = re.compile(r'रागं?\s*[:：]?\s*(.+?)[\s(（].*?ताळं?\s*[:：]?\s*(.+)', re.UNICODE)


def extract_krithi_sanskrit(doc: fitz.Document, toc_entry: TocEntry, next_page: int | None) -> ExtractedKrithi:
    """Extract a single krithi from Sanskrit PDF."""
    start_page_0 = toc_entry.page - 1
    if next_page is not None:
        end_page_0 = next_page - 2
    else:
        end_page_0 = len(doc) - 1
    end_page_0 = min(end_page_0, len(doc) - 1)

    krithi = ExtractedKrithi(
        number=toc_entry.number,
        title=toc_entry.title,
        page_start=toc_entry.page,
        page_end=end_page_0 + 1,
    )

    # Extract text from pages - for Sanskrit we use get_text("text") since
    # PyMuPDF handles the Velthuis font extraction better in text mode
    all_text = ""
    for page_idx in range(start_page_0, end_page_0 + 1):
        page = doc[page_idx]
        page_text = page.get_text("text")
        all_text += page_text + "\n"

    lines = [l.strip() for l in all_text.split("\n") if l.strip()]

    current_section = None
    raga_found = False

    for line in lines:
        # Skip page numbers (Devanagari or Arabic)
        if re.match(r'^[०-९\d]+$', line):
            continue

        # Skip Maltese crosses
        if _MALTESE_CROSS.search(line) and len(line.replace('✠', '').replace('✛', '').replace(' ', '')) < 5:
            continue

        # Parse raga/tala
        if not raga_found:
            m = _DEVA_RAGA_PATTERN.match(line)
            if not m:
                m = _DEVA_RAGA_TALA_COMBINED.match(line)
            if m:
                krithi.raga = re.sub(r'\s*[（(][^)）]*[)）]\s*$', '', m.group(1).strip())
                krithi.tala = m.group(2).strip()
                raga_found = True
                continue

        if not raga_found:
            continue

        # Check for section headers
        line_clean = line.strip()

        # Check parenthesized madhyamakala
        if 'मध्यमकाल' in line_clean:
            current_section = "madhyamakala"
            continue

        # Check Devanagari section headers
        matched_section = None
        for header, section_name in _DEVA_SECTION_MAP.items():
            if line_clean == header or line_clean.startswith(header):
                matched_section = section_name
                # Check for samashti qualifier
                if section_name == "anupallavi" and "समष्टि" in line_clean:
                    matched_section = "samashti_charanam"
                break

        if matched_section:
            current_section = matched_section
            continue

        # Add text to current section
        if current_section:
            if current_section in krithi.sections:
                krithi.sections[current_section] += "\n" + line_clean
            else:
                krithi.sections[current_section] = line_clean

    return krithi


# ---------------------------------------------------------------------------
# Main extraction pipeline
# ---------------------------------------------------------------------------

def extract_all_english(pdf_path: str) -> list[ExtractedKrithi]:
    """Extract all krithis from English PDF."""
    doc = fitz.open(pdf_path)
    toc = parse_toc_english(doc)
    print(f"Parsed {len(toc)} TOC entries from English PDF")

    if len(toc) < 480:
        print(f"WARNING: Expected ~484 TOC entries, got {len(toc)}")

    krithis = []
    for i, entry in enumerate(toc):
        next_page = toc[i + 1].page if i + 1 < len(toc) else None
        krithi = extract_krithi_english(doc, entry, next_page)
        krithis.append(krithi)

        # Progress
        if (i + 1) % 50 == 0:
            print(f"  Extracted {i + 1}/{len(toc)} English krithis...")

    doc.close()
    return krithis


def extract_all_sanskrit(pdf_path: str) -> list[ExtractedKrithi]:
    """Extract all krithis from Sanskrit PDF."""
    doc = fitz.open(pdf_path)
    toc = parse_toc_sanskrit(doc)
    print(f"Parsed {len(toc)} TOC entries from Sanskrit PDF")

    if len(toc) < 480:
        print(f"WARNING: Expected ~484 TOC entries, got {len(toc)}")

    krithis = []
    for i, entry in enumerate(toc):
        next_page = toc[i + 1].page if i + 1 < len(toc) else None
        krithi = extract_krithi_sanskrit(doc, entry, next_page)
        krithis.append(krithi)

        if (i + 1) % 50 == 0:
            print(f"  Extracted {i + 1}/{len(toc)} Sanskrit krithis...")

    doc.close()
    return krithis


def to_json(krithis: list[ExtractedKrithi]) -> list[dict]:
    """Convert extracted krithis to JSON-serializable format."""
    result = []
    for k in krithis:
        result.append({
            "id": k.number,
            "title": k.title,
            "title_is_numbered": True,
            "raga": k.raga,
            "tala": k.tala,
            "sections": k.sections,
            "page_range": f"{k.page_start}-{k.page_end}",
        })
    return result


def generate_matched_csv(eng_krithis: list[ExtractedKrithi], skt_krithis: list[ExtractedKrithi], output_path: str):
    """Generate matched CSV from English and Sanskrit krithis by krithi number."""
    skt_by_num = {k.number: k for k in skt_krithis}

    with open(output_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["Title-EN", "Title-SA", "Raga-EN", "Tala-EN"])
        writer.writeheader()
        for ek in eng_krithis:
            sk = skt_by_num.get(ek.number)
            writer.writerow({
                "Title-EN": ek.title,
                "Title-SA": sk.title if sk else "",
                "Raga-EN": ek.raga,
                "Tala-EN": ek.tala,
            })


def print_stats(label: str, krithis: list[ExtractedKrithi]):
    """Print extraction statistics."""
    total = len(krithis)
    with_pallavi = sum(1 for k in krithis if "pallavi" in k.sections)
    with_anupallavi = sum(1 for k in krithis if "anupallavi" in k.sections)
    with_charanam = sum(1 for k in krithis if "charanam" in k.sections)
    with_madhyamakala = sum(1 for k in krithis if "madhyamakala" in k.sections)
    with_samashti = sum(1 for k in krithis if "samashti_charanam" in k.sections)
    with_raga = sum(1 for k in krithis if k.raga)
    with_tala = sum(1 for k in krithis if k.tala)
    empty_sections = sum(1 for k in krithis if not k.sections)

    print(f"\n{'='*60}")
    print(f"  {label} — {total} krithis extracted")
    print(f"{'='*60}")
    print(f"  With raga:          {with_raga}/{total}")
    print(f"  With tala:          {with_tala}/{total}")
    print(f"  With pallavi:       {with_pallavi}/{total}")
    print(f"  With anupallavi:    {with_anupallavi}/{total}")
    print(f"  With charanam:      {with_charanam}/{total}")
    print(f"  With madhyamakala:  {with_madhyamakala}/{total}")
    print(f"  With samashti:      {with_samashti}/{total}")
    print(f"  Empty sections:     {empty_sections}/{total}")

    if empty_sections > 0:
        print(f"\n  Krithis with empty sections:")
        for k in krithis:
            if not k.sections:
                print(f"    #{k.number}: {k.title} (pages {k.page_start}-{k.page_end})")


def main():
    base_dir = Path(__file__).parent

    eng_pdf = str(base_dir / "mdeng.pdf")
    skt_pdf = str(base_dir / "mdskt.pdf")

    print("=" * 60)
    print("  TOC-Based Dikshitar Krithi Extraction")
    print("=" * 60)

    # Extract English
    print("\n--- Extracting from English PDF ---")
    eng_krithis = extract_all_english(eng_pdf)
    print_stats("English (IAST)", eng_krithis)

    # Extract Sanskrit
    print("\n--- Extracting from Sanskrit PDF ---")
    skt_krithis = extract_all_sanskrit(skt_pdf)
    print_stats("Sanskrit (Devanagari)", skt_krithis)

    # Write JSON files
    eng_json_path = str(base_dir / "eng_krithis.json")
    skt_json_path = str(base_dir / "skt_krithis.json")
    csv_path = str(base_dir / "krithi_comparison_matched.csv")

    with open(eng_json_path, "w", encoding="utf-8") as f:
        json.dump(to_json(eng_krithis), f, ensure_ascii=False, indent=2)
    print(f"\nWrote {len(eng_krithis)} krithis to {eng_json_path}")

    with open(skt_json_path, "w", encoding="utf-8") as f:
        json.dump(to_json(skt_krithis), f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(skt_krithis)} krithis to {skt_json_path}")

    # Generate matched CSV
    generate_matched_csv(eng_krithis, skt_krithis, csv_path)
    print(f"Wrote matched CSV to {csv_path}")

    # Spot check first 5
    print("\n--- Spot Check: First 5 English Krithis ---")
    for k in eng_krithis[:5]:
        print(f"\n  #{k.number}: {k.title}")
        print(f"    Raga: {k.raga}, Tala: {k.tala}")
        for sec, text in k.sections.items():
            preview = text[:80].replace('\n', ' ')
            print(f"    {sec}: {preview}...")

    # Check krithis with issues
    print("\n--- Krithis missing pallavi ---")
    for k in eng_krithis:
        if "pallavi" not in k.sections and k.sections:
            print(f"  #{k.number}: {k.title} — sections: {list(k.sections.keys())}")

    print("\nDone!")


if __name__ == "__main__":
    main()
