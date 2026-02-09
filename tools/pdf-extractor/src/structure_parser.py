"""Section label detection for Carnatic music compositions.

Detects structural sections (Pallavi, Anupallavi, Charanam, Samashti Charanam,
Chittaswaram, etc.) in extracted text. Handles both Sanskrit/Devanagari and
English/Latin script labels.
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass, field

from .schema import CanonicalLyricSection, CanonicalSection, SectionType

logger = logging.getLogger(__name__)


@dataclass
class DetectedSection:
    """A section detected in extracted text."""

    section_type: SectionType
    order: int
    label: str
    text: str
    start_pos: int  # character position in source text
    end_pos: int


# ─── Section label patterns ─────────────────────────────────────────────────

# Each pattern group handles multiple transliterations and scripts
SECTION_PATTERNS: list[tuple[SectionType, re.Pattern[str]]] = [
    # Samashti Charanam (must be checked before Charanam)
    (
        SectionType.SAMASHTI_CHARANAM,
        re.compile(
            r"(?:samashti\s*charaa?n[au]m|samashṭi\s*caraṇam|समष्टि\s*चरणम्)",
            re.IGNORECASE,
        ),
    ),
    # Chittaswaram
    (
        SectionType.CHITTASWARAM,
        re.compile(
            r"(?:chitta?\s*swara[mn]?|cittasvaram|चित्तस्वरम्)",
            re.IGNORECASE,
        ),
    ),
    # Swara Sahitya
    (
        SectionType.SWARA_SAHITYA,
        re.compile(
            r"(?:swara\s*sahitya|स्वरसाहित्य)",
            re.IGNORECASE,
        ),
    ),
    # Madhyama Kala
    (
        SectionType.MADHYAMA_KALA,
        re.compile(
            r"(?:madhyama\s*kaa?la|मध्यमकाल)",
            re.IGNORECASE,
        ),
    ),
    # Pallavi
    (
        SectionType.PALLAVI,
        re.compile(
            r"(?:pallavi|पल्लवि)",
            re.IGNORECASE,
        ),
    ),
    # Anupallavi
    (
        SectionType.ANUPALLAVI,
        re.compile(
            r"(?:anupallavi|अनुपल्लवि)",
            re.IGNORECASE,
        ),
    ),
    # Charanam (with optional number: Charanam 1, Charanam 2, etc.)
    (
        SectionType.CHARANAM,
        re.compile(
            r"(?:charaa?n[au]m|caraṇam|चरणम्)(?:\s*(\d+))?",
            re.IGNORECASE,
        ),
    ),
]

# Pattern to detect any section label line
ANY_SECTION_LABEL = re.compile(
    r"^\s*(?:pallavi|anupallavi|charaa?n[au]m|samashti|chitta?\s*swara|swara\s*sahitya|madhyama|"
    r"पल्लवि|अनुपल्लवि|चरणम्|समष्टि|चित्तस्वर|स्वरसाहित्य|मध्यमकाल)",
    re.IGNORECASE | re.MULTILINE,
)


class StructureParser:
    """Parse section structure from extracted Krithi text.

    Detects Pallavi, Anupallavi, Charanam, and special sections in the text,
    returning a list of CanonicalSection and associated CanonicalLyricSection entries.
    """

    def parse_sections(self, text: str) -> list[DetectedSection]:
        """Parse text to detect section labels and their associated content.

        Args:
            text: Full extracted text of a single Krithi (post-segmentation).

        Returns:
            List of DetectedSection with type, order, label, and text content.
        """
        if not text.strip():
            return []

        # Find all section label positions
        label_positions: list[tuple[int, SectionType, str, re.Match[str]]] = []

        for section_type, pattern in SECTION_PATTERNS:
            for match in pattern.finditer(text):
                label_positions.append((match.start(), section_type, match.group(0), match))

        # Sort by position in text
        label_positions.sort(key=lambda x: x[0])

        # Remove overlapping matches (keep the first/longest)
        filtered: list[tuple[int, SectionType, str, re.Match[str]]] = []
        last_end = -1
        for pos, stype, label, match in label_positions:
            if pos >= last_end:
                filtered.append((pos, stype, label, match))
                last_end = match.end()

        if not filtered:
            # No section labels found — treat entire text as a single section
            logger.debug("No section labels detected in text")
            return [
                DetectedSection(
                    section_type=SectionType.OTHER,
                    order=1,
                    label="Unknown",
                    text=text.strip(),
                    start_pos=0,
                    end_pos=len(text),
                )
            ]

        # Build sections with content between labels
        sections: list[DetectedSection] = []
        charanam_counter = 0

        for i, (pos, stype, label, match) in enumerate(filtered):
            # Content starts after the label line
            content_start = match.end()
            # Content ends at the start of the next label, or end of text
            content_end = filtered[i + 1][0] if i + 1 < len(filtered) else len(text)

            content = text[content_start:content_end].strip()

            # Track Charanam numbering
            order = i + 1
            if stype == SectionType.CHARANAM:
                charanam_counter += 1
                section_label = f"Charanam {charanam_counter}" if charanam_counter > 1 else "Charanam"
            else:
                section_label = stype.value.replace("_", " ").title()

            sections.append(
                DetectedSection(
                    section_type=stype,
                    order=order,
                    label=section_label,
                    text=content,
                    start_pos=pos,
                    end_pos=content_end,
                )
            )

        return sections

    def to_canonical_sections(
        self, detected: list[DetectedSection]
    ) -> list[CanonicalSection]:
        """Convert detected sections to canonical section DTOs."""
        return [
            CanonicalSection(
                type=d.section_type,
                order=d.order,
                label=d.label,
            )
            for d in detected
        ]

    def to_canonical_lyric_sections(
        self,
        detected: list[DetectedSection],
    ) -> list[CanonicalLyricSection]:
        """Convert detected sections to canonical lyric section DTOs."""
        return [
            CanonicalLyricSection(
                sectionOrder=d.order,
                text=d.text,
            )
            for d in detected
            if d.text  # Skip empty sections
        ]
