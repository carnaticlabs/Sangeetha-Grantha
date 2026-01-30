# TRACK-032: Multi-Language Lyric Extraction from Web Sources

| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-30 |
| **Author** | Sangeetha Grantha Team |
| **Related Tracks** | TRACK-001 (Bulk Import), TRACK-029 (Kshetra/Deity) |

## 1. Goal
Extract all language/script variants of lyrics in a single scrape from pages that publish multiple scripts (e.g. [Guru Guha – Bala Kuchambike](http://guru-guha.blogspot.com/2008/02/dikshitar-kriti-bala-kuchambike-raga.html): English, Devanagari, Tamil, Telugu, Kannada, Malayalam), and create one lyric variant per language/script when approving the import.

## 2. Problem Statement
- **Current behaviour**: Scraping returns a single `lyrics` string (and optional `sections`). Approve flow creates at most one lyric variant (e.g. Latin/primary). Other scripts on the page are ignored.
- **Desired**: One scrape should yield structured lyrics by language/script (e.g. `lyricVariants: [{ language: 'SA', script: 'DEVANAGARI', sections: [...] }, { language: 'TA', script: 'TAMIL', sections: [...] }, ...]`), and approve should create multiple lyric variants in one go.
- **Benefit**: Reduces manual work to add Tamil/Telugu/Devanagari etc. later; improves coverage for multi-script sources (Guru Guha, others).

## 3. Scope (Analysis)
- **In scope**: Web scraping schema and prompt; `ScrapedKrithiMetadata` and import-approve flow; creating multiple lyric variants from one import.
- **Out of scope**: Transliteration (separate feature); editing lyrics in the krithi editor (existing).

## 4. Implementation Plan (To Be Refined)

### 4.1 Scraping Schema
- Extend `ScrapedKrithiMetadata` (or add) with a list of lyric variants, e.g. `lyricVariants: List<ScrapedLyricVariantDto>` where each has `language`, `script`, `sections` (or `lyrics` + optional section breakdown).
- **Gemini prompt**: Ask the model to detect and output each distinct language/script block (e.g. "English", "Devanagari", "Tamil", "Telugu", "Kannada", "Malayalam") with the corresponding text/sections. Define a stable set of language/script codes (SA/DEVANAGARI, TA/TAMIL, TE/TELUGU, etc.).

### 4.2 Prompt and Parsing
- Update WebScrapingService prompt to: (1) keep existing single `lyrics`/`sections` for backward compatibility or as fallback; (2) add "If the page contains lyrics in multiple languages or scripts, also populate `lyricVariants` with one entry per language/script and the corresponding text/sections."
- Parse response into `ScrapedLyricVariantDto` (language, script, sections or full lyrics).

### 4.3 Import Approval
- When creating krithi from import: if `metadata.lyricVariants` is non-empty, create one `krithi_lyric_variant` per scraped variant (with correct language/script), then create sections and `krithi_lyric_sections` per variant from scraped sections.
- If only `metadata.lyrics`/`metadata.sections` exist, keep current behaviour (single primary variant).
- Primary language of the krithi: set from first variant or from existing language detection; optionally allow override in review UI.

### 4.4 Section Structure
- Reuse existing section types (PALLAVI, SAMASHTI_CHARANAM, etc.). Per-variant sections may have different line counts; store section text per variant as today.

### 4.5 Source Reference
- Store source URL (or batch/source key) on each lyric variant for traceability.

## 5. References
- [Guru Guha – Bala Kuchambike (Raga Surati)](http://guru-guha.blogspot.com/2008/02/dikshitar-kriti-bala-kuchambike-raga.html) – Example page with English, Devanagari, Tamil, Telugu, Kannada, Malayalam lyrics and section labels (pallavi, samashTi caraNam).
- [Lyric structure in bulk import](../application_documentation/01-requirements/features/bulk-import/01-strategy/master-analysis.md) – Sections and lyric variants.
- Domain: `krithi_lyric_variants`, `krithi_lyric_sections`, `krithi_sections` (existing).

## 6. Progress Log
- 2026-01-30: Track created; analysis and implementation plan drafted.
- 2026-01-30: Implemented ScrapedLyricVariantDto and lyricVariants on ScrapedKrithiMetadata; updated WebScrapingService prompt for multi-language/script extraction; ImportService.reviewImport creates one lyric variant per scraped lyricVariant (language/script/sections) when lyricVariants non-empty, with parseLanguageCode/parseScriptCode helpers.
