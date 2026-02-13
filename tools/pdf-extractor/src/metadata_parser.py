"""Header field extraction for Carnatic music compositions.

Parses Krithi headers to extract title, raga, tala, deity, and temple
references. Handles the consistent format used in guruguha.org PDFs
and similar scholarly publications, including garbled diacritic forms
from Utopia-family fonts.
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass
from typing import Optional

from .diacritic_normalizer import cleanup_raga_tala_name, normalize_garbled_diacritics

logger = logging.getLogger(__name__)


@dataclass
class KrithiMetadata:
    """Metadata extracted from a Krithi's header block."""

    title: str
    alternate_title: Optional[str] = None
    raga: Optional[str] = None
    tala: Optional[str] = None
    composer: Optional[str] = None
    deity: Optional[str] = None
    temple: Optional[str] = None
    temple_location: Optional[str] = None


# ─── Label patterns that handle all four encoding categories ─────────────
#
# 1. Clean ASCII:      raga, raaga
# 2. Clean IAST:       rāga, rāgaṁ
# 3. Garbled Utopia:   r¯aga, r¯aga ˙m, r¯aga˙m
# 4. Devanagari:       राग

_RAGA_LABEL = (
    r"(?:"
    r"r[\u00AF\u0101]?a+ga\s*[\u02D9.]?\s*[\u1E41m]?"  # ASCII/IAST/garbled
    r"|r\u0101ga[\u1E41m]?"                              # Precomposed IAST
    r"|राग"                                               # Devanagari
    r")"
)

_TALA_LABEL = (
    r"(?:"
    r"t[\u00AF\u0101]?a+l[.\u1E37]?\s*a\s*[\u02D9.]?\s*[\u1E41m]?"  # ASCII/IAST/garbled
    r"|t\u0101l[\u1E37]?a[\u1E41m]?"                                  # Precomposed IAST
    r"|ताल"                                                            # Devanagari
    r")"
)


class MetadataParser:
    """Extract metadata fields from Krithi header text.

    Handles multiple header formats:
    - guruguha.org style: "Title\\nRaga: X — Tala: Y\\nDeity: Z at Temple"
    - Garbled Utopia: "r¯aga ˙m: juj¯avanti (28)\\nt¯al.a ˙m: ¯adi"
    - Generic style: "Title\\nRaga - Tala\\nComposer"
    - Devanagari headers with corresponding field labels
    """

    # ─── Regex patterns for field extraction ─────────────────────────────

    RAGA_PATTERN = re.compile(
        _RAGA_LABEL + r"\s*[:—–\-]\s*" +
        r"(.+?)"
        r"(?:\s*\(\d+\)\s*)?"           # Optional mēḷa number: (28)
        r"(?:"
        r"\s*[—–\-|]\s*" + _TALA_LABEL +  # Followed by tala label
        r"|$"                             # Or end of line
        r")",
        re.IGNORECASE | re.MULTILINE,
    )

    TALA_PATTERN = re.compile(
        _TALA_LABEL + r"\s*[:—–\-]\s*" +
        r"(.+?)"
        r"(?:\s*\(\d+\))?"              # Optional number
        r"(?:\s*$|\s*[—–\-|])",          # End of line or separator
        re.IGNORECASE | re.MULTILINE,
    )

    # Combined raga-tala on a single line
    RAGA_TALA_COMBINED = re.compile(
        _RAGA_LABEL + r"\s*[:—–\-]\s*(.+?)" +
        r"(?:\s*\(\d+\)\s*)?"
        r"\s*[—–\-|]\s*"
        + _TALA_LABEL + r"\s*[:—–\-]\s*(.+?)"
        r"(?:\s*\(\d+\))?"
        r"$",
        re.IGNORECASE | re.MULTILINE,
    )

    # Inline variant commonly seen in blog text:
    # "rAgaM kumudakriyA - tALaM - rUpakaM"
    RAGA_TALA_INLINE = re.compile(
        _RAGA_LABEL + r"\s*(?:[:—–\-]\s*)?(.+?)" +
        r"(?:\s*\(\d+\)\s*)?"
        r"\s*[—–\-|]\s*"
        + _TALA_LABEL + r"\s*(?:[:—–\-]\s*)?(.+?)"
        r"(?:\s*\(\d+\))?"
        r"(?:\s*$|\s*[—–\-|])",
        re.IGNORECASE | re.MULTILINE,
    )

    # Simple "Raga - Tala" format (no labels)
    RAGA_TALA_SIMPLE = re.compile(
        r"^([A-Z][a-zāīūṛṣṇḍṭḥ]+(?:\s[A-Z][a-zāīūṛṣṇḍṭḥ]+)*)\s*[—–\-]\s*"
        r"([A-Z][a-zāīūṛṣṇḍṭḥ]+(?:\s[A-Z][a-zāīūṛṣṇḍṭḥ]+)*)$",
        re.MULTILINE,
    )

    DEITY_PATTERN = re.compile(
        r"(?:deity|devatā|देवता)\s*[:—–\-]\s*(.+?)(?:\s*(?:at|temple|kshetra|क्षेत्र)|$)",
        re.IGNORECASE,
    )

    TEMPLE_PATTERN = re.compile(
        r"(?:temple|kshetra|sthala|क्षेत्र|स्थल)\s*[:—–\-]\s*(.+?)$",
        re.IGNORECASE | re.MULTILINE,
    )

    COMPOSER_PATTERN = re.compile(
        r"(?:composer|vāggeyakāra|वाग्गेयकार)\s*[:—–\-]\s*(.+?)$",
        re.IGNORECASE | re.MULTILINE,
    )
    METADATA_LABEL_PATTERN = re.compile(
        _RAGA_LABEL + r"|" + _TALA_LABEL,
        re.IGNORECASE,
    )
    INLINE_TITLE_WITH_RAGA_PATTERN = re.compile(
        r"^\s*(.+?)\s*[—–\-]\s*" + _RAGA_LABEL,
        re.IGNORECASE,
    )

    # Blog/page-title boilerplate frequently present in HTML sources.
    _TITLE_PREFIX_PATTERNS = [
        re.compile(r"^\s*guru\s+guha\s+vaibhavam\s*:\s*", re.IGNORECASE),
        re.compile(r"^\s*dikshitar\s+kriti\s*[-:]\s*", re.IGNORECASE),
        re.compile(r"^\s*kriti\s*[-:]\s*", re.IGNORECASE),
    ]
    _TITLE_SUFFIX_PATTERNS = [
        re.compile(r"\s*-\s*raga\s+[^|]+$", re.IGNORECASE),
        re.compile(r"\s*\|\s*[^|]+$", re.IGNORECASE),
    ]
    _TITLE_LEADING_INDEX = re.compile(r"^\s*[0-9०-९]+\s*[-:.)]?\s*")

    def parse(self, header_text: str, title_hint: Optional[str] = None) -> KrithiMetadata:
        """Parse a Krithi header block to extract metadata.

        Args:
            header_text: The full header text (title + metadata lines).
            title_hint: Optional pre-detected title from font-size analysis.

        Returns:
            KrithiMetadata with extracted fields.
        """
        lines = [line.strip() for line in header_text.strip().split("\n") if line.strip()]

        if not lines:
            return KrithiMetadata(title=title_hint or "Unknown")

        # Title is typically the first line (or provided via title_hint).
        # For HTML pages, title_hint often includes blog boilerplate while the first
        # extracted line may contain "title - rAgaM ... - tALaM ..."; prefer that.
        raw_title = title_hint or lines[0]
        if title_hint:
            inline_title = self._extract_inline_title(lines[0])
            if inline_title:
                raw_title = inline_title

        # Normalise garbled diacritics in title (TRACK-059: was previously skipped)
        title = normalize_garbled_diacritics(raw_title)
        title = self._normalize_title_boilerplate(title)

        # Join lines for field extraction. When title_hint is supplied and the first
        # line is metadata-bearing, keep it; otherwise skip first line as title.
        if title_hint and self.METADATA_LABEL_PATTERN.search(lines[0]):
            metadata_lines = lines
        else:
            metadata_lines = lines[1:]
        metadata_text = "\n".join(metadata_lines) if metadata_lines else ""

        # Normalise garbled diacritics in metadata before regex matching
        normalised_text = normalize_garbled_diacritics(metadata_text)

        raga, tala = self._extract_raga_tala(normalised_text)

        # If normalised text didn't match, try the raw text as fallback
        if not raga and not tala:
            raga, tala = self._extract_raga_tala(metadata_text)

        # Clean up extracted names
        if raga:
            raga = cleanup_raga_tala_name(raga)
        if tala:
            tala = cleanup_raga_tala_name(tala)

        deity = self._extract_field(self.DEITY_PATTERN, metadata_text)
        temple = self._extract_field(self.TEMPLE_PATTERN, metadata_text)
        composer = self._extract_field(self.COMPOSER_PATTERN, metadata_text)

        return KrithiMetadata(
            title=title.strip(),
            raga=raga,
            tala=tala,
            composer=composer,
            deity=deity,
            temple=temple,
        )

    def _normalize_title_boilerplate(self, title: str) -> str:
        """Remove known blog/page-title prefixes that hurt title matching."""
        cleaned = title.strip()
        for pattern in self._TITLE_PREFIX_PATTERNS:
            cleaned = pattern.sub("", cleaned)
        cleaned = self._TITLE_LEADING_INDEX.sub("", cleaned)
        for pattern in self._TITLE_SUFFIX_PATTERNS:
            cleaned = pattern.sub("", cleaned)
        cleaned = re.sub(r"\s+", " ", cleaned).strip(" -:\t")
        return cleaned or title.strip()

    def _extract_inline_title(self, first_line: str) -> Optional[str]:
        match = self.INLINE_TITLE_WITH_RAGA_PATTERN.search(first_line)
        if not match:
            return None
        value = match.group(1).strip(" -:\t")
        return value or None

    def _extract_raga_tala(self, text: str) -> tuple[Optional[str], Optional[str]]:
        """Extract raga and tala from metadata text."""
        # Try combined format first
        match = self.RAGA_TALA_COMBINED.search(text)
        if match:
            return match.group(1).strip(), match.group(2).strip()

        # Try inline combined format (no explicit ':' after raga/tala labels)
        match = self.RAGA_TALA_INLINE.search(text)
        if match:
            return match.group(1).strip(), match.group(2).strip()

        # Try individual patterns
        raga = self._extract_field(self.RAGA_PATTERN, text)
        tala = self._extract_field(self.TALA_PATTERN, text)

        # Fallback: simple "Raga - Tala" format
        if not raga and not tala:
            match = self.RAGA_TALA_SIMPLE.search(text)
            if match:
                return match.group(1).strip(), match.group(2).strip()

        return raga, tala

    def _extract_field(
        self, pattern: re.Pattern[str], text: str
    ) -> Optional[str]:
        """Extract a single field using a regex pattern."""
        match = pattern.search(text)
        if match:
            value = match.group(1).strip()
            return value if value else None
        return None
