"""Section and lyric-variant parsing for Carnatic compositions.

Ports deterministic header/regex heuristics from the Kotlin KrithiStructureParser
into the Python extraction service. Produces a frozen parse contract with:
- canonical sections
- lyric variants (language/script scoped)
- metadata boundaries (meaning/notes/etc.)
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass, field
from typing import Optional

from .schema import (
    CanonicalLyricSection,
    CanonicalLyricVariant,
    CanonicalMetadataBoundary,
    CanonicalSection,
    SectionType,
)

logger = logging.getLogger(__name__)


@dataclass
class DetectedSection:
    """A lyric section detected in extracted text."""

    section_type: SectionType
    order: int
    label: str
    text: str
    start_pos: int
    end_pos: int


@dataclass
class DetectedLyricVariant:
    """A full lyric variant in one language/script."""

    language: str
    script: str
    sections: list[DetectedSection] = field(default_factory=list)


@dataclass
class MetadataBoundary:
    """Start/end offsets for non-lyric metadata blocks in source text."""

    label: str
    start_pos: int
    end_pos: int


@dataclass
class StructureParseResult:
    """Frozen parser contract emitted by the extraction layer."""

    sections: list[DetectedSection] = field(default_factory=list)
    lyric_variants: list[DetectedLyricVariant] = field(default_factory=list)
    metadata_boundaries: list[MetadataBoundary] = field(default_factory=list)


@dataclass
class _LineToken:
    text: str
    start_pos: int
    end_pos: int


@dataclass
class _TextBlock:
    label: str
    lines: list[_LineToken]
    start_pos: int
    end_pos: int


@dataclass
class _HeaderMatch:
    label: str
    remainder: str


LANGUAGE_LABELS = {
    "DEVANAGARI",
    "TAMIL",
    "TELUGU",
    "KANNADA",
    "MALAYALAM",
    "ENGLISH",
    "LATIN",
    "SANSKRIT",
    "HINDI",
    "WORD_DIVISION",
    "MEANING",
    "GIST",
    "NOTES",
    "VARIATIONS",
}

METADATA_LABELS = {"WORD_DIVISION", "MEANING", "GIST", "NOTES", "VARIATIONS"}

LANGUAGE_HEADER_CANDIDATES: list[tuple[str, str]] = [
    ("devanagari", "DEVANAGARI"),
    ("tamil", "TAMIL"),
    ("telugu", "TELUGU"),
    ("kannada", "KANNADA"),
    ("malayalam", "MALAYALAM"),
    ("english", "ENGLISH"),
    ("roman", "LATIN"),
    ("latin", "LATIN"),
    ("sanskrit", "SANSKRIT"),
    ("hindi", "HINDI"),
    ("word division", "WORD_DIVISION"),
    ("meaning", "MEANING"),
    ("gist", "GIST"),
    ("notes", "NOTES"),
    ("variations", "VARIATIONS"),
]

SECTION_LABEL_TO_TYPE: dict[str, SectionType] = {
    "PALLAVI": SectionType.PALLAVI,
    "ANUPALLAVI": SectionType.ANUPALLAVI,
    "CHARANAM": SectionType.CHARANAM,
    "SAMASHTI_CHARANAM": SectionType.SAMASHTI_CHARANAM,
    "CHITTASWARAM": SectionType.CHITTASWARAM,
    "SWARA_SAHITYA": SectionType.SWARA_SAHITYA,
    "MADHYAMAKALA": SectionType.MADHYAMA_KALA,
    "MADHYAMA_KALA": SectionType.MADHYAMA_KALA,
}

# Kotlin parity: matches "1. SrI rAgaM" or "Arabhi rAgaM"
RAGA_SUBSECTION_PATTERN = re.compile(r"^(?:\d+\.\s*)?(.+?)\s+rAgaM\s*$", re.IGNORECASE)
# Kotlin parity: matches "vilOma - mOhana rAgaM"
VILOMA_SUBSECTION_PATTERN = re.compile(r"^vilOma\s*-\s*(.+?)\s+rAgaM\s*$", re.IGNORECASE)

SECTION_HEADER_PATTERNS: list[tuple[re.Pattern[str], str]] = [
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*pallavi(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*anupallavi(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "ANUPALLAVI"),
    (
        re.compile(
            r"^\s*[\-–—•*()=\[\]]*\s*(?:(?:ch|c)ara?n(?:\.\s*am|am)|caraṇam)(?:\b|:|\.|\-|\)|]|=|$)",
            re.IGNORECASE,
        ),
        "CHARANAM",
    ),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*samashti\s+(?:ch|c)ara?nam(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*samash?ti\s+(?:ch|c)ara?nam(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*chittaswaram(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "CHITTASWARAM"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*madhyama\s+kAla(?:\s+sAhityam)?(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "MADHYAMAKALA"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*madhyama\s+kala(?:\s+sahityam)?(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "MADHYAMAKALA"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*madhyamakala(?:\s+sahityam)?(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "MADHYAMAKALA"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*m\.\s*k(?:\s+sahityam)?(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "MADHYAMAKALA"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*svara\s+sahitya(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "SWARA_SAHITYA"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*swarasahitya(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "SWARA_SAHITYA"),
    # Latin single-letter abbreviations.
    (re.compile(r"^\s*P(?:\s|\.|:|-|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*A(?:\s|\.|:|-|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*C(?:\s|\.|:|-|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*Ch(?:\s|\.|:|-|$)", re.IGNORECASE), "CHARANAM"),
    # Indic abbreviations.
    (re.compile(r"^\s*प(?:\.|\s|:|-|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*अ(?:\.|\s|:|-|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*च(?:\.|\s|:|-|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*ப(?:\.|\s|:|-|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*அ(?:\.|\s|:|-|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*ச(?:\.|\s|:|-|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*ప(?:\.|\s|:|-|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*అ(?:\.|\s|:|-|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*చ(?:\.|\s|:|-|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*ಪ(?:\.|\s|:|-|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*ಅ(?:\.|\s|:|-|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*ಚ(?:\.|\s|:|-|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*പ(?:\.|\s|:|-|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*അ(?:\.|\s|:|-|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*ച(?:\.|\s|:|-|$)", re.IGNORECASE), "CHARANAM"),
    # Devanagari full headers.
    (re.compile(r"^\s*पल्लवि(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*अनुपल्लवि(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*चरण[म्ंम]+(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*समष्टि\s+चरण[म्ंम]+(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    (re.compile(r"^\s*[(]?मध्यम\s+काल\s+साहित्य[म्ंम]*[)]?(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "MADHYAMAKALA"),
    # Tamil full headers.
    (re.compile(r"^\s*பல்லவி(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*அனுபல்லவி(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*சரணம்(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*ஸமஷ்டி\s+சரணம்(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    # Telugu full headers (both anusvara చరణం and explicit చరణమ్ forms).
    (re.compile(r"^\s*పల్లవి(?:\s|:|\-|\)|]|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*అనుపల్లవి(?:\s|:|\-|\)|]|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*చరణం(?:\s|:|\-|\)|]|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*చరణమ్(?:\s|:|\-|\)|]|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*సమష్టి\s+చరణ[ంమ్]+(?:\s|:|\-|\)|]|$)", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    # Kannada full headers (both short ಚರಣ and full ಚರಣಮ್/ಚರಣಂ forms).
    (re.compile(r"^\s*ಪಲ್ಲವಿ(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*ಅನುಪಲ್ಲವಿ(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*ಚರಣ[ಮ್ಂ]*(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*ಸಮಷ್ಟಿ\s+ಚರಣ[ಮ್ಂ]*(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    # Malayalam full headers (both ചരണം and ചരണമ് forms).
    (re.compile(r"^\s*പല്ലവി(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*അനുപല്ലവി(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*ചരണ[ംമ്]+(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*സമഷ്ടി\s+ചരണ[ംമ്]+(?:\s|:|\-|\.|\)|]|$)", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    # Telugu parenthesized MKS: (మధ్యమ కాల సాహిత్యం)
    (re.compile(r"^\s*[(]మధ్యమ\s+కాల\s+సాహిత్య[ంమ్]+[)]\s*$", re.IGNORECASE), "MADHYAMAKALA"),
    # Kannada parenthesized MKS: (ಮಧ್ಯಮ ಕಾಲ ಸಾಹಿತ್ಯಂ)
    (re.compile(r"^\s*[(]ಮಧ್ಯಮ\s+ಕಾಲ\s+ಸಾಹಿತ್ಯ[ಂಮ್]*[)]\s*$", re.IGNORECASE), "MADHYAMAKALA"),
    # Malayalam parenthesized MKS: (മധ്യമ കാല സാഹിത്യം)
    (re.compile(r"^\s*[(]മധ്യമ\s+കാല\s+സാഹിത്യ[ംമ്]+[)]\s*$", re.IGNORECASE), "MADHYAMAKALA"),
    # Tamil parenthesized MKS: (மத்யம கால ஸாஹித்யம்)
    (re.compile(r"^\s*[(]மத்யம\s+கால\s+ஸாஹித்ய[ம்]+[)]\s*$", re.IGNORECASE), "MADHYAMAKALA"),
    # Bracket-format headers with underscores (from stored lyrics).
    (re.compile(r"^\s*\[PALLAVI\]\s*$", re.IGNORECASE), "PALLAVI"),
    (re.compile(r"^\s*\[ANUPALLAVI\]\s*$", re.IGNORECASE), "ANUPALLAVI"),
    (re.compile(r"^\s*\[CHARANAM\]\s*$", re.IGNORECASE), "CHARANAM"),
    (re.compile(r"^\s*\[SAMASHTI_CHARANAM\]\s*$", re.IGNORECASE), "SAMASHTI_CHARANAM"),
    (re.compile(r"^\s*\[CHITTASWARAM\]\s*$", re.IGNORECASE), "CHITTASWARAM"),
    (re.compile(r"^\s*\[MADHYAMA_KALA\]\s*$", re.IGNORECASE), "MADHYAMAKALA"),
    (re.compile(r"^\s*\[SWARA_SAHITYA\]\s*$", re.IGNORECASE), "SWARA_SAHITYA"),
    (re.compile(r"^\s*\[SOLKATTU_SWARA\]\s*$", re.IGNORECASE), "SOLKATTU_SWARA"),
    # Non-lyric metadata headers.
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*meaning(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "MEANING"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*notes?(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "NOTES"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*gist(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "GIST"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*word\s+division(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "WORD_DIVISION"),
    (re.compile(r"^\s*[\-–—•*()=\[\]]*\s*variations?(?:\b|:|\.|\-|\)|]|=|$)", re.IGNORECASE), "VARIATIONS"),
]

METADATA_BOUNDARY_PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("MEANING", re.compile(r"^\s*(?:meaning|artha|artham|भावार्थ)\b", re.IGNORECASE | re.MULTILINE)),
    ("GIST", re.compile(r"^\s*(?:gist|summary)\b", re.IGNORECASE | re.MULTILINE)),
    ("NOTES", re.compile(r"^\s*(?:notes?|tippani)\b", re.IGNORECASE | re.MULTILINE)),
    ("WORD_DIVISION", re.compile(r"^\s*(?:word\s*division|pada\s*ccheda)\b", re.IGNORECASE | re.MULTILINE)),
    ("VARIATIONS", re.compile(r"^\s*(?:variations?|alternate\s*reading)\b", re.IGNORECASE | re.MULTILINE)),
]

METADATA_KEYWORDS = (
    "title",
    "raga",
    "ragam",
    "raagam",
    "tala",
    "talam",
    "taala",
    "composer",
    "composed by",
    "deity",
    "temple",
    "kshetra",
    "kshetram",
    "kshethra",
    "kriti",
    "krithi",
    "language",
)


class StructureParser:
    """Deterministic structure parser with Kotlin-parity heuristics."""

    def parse(self, text: str) -> StructureParseResult:
        if not text.strip():
            return StructureParseResult()

        source_text = text.replace("\\n", "\n")
        metadata_boundaries = self._find_metadata_boundaries(source_text)
        lyric_window_end = metadata_boundaries[0].start_pos if metadata_boundaries else len(source_text)
        lyric_text = source_text[:lyric_window_end]

        blocks = self._build_blocks(lyric_text)
        sections = self._extract_sections(blocks, lyric_text)
        lyric_variants = self._extract_lyric_variants(blocks, lyric_text, sections)

        return StructureParseResult(
            sections=sections,
            lyric_variants=lyric_variants,
            metadata_boundaries=metadata_boundaries,
        )

    def parse_sections(self, text: str) -> list[DetectedSection]:
        return self.parse(text).sections

    def _build_blocks(self, raw_text: str) -> list[_TextBlock]:
        if not raw_text.strip():
            return []

        tokens: list[_LineToken] = []
        offset = 0
        for raw_line in raw_text.splitlines(keepends=True):
            line_without_newline = raw_line.rstrip("\n")
            stripped = line_without_newline.strip()
            if not stripped:
                offset += len(raw_line)
                continue

            left_padding = len(line_without_newline) - len(line_without_newline.lstrip())
            start_pos = offset + left_padding
            end_pos = offset + len(line_without_newline)
            offset += len(raw_line)

            normalized = self._normalize_line(stripped)
            if not normalized or self._is_boilerplate(normalized):
                continue
            tokens.append(_LineToken(text=normalized, start_pos=start_pos, end_pos=end_pos))

        if not tokens:
            return []

        blocks: list[_TextBlock] = []
        current_label = "UNLABELED"
        current_start = tokens[0].start_pos
        current_lines: list[_LineToken] = []

        def flush() -> None:
            nonlocal current_lines
            if current_lines or current_label in LANGUAGE_LABELS:
                block_start = current_start if not current_lines else current_lines[0].start_pos
                block_end = current_lines[-1].end_pos if current_lines else current_start
                blocks.append(
                    _TextBlock(
                        label=current_label,
                        lines=current_lines,
                        start_pos=block_start,
                        end_pos=block_end,
                    )
                )
                current_lines = []

        for token in tokens:
            header = self._detect_header(token.text)
            if header is not None:
                flush()
                current_label = header.label
                current_start = token.start_pos
                if header.remainder:
                    current_lines.append(
                        _LineToken(
                            text=header.remainder,
                            start_pos=token.start_pos,
                            end_pos=token.end_pos,
                        )
                    )
                continue

            current_lines.append(token)

        flush()
        return blocks

    def _detect_header(self, line: str) -> Optional[_HeaderMatch]:
        language = self._detect_language_header(line)
        if language is not None:
            return language
        return self._detect_section_header(line)

    def _detect_language_header(self, line: str) -> Optional[_HeaderMatch]:
        lowered = line.lower()
        for key, label in LANGUAGE_HEADER_CANDIDATES:
            if lowered == key or lowered.startswith(f"{key}:") or lowered.startswith(f"{key} -") or lowered.startswith(f"{key} –"):
                remainder = line[len(key) :].lstrip(":-– ")
                return _HeaderMatch(label=label, remainder=remainder)
        return None

    def _detect_section_header(self, line: str) -> Optional[_HeaderMatch]:
        for pattern, label in SECTION_HEADER_PATTERNS:
            if pattern.search(line):
                remainder = pattern.sub("", line, count=1).strip()
                remainder = re.sub(r"^[:\-)\]\.\s]+", "", remainder).strip()
                return _HeaderMatch(label=label, remainder=remainder)
        return None

    def _extract_sections(self, blocks: list[_TextBlock], lyric_text: str) -> list[DetectedSection]:
        if not blocks:
            trimmed = lyric_text.strip()
            if not trimmed:
                return []
            return [
                DetectedSection(
                    section_type=SectionType.OTHER,
                    order=1,
                    label="Unknown",
                    text=trimmed,
                    start_pos=0,
                    end_pos=len(lyric_text),
                )
            ]

        sections: list[DetectedSection] = []
        found_first_section = False
        charanam_counter = 0

        for block in blocks:
            if block.label in LANGUAGE_LABELS:
                if found_first_section:
                    break
                continue

            section_type = SECTION_LABEL_TO_TYPE.get(block.label)
            if section_type is None:
                continue

            found_first_section = True
            split_sections = self._split_ragamalika_subsections(section_type, block)
            if not split_sections:
                block_text = "\n".join(line.text for line in block.lines).strip()
                if block_text:
                    split_sections = [(None, block_text, block.start_pos, block.end_pos)]

            for subsection_label, subsection_text, start_pos, end_pos in split_sections:
                order = len(sections) + 1
                if section_type == SectionType.CHARANAM:
                    charanam_counter += 1
                    canonical_label = "Charanam" if charanam_counter == 1 else f"Charanam {charanam_counter}"
                else:
                    canonical_label = section_type.value.replace("_", " ").title()

                sections.append(
                    DetectedSection(
                        section_type=section_type,
                        order=order,
                        label=subsection_label or canonical_label,
                        text=subsection_text,
                        start_pos=start_pos,
                        end_pos=end_pos,
                    )
                )

        if sections:
            sections = self._demote_mks(sections)
            sections = self._merge_dual_format(sections)
            return sections

        trimmed = lyric_text.strip()
        if not trimmed:
            return []
        return [
            DetectedSection(
                section_type=SectionType.OTHER,
                order=1,
                label="Unknown",
                text=trimmed,
                start_pos=0,
                end_pos=len(lyric_text),
            )
        ]

    def _demote_mks(self, sections: list[DetectedSection]) -> list[DetectedSection]:
        """Rule 1: MKS is never a top-level section — attach to preceding parent.

        If the MKS block contains an inline section header (e.g. చరణమ్ for Charanam),
        split it: text before the header is MKS of the parent, text after is a new section.
        """
        result: list[DetectedSection] = []
        for section in sections:
            if section.section_type == SectionType.MADHYAMA_KALA:
                # Check for inline section headers within MKS text
                split_sections = self._split_mks_inline_headers(section)
                if split_sections:
                    mks_text, new_sections = split_sections
                    # Attach MKS text to parent
                    if result and mks_text.strip():
                        parent = result[-1]
                        result[-1] = DetectedSection(
                            section_type=parent.section_type,
                            order=parent.order,
                            label=parent.label,
                            text=parent.text + "\n\n[Madhyama Kala Sahitya]\n" + mks_text,
                            start_pos=parent.start_pos,
                            end_pos=section.end_pos,
                        )
                    elif mks_text.strip() and not result:
                        # No parent — keep as OTHER
                        result.append(DetectedSection(
                            section_type=SectionType.OTHER,
                            order=0, label="Madhyama Kala",
                            text=mks_text, start_pos=section.start_pos,
                            end_pos=section.end_pos,
                        ))
                    # Add the extracted sections
                    result.extend(new_sections)
                elif result:
                    parent = result[-1]
                    result[-1] = DetectedSection(
                        section_type=parent.section_type,
                        order=parent.order,
                        label=parent.label,
                        text=parent.text + "\n\n[Madhyama Kala Sahitya]\n" + section.text,
                        start_pos=parent.start_pos,
                        end_pos=section.end_pos,
                    )
                # else: standalone MKS with no parent and no inline headers — drop it
            else:
                result.append(section)
        # Re-index order sequentially
        for i, s in enumerate(result):
            result[i] = DetectedSection(
                section_type=s.section_type,
                order=i + 1,
                label=s.label,
                text=s.text,
                start_pos=s.start_pos,
                end_pos=s.end_pos,
            )
        return result

    def _split_mks_inline_headers(
        self, mks_section: DetectedSection
    ) -> tuple[str, list[DetectedSection]] | None:
        """Check if MKS text contains inline section headers (e.g. చరణమ్).

        Returns (mks_text_before_header, [new_sections]) or None if no inline header found.
        """
        lines = mks_section.text.splitlines()
        for i, line in enumerate(lines):
            header = self._detect_section_header(line.strip())
            if header is not None and header.label not in METADATA_LABELS:
                section_type = SECTION_LABEL_TO_TYPE.get(header.label)
                if section_type and section_type != SectionType.MADHYAMA_KALA:
                    mks_text = "\n".join(lines[:i]).strip()
                    section_text_lines = []
                    if header.remainder:
                        section_text_lines.append(header.remainder)
                    section_text_lines.extend(lines[i + 1:])
                    section_text = "\n".join(section_text_lines).strip()

                    new_sections = []
                    if section_text:
                        new_sections.append(DetectedSection(
                            section_type=section_type,
                            order=0,
                            label=section_type.value.replace("_", " ").title(),
                            text=section_text,
                            start_pos=mks_section.start_pos,
                            end_pos=mks_section.end_pos,
                        ))
                    return (mks_text, new_sections)
        return None

    def _merge_dual_format(self, sections: list[DetectedSection]) -> list[DetectedSection]:
        """Rule 3: Merge dual-format (continuous + word-division) duplicates."""
        if len(sections) < 2:
            return sections
        result: list[DetectedSection] = []
        skip_next = False
        for i, section in enumerate(sections):
            if skip_next:
                skip_next = False
                continue
            if i + 1 < len(sections):
                next_section = sections[i + 1]
                if section.section_type == next_section.section_type:
                    norm_a = re.sub(r"\s+", "", section.text)
                    norm_b = re.sub(r"\s+", "", next_section.text)
                    if norm_a and norm_b:
                        overlap = sum(1 for ca, cb in zip(norm_a, norm_b) if ca == cb)
                        max_len = max(len(norm_a), len(norm_b))
                        if max_len > 0 and overlap / max_len > 0.9:
                            # Keep the longer one (word-division has more spaces)
                            kept = next_section if len(next_section.text) >= len(section.text) else section
                            result.append(DetectedSection(
                                section_type=kept.section_type,
                                order=len(result) + 1,
                                label=kept.label,
                                text=kept.text,
                                start_pos=kept.start_pos,
                                end_pos=kept.end_pos,
                            ))
                            skip_next = True
                            continue
            result.append(DetectedSection(
                section_type=section.section_type,
                order=len(result) + 1,
                label=section.label,
                text=section.text,
                start_pos=section.start_pos,
                end_pos=section.end_pos,
            ))
        return result

    def _split_ragamalika_subsections(
        self,
        section_type: SectionType,
        block: _TextBlock,
    ) -> list[tuple[Optional[str], str, int, int]]:
        if not block.lines:
            return []

        parts: list[tuple[Optional[str], str, int, int]] = []
        current_label: Optional[str] = None
        current_lines: list[_LineToken] = []
        saw_subsection_marker = False

        def flush() -> None:
            nonlocal current_lines
            if not current_lines:
                return
            text = "\n".join(line.text for line in current_lines).strip()
            if text:
                parts.append((current_label, text, current_lines[0].start_pos, current_lines[-1].end_pos))
            current_lines = []

        for line in block.lines:
            viloma_match = VILOMA_SUBSECTION_PATTERN.search(line.text)
            raga_match = RAGA_SUBSECTION_PATTERN.search(line.text)
            if viloma_match is not None:
                saw_subsection_marker = True
                flush()
                current_label = f"Viloma - {viloma_match.group(1).strip()}"
                continue
            if raga_match is not None:
                saw_subsection_marker = True
                flush()
                current_label = raga_match.group(1).strip()
                continue
            current_lines.append(line)

        flush()
        if not saw_subsection_marker:
            return []
        return parts

    def _extract_lyric_variants(
        self,
        blocks: list[_TextBlock],
        lyric_text: str,
        canonical_sections: list[DetectedSection],
    ) -> list[DetectedLyricVariant]:
        language_blocks_seen = any(block.label in LANGUAGE_LABELS - METADATA_LABELS for block in blocks)
        if language_blocks_seen:
            return self._extract_language_header_variants(blocks, canonical_sections)
        return self._extract_script_split_variants(lyric_text, canonical_sections)

    def _extract_language_header_variants(
        self,
        blocks: list[_TextBlock],
        canonical_sections: list[DetectedSection],
    ) -> list[DetectedLyricVariant]:
        variants: list[DetectedLyricVariant] = []
        current_label: Optional[str] = None
        current_blocks: list[_TextBlock] = []

        def flush() -> None:
            nonlocal current_label, current_blocks
            if current_label is None:
                return
            language, script = self._language_script_for_label(current_label)
            sections = self._sections_from_variant_blocks(current_blocks, canonical_sections)
            if sections:
                variants.append(
                    DetectedLyricVariant(language=language, script=script, sections=sections)
                )
            current_label = None
            current_blocks = []

        for block in blocks:
            if block.label in METADATA_LABELS:
                flush()
                break

            if block.label in LANGUAGE_LABELS - METADATA_LABELS:
                flush()
                current_label = block.label
                if block.lines:
                    # Re-parse lines to detect inline section headers within variant text
                    sub_blocks = self._reparse_lines_for_sections(block.lines, block.start_pos, block.end_pos)
                    current_blocks.extend(sub_blocks)
                continue

            if current_label is not None:
                current_blocks.append(block)

        flush()
        return variants

    def _sections_from_variant_blocks(
        self,
        blocks: list[_TextBlock],
        canonical_sections: list[DetectedSection],
    ) -> list[DetectedSection]:
        if not blocks:
            return []

        # Collect raw parsed sections from blocks, applying MKS demotion
        raw_sections: list[DetectedSection] = []
        for block in blocks:
            text = "\n".join(line.text for line in block.lines).strip()
            if not text:
                continue
            section_type = SECTION_LABEL_TO_TYPE.get(block.label, SectionType.OTHER)
            raw_sections.append(
                DetectedSection(
                    section_type=section_type,
                    order=0,
                    label=section_type.value.replace("_", " ").title() if section_type != SectionType.OTHER else "Other",
                    text=text,
                    start_pos=block.start_pos,
                    end_pos=block.end_pos,
                )
            )

        # Apply MKS demotion and dual-format merging to variant too
        raw_sections = self._demote_mks(raw_sections)
        raw_sections = self._merge_dual_format(raw_sections)

        # Map to canonical structure by matching type and sequential occurrence
        if not canonical_sections:
            return raw_sections

        type_queues: dict[SectionType, list[DetectedSection]] = {}
        for s in raw_sections:
            type_queues.setdefault(s.section_type, []).append(s)

        sections: list[DetectedSection] = []
        for canonical in canonical_sections:
            queue = type_queues.get(canonical.section_type, [])
            if queue:
                matched = queue.pop(0)
                sections.append(
                    DetectedSection(
                        section_type=canonical.section_type,
                        order=canonical.order,
                        label=canonical.label,
                        text=matched.text,
                        start_pos=matched.start_pos,
                        end_pos=matched.end_pos,
                    )
                )

        return sections

    def _reparse_lines_for_sections(
        self,
        lines: list[_LineToken],
        block_start: int,
        block_end: int,
    ) -> list[_TextBlock]:
        """Re-parse lines within a language block to detect inline section headers."""
        blocks: list[_TextBlock] = []
        current_label = "UNLABELED"
        current_lines: list[_LineToken] = []

        def flush() -> None:
            nonlocal current_lines
            if current_lines:
                blocks.append(_TextBlock(
                    label=current_label,
                    lines=current_lines,
                    start_pos=current_lines[0].start_pos,
                    end_pos=current_lines[-1].end_pos,
                ))
                current_lines = []

        for line in lines:
            header = self._detect_section_header(line.text)
            if header is not None:
                flush()
                current_label = header.label
                if header.remainder:
                    current_lines.append(_LineToken(
                        text=header.remainder,
                        start_pos=line.start_pos,
                        end_pos=line.end_pos,
                    ))
            else:
                current_lines.append(line)

        flush()
        return blocks

    def _extract_script_split_variants(
        self,
        lyric_text: str,
        canonical_sections: list[DetectedSection],
    ) -> list[DetectedLyricVariant]:
        if not canonical_sections:
            return []

        script_sections: dict[str, dict[int, list[str]]] = {}

        for section in canonical_sections:
            for line in section.text.splitlines():
                clean = line.strip()
                if not clean:
                    continue
                script = self._detect_script(clean)
                if script is None:
                    continue
                script_sections.setdefault(script, {}).setdefault(section.order, []).append(clean)

        if not script_sections:
            script = self._detect_script(lyric_text) or "latin"
            language = self._language_for_script(script)
            return [
                DetectedLyricVariant(
                    language=language,
                    script=script,
                    sections=canonical_sections,
                )
            ]

        variants: list[DetectedLyricVariant] = []
        for script, by_order in script_sections.items():
            variant_sections: list[DetectedSection] = []
            for section in canonical_sections:
                lines = by_order.get(section.order)
                if not lines:
                    continue
                variant_sections.append(
                    DetectedSection(
                        section_type=section.section_type,
                        order=section.order,
                        label=section.label,
                        text="\n".join(lines),
                        start_pos=section.start_pos,
                        end_pos=section.end_pos,
                    )
                )
            if variant_sections:
                variants.append(
                    DetectedLyricVariant(
                        language=self._language_for_script(script),
                        script=script,
                        sections=variant_sections,
                    )
                )

        variants.sort(key=lambda v: v.script)
        return variants

    def _find_metadata_boundaries(self, text: str) -> list[MetadataBoundary]:
        boundaries_by_pos: dict[int, MetadataBoundary] = {}
        for label, pattern in METADATA_BOUNDARY_PATTERNS:
            for match in pattern.finditer(text):
                start = match.start()
                existing = boundaries_by_pos.get(start)
                candidate = MetadataBoundary(label=label, start_pos=start, end_pos=match.end())
                if existing is None or candidate.end_pos > existing.end_pos:
                    boundaries_by_pos[start] = candidate
        return [boundaries_by_pos[pos] for pos in sorted(boundaries_by_pos)]

    def _normalize_line(self, line: str) -> str:
        if not line:
            return line
        return re.sub(r"[\u2080-\u2089]", "", line)

    def _is_boilerplate(self, line: str) -> bool:
        lowered = line.lower()
        if "a i i u u" in lowered or "ch j jh" in lowered or "ph b bh m" in lowered:
            return True
        if "pronunciation guide" in lowered:
            return True
        # Filter nOTTu-svara header — it's metadata not lyric content
        if re.match(r"^\s*\(?n[oō]t+u[\s-]*svara\s+s[aā]hityam?\)?\.?\s*$", lowered):
            return True
        if lowered.startswith("updated on "):
            return True
        return any(
            marker in lowered
            for marker in (
                "powered by blogger",
                "newer post",
                "older post",
                "subscribe to",
                "post a comment",
                "blog archive",
                "link to this post",
                "posted by",
                "all rights reserved",
                "copyright",
                "skip to main",
                "related posts",
            )
        )

    def _is_meta_line(self, line: str) -> bool:
        if RAGA_SUBSECTION_PATTERN.search(line) or VILOMA_SUBSECTION_PATTERN.search(line):
            return False
        lowered = line.lower()
        return any(keyword in lowered for keyword in METADATA_KEYWORDS)

    def _language_script_for_label(self, label: str) -> tuple[str, str]:
        mapping = {
            "SANSKRIT": ("sa", "devanagari"),
            "DEVANAGARI": ("sa", "devanagari"),
            "HINDI": ("hi", "devanagari"),
            "TAMIL": ("ta", "tamil"),
            "TELUGU": ("te", "telugu"),
            "KANNADA": ("kn", "kannada"),
            "MALAYALAM": ("ml", "malayalam"),
            "ENGLISH": ("en", "latin"),
            "LATIN": ("en", "latin"),
        }
        return mapping.get(label, ("en", "latin"))

    def _language_for_script(self, script: str) -> str:
        return {
            "devanagari": "sa",
            "tamil": "ta",
            "telugu": "te",
            "kannada": "kn",
            "malayalam": "ml",
            "latin": "en",
        }.get(script, "en")

    def _detect_script(self, text: str) -> Optional[str]:
        counts = {
            "devanagari": 0,
            "tamil": 0,
            "telugu": 0,
            "kannada": 0,
            "malayalam": 0,
            "latin": 0,
        }

        for ch in text:
            cp = ord(ch)
            if 0x0900 <= cp <= 0x097F:
                counts["devanagari"] += 1
            elif 0x0B80 <= cp <= 0x0BFF:
                counts["tamil"] += 1
            elif 0x0C00 <= cp <= 0x0C7F:
                counts["telugu"] += 1
            elif 0x0C80 <= cp <= 0x0CFF:
                counts["kannada"] += 1
            elif 0x0D00 <= cp <= 0x0D7F:
                counts["malayalam"] += 1
            elif 0x0041 <= cp <= 0x024F:
                counts["latin"] += 1

        script, score = max(counts.items(), key=lambda kv: kv[1])
        return script if score > 0 else None

    def to_canonical_sections(self, detected: list[DetectedSection]) -> list[CanonicalSection]:
        return [
            CanonicalSection(
                type=d.section_type,
                order=d.order,
                label=d.label,
            )
            for d in detected
        ]

    def to_canonical_lyric_sections(self, detected: list[DetectedSection]) -> list[CanonicalLyricSection]:
        return [
            CanonicalLyricSection(sectionOrder=d.order, text=d.text)
            for d in detected
            if d.text
        ]

    def to_canonical_lyric_variants(
        self,
        variants: list[DetectedLyricVariant],
    ) -> list[CanonicalLyricVariant]:
        return [
            CanonicalLyricVariant(
                language=variant.language,
                script=variant.script,
                sections=self.to_canonical_lyric_sections(variant.sections),
            )
            for variant in variants
            if variant.sections
        ]

    def to_canonical_metadata_boundaries(
        self,
        boundaries: list[MetadataBoundary],
    ) -> list[CanonicalMetadataBoundary]:
        return [
            CanonicalMetadataBoundary(
                label=b.label,
                startOffset=b.start_pos,
                endOffset=b.end_pos,
            )
            for b in boundaries
        ]
