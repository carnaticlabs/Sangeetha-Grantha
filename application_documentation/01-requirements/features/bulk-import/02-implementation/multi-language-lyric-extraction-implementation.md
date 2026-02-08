| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Multi-Language Lyric Extraction and Scraping Robustness – Implementation Summary

**Conductor:** [TRACK-032](../../../../../conductor/tracks/TRACK-032-multi-language-lyric-extraction-analysis.md) – Multi-Language Lyric Extraction from Web Sources

## 1. Purpose

- **Multi-language lyrics:** Extract all language/script variants of lyrics in a single scrape from pages that publish multiple scripts (e.g. English, Devanagari, Tamil, Telugu, Kannada, Malayalam), and create one lyric variant per language/script when approving the import.
- **Scraping robustness:** Fix JSON decoding failures when Gemini returns literal newlines inside string fields (e.g. `lyricVariants[].lyrics`), which produce invalid JSON.

## 2. Categorization of Changes

| Category | Description |
|:---|:---|
| **Scraped schema** | New DTO and field for multi-script lyric variants |
| **Scraping prompt** | Gemini prompt extended to request `lyricVariants` per language/script |
| **Import approval** | Review flow creates multiple lyric variants and section texts when `lyricVariants` is present |
| **Structured JSON parsing** | Preprocess Gemini response to escape literal newlines inside quoted strings |

## 3. Code Changes Summary

### 3.1 Scraped Schema and Prompt

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/WebScrapingService.kt` | **Updated.** Added `ScrapedLyricVariantDto(language, script, lyrics?, sections?)`. Added `lyricVariants: List<ScrapedLyricVariantDto>? = null` to `ScrapedKrithiMetadata`. Extended Gemini prompt (TRACK-032): instruct model to populate `lyricVariants` with one entry per language/script (language codes SA, TA, TE, KN, ML, HI, EN; script values devanagari, tamil, telugu, kannada, malayalam, latin); keep backward compatibility with top-level `lyrics`/`sections`. |

### 3.2 Import Approval – Multiple Lyric Variants

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/ImportService.kt` | **Updated.** In `reviewImport`, when creating krithi from import: if `metadata.lyricVariants` is non-empty, (1) derive section structure from first variant or top-level sections and call `saveSections` once; (2) for each `lyricVariant` create one lyric variant via `createLyricVariant` (language, script from scraped; `parseLanguageCode`/`parseScriptCode` helpers); (3) for each variant with sections, call `saveLyricVariantSections`. Else: retain existing single primary-variant behaviour from `metadata.lyrics`/`metadata.sections`. Added private helpers `parseLanguageCode(String?)` and `parseScriptCode(String?)` mapping scraped codes to `LanguageCode`/`ScriptCode`. |

### 3.3 Structured JSON Parsing Robustness

| File | Change |
|:---|:---|
| `modules/backend/api/.../clients/GeminiApiClient.kt` | **Updated.** Added `escapeNewlinesInsideJsonStrings(input: String): String`: walks raw JSON, tracks in-string state and backslash escaping; replaces literal newline and CR/LF inside double-quoted string values with the two-character sequence `\n` so the result is valid JSON. Called in `generateStructured` after stripping markdown fences and before `json.decodeFromString<T>()`. Prompt appended: "In string fields (lyrics, text, etc.) use escaped newlines \\n for line breaks, not literal newline characters." |

### 3.4 Conductor

| File | Change |
|:---|:---|
| `conductor/tracks/TRACK-032-multi-language-lyric-extraction-analysis.md` | **Updated.** Status set to In Progress; progress log entry for 2026-01-30 implementation. |
| `conductor/tracks.md` | **Updated.** TRACK-032 status set to In Progress. |

## 4. Behaviour After Implementation

- **Scraping:** Gemini is asked to output `lyricVariants` with one object per language/script; each has `language`, `script`, `lyrics`, and optional `sections`. Top-level `lyrics`/`sections` remain for backward compatibility.
- **Import approve:** When `metadata.lyricVariants` is present, one `krithi_lyric_variant` per scraped variant is created (with correct language/script and optional section texts); first variant is primary. When only top-level lyrics/sections exist, behaviour is unchanged (single primary variant).
- **Parsing:** Responses that contain literal newlines inside `lyrics` or other string fields are normalized before decoding, avoiding `JsonDecodingException` (e.g. "Expected quotation mark '\"', but had ' ' instead").

## 5. Commit Reference

Use this file as the single documentation reference for the **Multi-Language Lyric Extraction and Scraping Robustness** commit:

```text
Ref: application_documentation/01-requirements/features/bulk-import/02-implementation/multi-language-lyric-extraction-implementation.md
```

**Suggested commit scope (atomic):** All files listed in §3 above for TRACK-032 (WebScrapingService, ImportService, GeminiApiClient, conductor TRACK-032 + tracks.md).