"""Canonical Extraction Schema — Pydantic models matching the Kotlin CanonicalExtractionDto.

This is the contract between the Python extraction service and the Kotlin backend.
All source adapters (PDF, DOCX, OCR) must produce output conforming to this schema.
"""

from __future__ import annotations

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class MusicalForm(str, Enum):
    """Musical form classification."""

    KRITHI = "KRITHI"
    VARNAM = "VARNAM"
    SWARAJATHI = "SWARAJATHI"


class SectionType(str, Enum):
    """Canonical section types in Carnatic compositions."""

    PALLAVI = "PALLAVI"
    ANUPALLAVI = "ANUPALLAVI"
    CHARANAM = "CHARANAM"
    SAMASHTI_CHARANAM = "SAMASHTI_CHARANAM"
    CHITTASWARAM = "CHITTASWARAM"
    SWARA_SAHITYA = "SWARA_SAHITYA"
    MADHYAMA_KALA = "MADHYAMA_KALA"
    OTHER = "OTHER"


class ExtractionMethod(str, Enum):
    """How the extraction was performed."""

    PDF_PYMUPDF = "PDF_PYMUPDF"
    PDF_OCR = "PDF_OCR"
    HTML_JSOUP = "HTML_JSOUP"
    HTML_JSOUP_GEMINI = "HTML_JSOUP_GEMINI"
    DOCX_PYTHON = "DOCX_PYTHON"
    MANUAL = "MANUAL"
    TRANSLITERATION = "TRANSLITERATION"


class CanonicalRaga(BaseModel):
    """Raga reference within a composition."""

    name: str = Field(..., description="Raga name (raw, resolved downstream)")
    order: int = Field(1, description="1 for primary raga, 2+ for ragamalika segments")
    section: Optional[str] = Field(None, description="Which section this raga covers (for ragamalika)")


class CanonicalSection(BaseModel):
    """A section in the composition's structural skeleton."""

    type: SectionType = Field(..., description="Section type")
    order: int = Field(..., description="Order within the composition (1-based)")
    label: Optional[str] = Field(None, description="Human-readable label, e.g. 'Charanam 2'")


class CanonicalLyricSection(BaseModel):
    """Lyric text for one section in one language/script."""

    section_order: int = Field(..., alias="sectionOrder", description="Matches CanonicalSection.order")
    text: str = Field(..., description="Lyric text for this section")

    model_config = {"populate_by_name": True}


class CanonicalLyricVariant(BaseModel):
    """A complete lyric text in a specific language and script."""

    language: str = Field(..., description="Language code: sa, ta, te, kn, ml, hi, en")
    script: str = Field(..., description="Script code: devanagari, tamil, telugu, kannada, malayalam, latin")
    sections: list[CanonicalLyricSection] = Field(default_factory=list)


class CanonicalMetadataBoundary(BaseModel):
    """Boundary where non-lyric metadata starts in source text."""

    label: str = Field(..., description="Boundary label, e.g. MEANING/NOTES")
    start_offset: int = Field(..., alias="startOffset", ge=0)
    end_offset: int = Field(..., alias="endOffset", ge=0)

    model_config = {"populate_by_name": True}


class CanonicalIdentityCandidate(BaseModel):
    """A fuzzy-matched reference entity candidate for identity resolution."""

    entity_id: str = Field(..., alias="entityId")
    name: str = Field(..., description="Canonical entity name from reference data")
    score: int = Field(..., ge=0, le=100, description="RapidFuzz score (0-100)")
    confidence: str = Field(..., description="HIGH, MEDIUM, or LOW")
    matched_on: Optional[str] = Field(
        None,
        alias="matchedOn",
        description="Which token matched best: canonical or alias",
    )

    model_config = {"populate_by_name": True}


class CanonicalIdentityCandidates(BaseModel):
    """Identity candidate sets for extracted composer/raga values."""

    composers: list[CanonicalIdentityCandidate] = Field(default_factory=list)
    ragas: list[CanonicalIdentityCandidate] = Field(default_factory=list)


class CanonicalMetadataEnrichment(BaseModel):
    """Metadata enrichment outcome emitted by the optional Gemini phase."""

    provider: str = Field(..., description="Enrichment provider, e.g. google-generativeai")
    model: Optional[str] = Field(None, description="Model name used for enrichment")
    applied: bool = Field(False, description="Whether any canonical metadata field was updated")
    confidence: Optional[float] = Field(None, ge=0.0, le=1.0)
    fields_updated: list[str] = Field(default_factory=list, alias="fieldsUpdated")
    warnings: list[str] = Field(default_factory=list)

    model_config = {"populate_by_name": True}


class CanonicalExtraction(BaseModel):
    """The universal output format for all source adapters.

    Whether the source is HTML, PDF, DOCX, or manual entry, every extraction
    produces this same structure. This is the boundary contract between the
    extraction layer and the Kotlin resolution/quality pipeline.
    """

    # ─── Identity ───────────────────────────────────────────────────────────
    title: str = Field(..., description="Composition title")
    alternate_title: Optional[str] = Field(None, alias="alternateTitle", description="Transliterated form")
    composer: str = Field(..., description="Raw composer name, resolved downstream")
    musical_form: MusicalForm = Field(MusicalForm.KRITHI, alias="musicalForm")

    # ─── Musical structure ──────────────────────────────────────────────────
    ragas: list[CanonicalRaga] = Field(..., min_length=1)
    tala: str = Field(..., description="Tala name (raw, resolved downstream)")
    sections: list[CanonicalSection] = Field(default_factory=list)

    # ─── Lyric content ──────────────────────────────────────────────────────
    lyric_variants: list[CanonicalLyricVariant] = Field(
        default_factory=list, alias="lyricVariants"
    )
    metadata_boundaries: list[CanonicalMetadataBoundary] = Field(
        default_factory=list,
        alias="metadataBoundaries",
    )
    identity_candidates: Optional[CanonicalIdentityCandidates] = Field(
        None,
        alias="identityCandidates",
    )
    metadata_enrichment: Optional[CanonicalMetadataEnrichment] = Field(
        None,
        alias="metadataEnrichment",
    )

    # ─── Metadata ───────────────────────────────────────────────────────────
    deity: Optional[str] = None
    temple: Optional[str] = None
    temple_location: Optional[str] = Field(None, alias="templeLocation")

    # ─── Provenance ─────────────────────────────────────────────────────────
    source_url: str = Field(..., alias="sourceUrl")
    source_name: str = Field(..., alias="sourceName")
    source_tier: int = Field(..., alias="sourceTier", ge=1, le=5)
    extraction_method: ExtractionMethod = Field(..., alias="extractionMethod")
    extraction_timestamp: Optional[str] = Field(None, alias="extractionTimestamp")
    page_range: Optional[str] = Field(None, alias="pageRange")
    checksum: Optional[str] = None

    model_config = {"populate_by_name": True}

    def to_json_dict(self) -> dict:
        """Serialize to JSON-compatible dict using camelCase aliases (for Kotlin interop)."""
        return self.model_dump(mode="json", by_alias=True, exclude_none=True)
