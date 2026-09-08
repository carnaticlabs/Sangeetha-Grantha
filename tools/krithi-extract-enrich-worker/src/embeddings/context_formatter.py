"""Musicological context formatter for semantic search document chunks.

Embeds structured metadata alongside lyrics so that semantic similarity searches
can discriminate between compositions with identical devotional incipits but distinct
composers, ragas, deities, or kshetras.
"""

from __future__ import annotations

import re
import unicodedata

BOILERPLATE_PATTERNS = [
    re.compile(r"^Transliteration[–-].*$", re.IGNORECASE),
    re.compile(r"^Transliteration as per.*$", re.IGNORECASE),
    re.compile(r"^\(including.*letters.*$", re.IGNORECASE),
    re.compile(r"^\([eE] – short \| [eE] – Long.*$", re.IGNORECASE),
    re.compile(r"^[aA]\s+[aA]\s+[iI]\s+[iI].*$", re.IGNORECASE),
    re.compile(r"^[kK]\s+kh\s+g\s+gh.*$", re.IGNORECASE),
    re.compile(r"^[tT]\s+th\s+d\s+dh.*$", re.IGNORECASE),
    re.compile(r"^[pP]\s+ph\s+b\s+bh.*$", re.IGNORECASE),
    re.compile(r"^[sS]\s+sh\s+s\s+h.*$", re.IGNORECASE),
    re.compile(r"^https?://\S+$", re.IGNORECASE),
    re.compile(r"^Refer to .*https?://.*$", re.IGNORECASE),
]


def strip_diacritics(text: str) -> str:
    """Strips combining diacritical marks to standard ASCII characters."""
    if not text:
        return ""
    nfkd = unicodedata.normalize("NFKD", text)
    return "".join(c for c in nfkd if unicodedata.category(c) != "Mn")


def format_raga_header(raga: str) -> str:
    """Formats the [Raga: ...] header, aliasing diacritics if present.

    E.g., 'Chārukesi' becomes '[Raga: Chārukesi / Charukesi]'.
    If no diacritics are present, returns '[Raga: Charukesi]'.
    """
    clean_raga = raga.strip()
    ascii_raga = strip_diacritics(clean_raga)
    if ascii_raga.lower() != clean_raga.lower():
        return f"[Raga: {clean_raga} / {ascii_raga}]"
    return f"[Raga: {clean_raga}]"


def _is_boilerplate_line(line: str) -> bool:
    stripped = line.strip()
    return any(p.match(stripped) for p in BOILERPLATE_PATTERNS)


def _clean_text(text: str) -> str:
    """Normalize internal spacing, newlines, and filter scraper boilerplate."""
    if not text:
        return ""
    lines = []
    for raw_line in text.strip().splitlines():
        line = raw_line.strip()
        if not line or _is_boilerplate_line(line):
            continue
        lines.append(line)
    return "\n".join(lines)


def _lakshana_headers(
    title: str,
    composer: str,
    raga: str | None = None,
    tala: str | None = None,
    deity: str | None = None,
    kshetra: str | None = None,
    tags: list[str] | None = None,
    section_type: str | None = None,
    musical_form: str | None = None,
) -> list[str]:
    header_parts = [f"[Composition: {title.strip()}]", f"[Composer: {composer.strip()}]"]
    if musical_form:
        header_parts.append(f"[Musical Form: {musical_form.strip().upper()}]")
    if section_type:
        header_parts.append(f"[Section: {section_type.strip().upper()}]")
    if raga:
        header_parts.append(format_raga_header(raga))
    if tala:
        header_parts.append(f"[Tala: {tala.strip()}]")
    if deity:
        header_parts.append(f"[Deity: {deity.strip()}]")
    if kshetra:
        header_parts.append(f"[Kshetra: {kshetra.strip()}]")
    if tags:
        valid_tags = [t.strip() for t in tags if t and t.strip()]
        if valid_tags:
            header_parts.append(f"[Thematic Group / Tags: {', '.join(valid_tags)}]")
    return header_parts


def format_composition_overview(
    title: str,
    composer: str,
    raga: str | None = None,
    tala: str | None = None,
    deity: str | None = None,
    kshetra: str | None = None,
    tags: list[str] | None = None,
    language: str | None = None,
    script: str | None = None,
    primary_lyrics: str | None = None,
    musical_form: str | None = None,
) -> str:
    """Formats a macro-level overview document representing the entire composition.

    Includes musical lakshana headers followed by the primary sahitya.
    """
    header = " ".join(
        _lakshana_headers(
            title,
            composer,
            raga=raga,
            tala=tala,
            deity=deity,
            kshetra=kshetra,
            tags=tags,
            musical_form=musical_form,
        )
    )

    body_parts = [header]
    meta_info = []
    if language:
        meta_info.append(f"Language: {language}")
    if script:
        meta_info.append(f"Script: {script}")
    if meta_info:
        body_parts.append(f"[{', '.join(meta_info)}]")

    if primary_lyrics:
        cleaned_lyrics = _clean_text(primary_lyrics)
        if cleaned_lyrics:
            body_parts.append("Sahitya:\n" + cleaned_lyrics)

    return "\n".join(body_parts)


def format_section_passage(
    title: str,
    composer: str,
    section_type: str,
    section_text: str,
    raga: str | None = None,
    tala: str | None = None,
    deity: str | None = None,
    kshetra: str | None = None,
    tags: list[str] | None = None,
    language: str | None = None,
    script: str | None = None,
    musical_form: str | None = None,
) -> str:
    """Formats an individual section passage (e.g., Pallavi, Anupallavi, Charanam).

    Allows fine-grained semantic matches on remembered single lines or stanzas.
    """
    header_parts = _lakshana_headers(
        title,
        composer,
        raga=raga,
        tala=tala,
        deity=deity,
        kshetra=kshetra,
        tags=tags,
        section_type=section_type,
        musical_form=musical_form,
    )
    if language:
        header_parts.append(f"[Language: {language.strip()}]")
    if script:
        header_parts.append(f"[Script: {script.strip()}]")

    header = " ".join(header_parts)
    cleaned_section = _clean_text(section_text)
    return f"{header}\nText:\n{cleaned_section}"
