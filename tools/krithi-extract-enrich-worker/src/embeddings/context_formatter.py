"""Musicological context formatter for semantic search document chunks.

Embeds structured metadata alongside lyrics so that semantic similarity searches
can discriminate between compositions with identical devotional incipits but distinct
composers, ragas, deities, or kshetras.
"""

from __future__ import annotations

import re
import unicodedata
from typing import Any

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

MAX_HEADER_WORDS = 60


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


def _format_cycle_clause(cycle_info: dict[str, Any]) -> str | None:
    """Formats a discriminative cycle clause matching TRACK-145 specification."""
    slug = cycle_info.get("slug") or ""
    display_name = cycle_info.get("display_name") or cycle_info.get("name") or "Canonical Cycle"
    role = cycle_info.get("role")
    axis_value = cycle_info.get("axis_value")
    attrs = cycle_info.get("attrs") or {}

    if slug == "kamalamba-navavarnam":
        if role == "CORE":
            avarana = attrs.get("avarana") or cycle_info.get("sequence_order")
            chakra = axis_value
            yogini = attrs.get("yogini")
            parts = [f"Cycle: Kamalamba Navavarnam"]
            if avarana:
                parts.append(f"Avarana: {avarana}")
            if chakra:
                parts.append(f"Chakra: {chakra}")
            if yogini:
                parts.append(f"Yogini: {yogini}")
            return f"[{'; '.join(parts)}]"
        elif role == "DHYANA":
            return "[Cycle: Kamalamba Navavarnam; Role: Dhyana]"
        elif role == "MANGALAM":
            return "[Cycle: Kamalamba Navavarnam; Role: Mangalam]"
        return "[Cycle: Kamalamba Navavarnam]"

    elif slug == "nottusvara-sahitya":
        if axis_value:
            return f"[Cycle: Nottusvara Sahitya; Tune: {axis_value.strip()}]"
        return "[Cycle: Nottusvara Sahitya]"

    elif slug == "tiruvarur-panchalinga":
        if axis_value:
            return f"[Cycle: Tiruvarur Pancha Linga; Lingam: {axis_value.strip()}]"
        return "[Cycle: Tiruvarur Pancha Linga]"

    elif slug == "navagraha-krithis":
        graha = axis_value or attrs.get("graha")
        if graha:
            return f"[Cycle: Navagraha; Graha: {graha.capitalize()}]"
        return "[Cycle: Navagraha]"

    elif slug == "pancha-bhuta-sthala":
        return "[Cycle: Pancha Bhuta Sthala]"

    else:
        role_suffix = f"; Role: {role.capitalize()}" if role and role in ("DHYANA", "MANGALAM") else ""
        return f"[Cycle: {display_name}{role_suffix}]"


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
    vibhakti_case: str | None = None,
    vibhakti_stem: str | None = None,
    mandalam: str | None = None,
    bhuta: str | None = None,
    deity_posture: str | None = None,
    cycle_info: dict[str, Any] | None = None,
    occasion_note: str | None = None,
    structural_features: list[str] | None = None,
    yati_pattern: str | None = None,
) -> list[str]:
    """Generates structured micro-clauses with strict token budget enforcement."""
    core_parts: list[str] = [f"[Composition: {title.strip()}]", f"[Composer: {composer.strip()}]"]

    if musical_form:
        core_parts.append(f"[Musical Form: {musical_form.strip().upper()}]")
    if section_type:
        core_parts.append(f"[Section: {section_type.strip().upper()}]")
    if raga:
        core_parts.append(format_raga_header(raga))
    if tala:
        core_parts.append(f"[Tala: {tala.strip()}]")
    if deity:
        core_parts.append(f"[Deity: {deity.strip()}]")
    if kshetra:
        core_parts.append(f"[Kshetra: {kshetra.strip()}]")
    if mandalam:
        core_parts.append(f"[Mandalam: {mandalam.strip().capitalize()}]")
    if bhuta:
        core_parts.append(f"[Element: {bhuta.strip().capitalize()}]")
    if deity_posture:
        core_parts.append(f"[Posture: {deity_posture.strip().capitalize()}]")

    # Grammar / Vibhakti
    if vibhakti_case:
        clean_case = vibhakti_case.strip().upper()
        case_name = "Sarva Vibhakti" if clean_case == "SARVA_VIBHAKTI" else clean_case.capitalize()
        core_parts.append(f"[Grammar: {case_name}]")

    # Cycle clause
    if cycle_info:
        clause = _format_cycle_clause(cycle_info)
        if clause:
            core_parts.append(clause)

    # Structural features
    if structural_features:
        features = [f.replace("_", " ").title() for f in structural_features if f]
        if features:
            core_parts.append(f"[Structure: {', '.join(features)}]")

    if yati_pattern and yati_pattern.strip().upper() != "NONE":
        core_parts.append(f"[Yati: {yati_pattern.strip().capitalize()}]")

    # Optional / lower priority clauses
    secondary_parts: list[str] = []
    if occasion_note:
        secondary_parts.append(f"[Occasion: {occasion_note.strip()}]")

    if tags:
        # Filter out cycle names if already captured
        cycle_slug = cycle_info.get("slug") if cycle_info else None
        valid_tags = [
            t.strip()
            for t in tags
            if t and t.strip() and (not cycle_slug or cycle_slug.lower() not in t.lower().replace(" ", "-"))
        ]
        if valid_tags:
            secondary_parts.append(f"[Thematic Group / Tags: {', '.join(valid_tags[:3])}]")

    # Assemble and enforce MAX_HEADER_WORDS budget
    combined_parts = list(core_parts)
    for part in secondary_parts:
        candidate_words = len(" ".join(combined_parts + [part]).split())
        if candidate_words <= MAX_HEADER_WORDS:
            combined_parts.append(part)

    # In rare cases where core_parts itself exceeds budget, prune least discriminative
    while len(" ".join(combined_parts).split()) > MAX_HEADER_WORDS and len(combined_parts) > 2:
        combined_parts.pop()

    return combined_parts


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
    vibhakti_case: str | None = None,
    vibhakti_stem: str | None = None,
    mandalam: str | None = None,
    bhuta: str | None = None,
    deity_posture: str | None = None,
    cycle_info: dict[str, Any] | None = None,
    occasion_note: str | None = None,
    structural_features: list[str] | None = None,
    yati_pattern: str | None = None,
) -> str:
    """Formats a macro-level overview document representing the entire composition.

    Includes musical lakshana headers followed by the primary sahitya.
    Guarantees metadata header does not exceed 60 words.
    """
    headers = _lakshana_headers(
        title,
        composer,
        raga=raga,
        tala=tala,
        deity=deity,
        kshetra=kshetra,
        tags=tags,
        musical_form=musical_form,
        vibhakti_case=vibhakti_case,
        vibhakti_stem=vibhakti_stem,
        mandalam=mandalam,
        bhuta=bhuta,
        deity_posture=deity_posture,
        cycle_info=cycle_info,
        occasion_note=occasion_note,
        structural_features=structural_features,
        yati_pattern=yati_pattern,
    )
    header = " ".join(headers)

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
    vibhakti_case: str | None = None,
    mandalam: str | None = None,
    bhuta: str | None = None,
    cycle_info: dict[str, Any] | None = None,
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
        vibhakti_case=vibhakti_case,
        mandalam=mandalam,
        bhuta=bhuta,
        cycle_info=cycle_info,
    )
    if language:
        header_parts.append(f"[Language: {language.strip()}]")
    if script:
        header_parts.append(f"[Script: {script.strip()}]")

    header = " ".join(header_parts)
    cleaned_section = _clean_text(section_text)
    return f"{header}\nText:\n{cleaned_section}"


def format_cycle_overview(
    cycle_name: str,
    composer: str,
    description: str,
    members: list[dict[str, Any]],
    kshetra: str | None = None,
    mandalam: str | None = None,
) -> str:
    """Formats a macro CYCLE_OVERVIEW document representing an entire thematic cycle."""
    header_parts = [f"[Cycle: {cycle_name.strip()}]", f"[Composer: {composer.strip()}]"]
    if kshetra:
        header_parts.append(f"[Kshetra: {kshetra.strip()}]")
    if mandalam:
        header_parts.append(f"[Mandalam: {mandalam.strip().capitalize()}]")
    header = " ".join(header_parts)

    body_parts = [header, "Overview:", description.strip(), "\nCompositions in Cycle:"]
    for m in members:
        seq = m.get("sequence_order", 0)
        t = m.get("title", "")
        r = m.get("raga", "")
        role = m.get("role", "CORE")
        axis = m.get("axis_value")
        case = m.get("vibhakti_case")
        attrs = []
        if role != "CORE":
            attrs.append(f"Role: {role.capitalize()}")
        if axis:
            attrs.append(f"Axis: {axis}")
        if case:
            attrs.append(f"Case: {case.capitalize()}")
        attr_str = f" [{'; '.join(attrs)}]" if attrs else ""
        body_parts.append(f"{seq}. {t} ({r}){attr_str}")

    return "\n".join(body_parts)


def format_kshetra_overview(
    temple_name: str,
    city: str,
    state: str,
    mandalam: str | None = None,
    bhuta: str | None = None,
    deity_posture: str | None = None,
    notes: str | None = None,
    kritis: list[dict[str, Any]] | None = None,
    is_gap: bool = False,
) -> str:
    """Formats a macro KSHETRA_OVERVIEW document representing a shrine and its musical corpus."""
    header_parts = [f"[Kshetra: {temple_name.strip()} ({city.strip()})]"]
    if mandalam:
        header_parts.append(f"[Mandalam: {mandalam.strip().capitalize()}]")
    if bhuta:
        header_parts.append(f"[Element: {bhuta.strip().capitalize()}]")
    if deity_posture:
        header_parts.append(f"[Posture: {deity_posture.strip().capitalize()}]")
    header = " ".join(header_parts)

    body_parts = [header, f"Location: {city.strip()}, {state.strip()}."]
    if notes:
        body_parts.append(f"Sthala Background:\n{notes.strip()}")

    if is_gap or not kritis:
        body_parts.append("Attestation Note:\nNo Muthuswami Dikshitar compositions are attested for this temple in the canonical repertoire.")
    else:
        body_parts.append(f"Attested Compositions ({len(kritis)}):")
        for k in kritis:
            title = k.get("title", "")
            raga = k.get("raga", "")
            tala = k.get("tala", "")
            vib = k.get("vibhakti_case")
            vib_str = f" [Grammar: {vib.capitalize()}]" if vib else ""
            body_parts.append(f"- {title} ({raga} / {tala}){vib_str}")

    return "\n".join(body_parts)
