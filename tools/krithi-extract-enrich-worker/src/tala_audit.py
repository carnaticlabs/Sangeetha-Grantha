"""TRACK-144: read-only, source-backed tala candidates. Never applies catalogue changes.

Reuses the production HTML and metadata parsers. A candidate is evidence to review,
not approval: source identity, competing traditions and incipit must still be checked.
"""

from __future__ import annotations

import hashlib
import re
import unicodedata
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field

from .html_extractor import HtmlTextExtractor
from .metadata_parser import MetadataParser


def comparison_key(value: str) -> str:
    """Conservative comparison only; not the application's raga identity resolver."""
    text = unicodedata.normalize("NFKD", value.casefold())
    return "".join(c for c in text if c.isalpha() and not unicodedata.combining(c))


# Reviewed spellings only. Bare Capu/Ekam/Triputa carry no inferred subtype.
TALA_SPELLINGS = {
    "adi": "Adi",
    "aadi": "Adi",
    "rupaka": "Rupaka",
    "rupakam": "Rupaka",
    "capu": "Capu",
    "chapu": "Capu",
    "misracapu": "Misra Capu",
    "misrachapu": "Misra Capu",
    "khandacapu": "Khanda Capu",
    "khandachapu": "Khanda Capu",
    "jhampa": "Jhampa",
    "jhampe": "Jhampa",
    "ata": "Ata",
    "triputa": "Triputa",
    "ekam": "Ekam",
    "tisraekam": "Tisra Ekam",
    "tisratriputa": "Tisra Triputa",
    "khandatriputa": "Khanda Triputa",
    "catusraekam": "Catusra Ekam",
    "caturasraekam": "Catusra Ekam",
    "khandaekam": "Khanda Ekam",
}


class SourceRef(BaseModel):
    source_url: str


class ImportRef(BaseModel):
    import_id: UUID
    title: str | None = None
    raga: str | None = None
    tala: str | None = None
    source_url: str | None = None


class InventoryRow(BaseModel):
    id: UUID
    title: str
    composer: str
    raga: str | None = None
    pallavi: str | None = None
    evidence: list[SourceRef] = Field(default_factory=list)
    imports: list[ImportRef] = Field(default_factory=list)


class Inventory(BaseModel):
    rows: list[InventoryRow]


class TalaCandidate(BaseModel):
    krithi_id: UUID
    title: str
    composer: str
    catalogue_raga: str | None
    source_url: str
    source_checksum: str | None = None
    source_title: str | None = None
    source_raga: str | None = None
    raw_tala: str | None = None
    proposed_tala: str | None = None
    evidence_excerpt: str | None = None
    pallavi_found: bool = False
    status: Literal[
        "candidate", "identity_review", "no_explicit_tala", "unrecognized_tala", "fetch_failed", "source_missing"
    ]
    review_required: bool = True
    notes: list[str] = Field(default_factory=list)
    extractor_version: str = "track144-tala-audit-1"


class TalaAuditor:
    def examine(self, row: InventoryRow, source_url: str, html: str) -> TalaCandidate:
        content = HtmlTextExtractor().extract(html, base_url=source_url)
        # Only the opening composition header, never references/comments or another song's tala.
        header = re.split(r"(?m)^\s*(?:[Pp]allavi\b|P[. ]|[Aa]nupallavi\b|[Cc]ara[Nn]am\b)", content.text, maxsplit=1)[
            0
        ]
        header = header[:2500]
        metadata = MetadataParser().parse("Source\n" + header, title_hint=content.title)
        # The production parser also accepts unlabeled "X - Y" headers. Here an
        # explicit tala label is required: "Transliteration - Telugu" is not evidence.
        raw = metadata.tala if MetadataParser.EXPLICIT_TALA_LABEL.search(header) else None
        proposed = TALA_SPELLINGS.get(comparison_key(raw or ""))
        status: Literal["candidate", "identity_review", "no_explicit_tala", "unrecognized_tala"] = (
            "candidate" if proposed else "unrecognized_tala" if raw else "no_explicit_tala"
        )
        notes = ["Verify composer, raga and incipit against this source before accepting."]
        # Linked imports are not trusted identities. Surface mismatches rather than copying their tala.
        for imported in row.imports:
            if (imported.source_url or "").removeprefix("http://").removeprefix("https://") != (
                source_url.removeprefix("http://").removeprefix("https://")
            ):
                continue
            if imported.raga and row.raga and comparison_key(imported.raga) != comparison_key(row.raga):
                notes.append(f"Linked import raga {imported.raga!r} differs; verify alias or wrong-composition match.")
                status = "identity_review"
        pallavi = comparison_key(row.pallavi or "")
        # Short title fragments are not sufficient evidence of a composition match.
        pallavi_found = len(pallavi) >= 20 and pallavi in comparison_key(content.text)
        if not pallavi_found:
            notes.append("Stored Latin pallavi absent or not found verbatim after punctuation normalization.")
        excerpt = None
        if raw:
            # Keep a bounded header excerpt; checksum identifies the cached UTF-8 HTML.
            match = MetadataParser.EXPLICIT_TALA_LABEL.search(header)
            if match:
                excerpt = " ".join(header[max(0, match.start() - 50) : match.start() + 150].split())
        return TalaCandidate(
            krithi_id=row.id,
            title=row.title,
            composer=row.composer,
            catalogue_raga=row.raga,
            source_url=source_url,
            source_checksum=hashlib.sha256(html.encode()).hexdigest(),
            source_title=content.title,
            source_raga=metadata.raga,
            raw_tala=raw,
            proposed_tala=proposed,
            evidence_excerpt=excerpt,
            pallavi_found=pallavi_found,
            status=status,
            notes=notes,
        )
