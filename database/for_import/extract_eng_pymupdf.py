"""PDF text extraction for mdeng.pdf using PyMuPDF block-level parsing.

Replaces the original page.get_text("markdown") approach which:
  - Inlined base64 JPEG images into the text stream
  - Provided no font metadata for section header detection

This version:
  - Uses page.get_text("dict") to get per-span font metadata
  - Detects section headers (pallavi / anupallavi / charanam / madhyamakala)
    by checking for italic font rendering — labels in mdeng.pdf are italic
  - Skips image blocks entirely (block type != 0)
  - Strips standalone page numbers and entry numbers from lyric lines
  - Emits a markdown file with explicit ## Section: <name> markers so
    clean_markdown_files.py and the JSON builder can parse boundaries cleanly
  - Runs a post-extraction validation pass and prints warnings

Ref: TRACK-070 Phase 1 (1.1 – 1.5)
"""

from __future__ import annotations

import re
import sys
import os
import logging
from dataclasses import dataclass, field
from typing import Optional

import fitz  # PyMuPDF

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Section header vocabulary
# ---------------------------------------------------------------------------

# Canonical keys used in the output markdown and downstream JSON.
SECTION_CANONICAL: dict[str, str] = {
    "pallavi": "pallavi",
    "anupallavi": "anupallavi",
    "caranam": "charanam",
    "charanam": "charanam",
    "caraNam": "charanam",
    "madhyamakala": "madhyamakala",
    "madhyamakālasāhityam": "madhyamakala",
    "madhyamakala sahityam": "madhyamakala",
    "samashti charanam": "samashti_charanam",
    "samaṣṭi caraṇam": "samashti_charanam",
    "chittaswaram": "chittaswaram",
    "citta svaram": "chittaswaram",
}

# Regex patterns for labels that appear with diacritics/encoding variants in the PDF.
# The PDF uses Velthuis-style ASCII transliteration (e.g. n. = ṇ, ¯a = ā, ´s = ś).
_SECTION_PATTERNS: list[tuple[re.Pattern, str]] = [
    # pallavi / anupallavi — straightforward
    (re.compile(r'^\s*pallavi\s*:?\s*$', re.IGNORECASE), "pallavi"),
    (re.compile(r'^\s*anupallavi\s*:?\s*$', re.IGNORECASE), "anupallavi"),
    # caranam variants: "caranam", "caraṇam", "caran. am", "charanam"
    (re.compile(r'^\s*c[ah]?aran?[.˙]?\s*a\s*m\s*:?\s*$', re.IGNORECASE), "charanam"),
    (re.compile(r'^\s*charanam\s*:?\s*$', re.IGNORECASE), "charanam"),
    # madhyamakala — may appear with diacritics/macrons, parentheses, or extra words.
    # The PDF encodes ā as U+00AF (¯) followed by 'a', so we use a flexible pattern.
    (re.compile(r'madhyamak.{0,6}la', re.IGNORECASE), "madhyamakala"),
    # samashti charanam
    (re.compile(r'^\s*samash?ti\s+c[ah]?aran?', re.IGNORECASE), "samashti_charanam"),
    # chittaswaram
    (re.compile(r'^\s*citta\s+svaram|chittaswaram', re.IGNORECASE), "chittaswaram"),
]


def _normalise_label(text: str) -> Optional[str]:
    """Return canonical section key if text is a recognised section label, else None."""
    stripped = text.strip().lower().rstrip(":")
    # Direct dictionary match first (fastest path)
    if stripped in SECTION_CANONICAL:
        return SECTION_CANONICAL[stripped]
    # Prefix match for multi-word labels
    for label, canonical in SECTION_CANONICAL.items():
        if stripped.startswith(label):
            return canonical
    # Regex match for diacritic/encoding variants
    for pattern, canonical in _SECTION_PATTERNS:
        if pattern.search(text.strip()):
            return canonical
    return None


# ---------------------------------------------------------------------------
# Font-based span classification
# ---------------------------------------------------------------------------

def _is_italic(font_name: str, font_flags: int) -> bool:
    """Return True if the span uses an italic font.

    PyMuPDF font flags: bit 1 (value 2) = italic.
    Also check font name as a fallback.
    """
    if font_flags & 2:
        return True
    lower = font_name.lower()
    return "italic" in lower or "oblique" in lower or lower.endswith("it")


def _is_bold(font_name: str, font_flags: int) -> bool:
    """Return True if the span uses a bold font (bit 4 = 16)."""
    if font_flags & 16:
        return True
    lower = font_name.lower()
    return "bold" in lower


# ---------------------------------------------------------------------------
# Text cleaning
# ---------------------------------------------------------------------------

# Matches markdown inline images — catches both data-URI and normal image refs.
_RE_IMAGE = re.compile(r'!\[.*?\]\([^)]*\)', re.DOTALL)

# Standalone Devanagari numeral line (e.g. "३" at start of line = entry number).
_RE_DEVANAGARI_NUM = re.compile(r'^[०-९]+\s*', re.MULTILINE)

# Standalone Arabic digit line (1–3 digits, optional whitespace) — page / entry numbers.
_RE_ARABIC_NUM_LINE = re.compile(r'^\s*\d{1,3}\s*$', re.MULTILINE)

# Leading Arabic entry number at start of a lyric line, e.g. "2 akhilāṇḍeśvaryai…"
_RE_LEADING_NUM = re.compile(r'^\s*\d{1,3}\s+', re.MULTILINE)

# 3+ blank lines → 2 blank lines.
_RE_MULTI_BLANK = re.compile(r'\n{3,}')


def clean_lyric_text(text: str) -> str:
    """Remove artefacts from extracted lyric text."""
    # Strip markdown images (base64 or otherwise)
    text = _RE_IMAGE.sub('', text)
    # Strip standalone Devanagari numeral lines
    text = _RE_DEVANAGARI_NUM.sub('', text)
    # Strip standalone Arabic numeral-only lines (page numbers)
    text = _RE_ARABIC_NUM_LINE.sub('', text)
    # Strip leading Arabic entry-number prefix on lyric lines
    text = _RE_LEADING_NUM.sub('', text)
    # Collapse excess blank lines
    text = _RE_MULTI_BLANK.sub('\n\n', text)
    return text.strip()


# ---------------------------------------------------------------------------
# Block-level page extraction
# ---------------------------------------------------------------------------

@dataclass
class _Section:
    key: str
    lines: list[str] = field(default_factory=list)

    def text(self) -> str:
        return clean_lyric_text("\n".join(self.lines))


@dataclass
class _KrithiEntry:
    """Accumulator for one krithi's worth of page content."""
    entry_lines: list[str] = field(default_factory=list)  # pre-section header lines
    sections: dict[str, _Section] = field(default_factory=dict)
    current_section: Optional[str] = None

    def add_line(self, line: str) -> None:
        if self.current_section:
            self.sections[self.current_section].lines.append(line)
        else:
            self.entry_lines.append(line)

    def open_section(self, canonical_key: str) -> None:
        self.current_section = canonical_key
        if canonical_key not in self.sections:
            self.sections[canonical_key] = _Section(key=canonical_key)


def _extract_page_blocks(page: fitz.Page, page_number: int) -> list[tuple[str, bool, bool, float]]:
    """Extract (text, is_italic, is_bold, font_size) tuples per span, skipping images.

    Returns one tuple per non-empty span, in reading order.
    """
    page_dict = page.get_text("dict", flags=fitz.TEXT_PRESERVE_WHITESPACE)
    results: list[tuple[str, bool, bool, float]] = []

    for block in page_dict.get("blocks", []):
        if block.get("type") != 0:  # Skip image blocks (type 1)
            continue
        for line in block.get("lines", []):
            line_parts: list[tuple[str, bool, bool, float]] = []
            for span in line.get("spans", []):
                text = span.get("text", "")
                if not text.strip():
                    continue
                flags = span.get("flags", 0)
                font_name = span.get("font", "")
                font_size = span.get("size", 0.0)
                italic = _is_italic(font_name, flags)
                bold = _is_bold(font_name, flags)
                line_parts.append((text, italic, bold, font_size))
            results.extend(line_parts)
        # Add a sentinel for line boundary after each block's lines
        if block.get("lines"):
            results.append(("\n", False, False, 0.0))

    return results


# ---------------------------------------------------------------------------
# Main extraction
# ---------------------------------------------------------------------------

ENTRY_SEPARATOR = "---"  # Written between krithi entries in the output markdown


def extract_to_markdown(pdf_path: str, output_path: str) -> None:
    logger.info("Opening %s", pdf_path)
    doc = fitz.open(pdf_path)
    total_pages = len(doc)
    logger.info("Total pages: %d", total_pages)

    output_lines: list[str] = []
    validation_errors: list[str] = []

    # We accumulate lines per "entry" (a krithi block in the PDF).
    # The PDF has one or more entries per page; each entry starts with a bold
    # title line. We detect entry boundaries by: a bold line that looks like
    # a krithi title (short, ≤ 120 chars, appears before a pallavi label).
    #
    # For Phase 1 the primary goal is correct section boundaries, so we emit
    # the full document as markdown with ## Section: <key> markers. The
    # downstream JSON builder will split on "## Entry" or on the entry number
    # pattern that already exists in the PDF.

    current_section: Optional[str] = None
    pending_line_parts: list[str] = []

    def flush_line() -> None:
        nonlocal pending_line_parts
        if pending_line_parts:
            output_lines.append(" ".join(pending_line_parts))
            pending_line_parts = []

    for page_num in range(total_pages):
        page = doc[page_num]
        spans = _extract_page_blocks(page, page_num)

        i = 0
        while i < len(spans):
            text, italic, bold, font_size = spans[i]

            if text == "\n":
                flush_line()
                output_lines.append("")
                i += 1
                continue

            # Check if this span is a section label.
            # Italic spans are tried first (the primary indicator in mdeng.pdf).
            # Non-italic spans are also checked against the strong regex patterns
            # (e.g. madhyamakala appears in parentheses and may not be italic).
            canonical = None
            if italic:
                canonical = _normalise_label(text)
            if canonical is None:
                # Also check non-italic spans via regex only (stricter patterns)
                for pattern, ckey in _SECTION_PATTERNS:
                    if pattern.search(text.strip()):
                        canonical = ckey
                        break
            if canonical:
                flush_line()
                output_lines.append(f"\n## Section: {canonical}\n")
                current_section = canonical
                i += 1
                continue

            # Regular lyric / header text — accumulate into current line.
            pending_line_parts.append(text)
            i += 1

        flush_line()

    doc.close()

    # Join and apply cleaning
    raw_output = "\n".join(output_lines)
    raw_output = clean_lyric_text(raw_output)

    # Post-extraction validation
    _validate_output(raw_output, validation_errors)

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(raw_output)

    logger.info("Written to %s (%d chars)", output_path, len(raw_output))

    if validation_errors:
        logger.warning("VALIDATION WARNINGS (%d):", len(validation_errors))
        for err in validation_errors:
            logger.warning("  %s", err)
    else:
        logger.info("Validation passed.")


def _validate_output(text: str, errors: list[str]) -> None:
    """Run post-extraction assertions per TRACK-070 §1.5."""
    if "data:image" in text or "base64" in text:
        errors.append("Output contains 'data:image' or 'base64' — image stripping incomplete")

    # Check that at least some pallavi sections are present
    pallavi_count = text.count("## Section: pallavi")
    if pallavi_count == 0:
        errors.append("No '## Section: pallavi' markers found — section detection may have failed")
    else:
        logger.info("Found %d pallavi section markers", pallavi_count)

    # Warn about suspiciously long section blobs (> 2000 chars between markers)
    parts = re.split(r'## Section: \w+', text)
    for idx, part in enumerate(parts[1:], start=1):
        part_stripped = part.strip()
        if len(part_stripped) > 2000:
            errors.append(
                f"Section block #{idx} is {len(part_stripped)} chars — "
                "may indicate page bleed from adjacent content"
            )

    # Total text sanity: must have at least some content
    if len(text.strip()) < 500:
        errors.append(f"Output is very short ({len(text)} chars) — extraction may have failed")


def main() -> None:
    pdf_path = "database/for_import/mdeng.pdf"
    output_path = "database/for_import/mdeng.md"

    if len(sys.argv) >= 2:
        pdf_path = sys.argv[1]
    if len(sys.argv) >= 3:
        output_path = sys.argv[2]

    if not os.path.exists(pdf_path):
        logger.error("PDF not found: %s", pdf_path)
        sys.exit(1)

    extract_to_markdown(pdf_path, output_path)


if __name__ == "__main__":
    main()
