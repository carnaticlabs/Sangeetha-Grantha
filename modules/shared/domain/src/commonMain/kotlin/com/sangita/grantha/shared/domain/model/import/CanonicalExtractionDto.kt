package com.sangita.grantha.shared.domain.model.import

import kotlinx.serialization.Serializable

/**
 * Canonical Extraction Format — the universal output contract for all source adapters.
 *
 * Whether the source is HTML (Jsoup + Gemini), PDF (PyMuPDF), DOCX (python-docx), or manual entry,
 * every extraction produces this same structure. It is the boundary between the extraction layer
 * and the resolution/quality pipeline.
 *
 * Related: TRACK-041 (Enhanced Sourcing Logic & Structural Voting)
 */
@Serializable
data class CanonicalExtractionDto(
    /** Composition title (required) */
    val title: String,

    /** Alternate title — e.g. transliterated form */
    val alternateTitle: String? = null,

    /** Raw composer name, resolved downstream by EntityResolutionService */
    val composer: String,

    /** Musical form classification */
    val musicalForm: CanonicalMusicalForm = CanonicalMusicalForm.KRITHI,

    /** Raga(s) — ordered list; multiple entries for Ragamalika compositions */
    val ragas: List<CanonicalRagaDto>,

    /** Tala name (raw, resolved downstream) */
    val tala: String,

    /** Canonical section structure — the invariant skeleton */
    val sections: List<CanonicalSectionDto>,

    /** Lyric variants by language/script */
    val lyricVariants: List<CanonicalLyricVariantDto> = emptyList(),

    /** Boundaries where non-lyric metadata blocks begin in source text */
    val metadataBoundaries: List<CanonicalMetadataBoundaryDto> = emptyList(),

    /** Deity name (optional, extracted or inferred) */
    val deity: String? = null,

    /** Temple / Kshetra name (optional) */
    val temple: String? = null,

    /** Temple location — city/region (optional) */
    val templeLocation: String? = null,

    // ─── Provenance ────────────────────────────────────────────────────────

    /** Source URL or file reference (required) */
    val sourceUrl: String,

    /** Human-readable source name, e.g. 'guruguha.org' */
    val sourceName: String,

    /** Source authority tier: 1 (Scholarly) → 5 (Blog) */
    val sourceTier: Int,

    /** How the extraction was performed */
    val extractionMethod: CanonicalExtractionMethod,

    /** When the extraction was performed (ISO-8601) */
    val extractionTimestamp: String? = null,

    /** For PDFs: which pages were extracted, e.g. '42-43' */
    val pageRange: String? = null,

    /** SHA-256 of source content for change detection */
    val checksum: String? = null,
)

@Serializable
data class CanonicalRagaDto(
    /** Raga name (raw, resolved downstream) */
    val name: String,

    /** Order: 1 for primary raga, 2+ for ragamalika segments */
    val order: Int = 1,

    /** Which section this raga covers (for ragamalika) */
    val section: String? = null,
)

@Serializable
data class CanonicalSectionDto(
    /** Section type */
    val type: CanonicalSectionType,

    /** Order within the composition (1-based) */
    val order: Int,

    /** Human-readable label, e.g. 'Charanam 2' */
    val label: String? = null,
)

@Serializable
data class CanonicalLyricVariantDto(
    /** Language code: sa, ta, te, kn, ml, hi, en */
    val language: String,

    /** Script code: devanagari, tamil, telugu, kannada, malayalam, latin */
    val script: String,

    /** Lyric text per section, aligned to the canonical section structure */
    val sections: List<CanonicalLyricSectionDto>,
)

@Serializable
data class CanonicalLyricSectionDto(
    /** Matches CanonicalSectionDto.order */
    val sectionOrder: Int,

    /** Lyric text for this section in this language/script */
    val text: String,
)

@Serializable
data class CanonicalMetadataBoundaryDto(
    /** Boundary label, e.g. MEANING/NOTES/GIST */
    val label: String,

    /** Start offset of the boundary marker in original extraction text */
    val startOffset: Int,

    /** End offset of the boundary marker in original extraction text */
    val endOffset: Int,
)

// ─── Enums ─────────────────────────────────────────────────────────────────

@Serializable
enum class CanonicalMusicalForm {
    KRITHI, VARNAM, SWARAJATHI
}

@Serializable
enum class CanonicalSectionType {
    PALLAVI,
    ANUPALLAVI,
    CHARANAM,
    SAMASHTI_CHARANAM,
    CHITTASWARAM,
    SWARA_SAHITYA,
    MADHYAMA_KALA,
    OTHER,
}

@Serializable
enum class CanonicalExtractionMethod {
    PDF_PYMUPDF,
    PDF_OCR,
    HTML_JSOUP,
    HTML_JSOUP_GEMINI,
    DOCX_PYTHON,
    MANUAL,
    TRANSLITERATION,
}
