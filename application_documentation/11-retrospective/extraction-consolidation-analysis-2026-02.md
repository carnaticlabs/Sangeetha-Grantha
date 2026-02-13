# Analysis: Consolidating All Extraction Logic into the Python Service

| Metadata | Value |
|:---|:---|
| **Status** | Proposal |
| **Date** | 2026-02-12 |
| **Author** | Claude Opus 4.6 (requested by Seshadri) |
| **Motivation** | Remediation retrospective Finding 2.1 — Architectural Divergence: The Heuristic Split |
| **Decision Required** | Should HTML extraction move from Kotlin to Python? |

---

## 1. Executive Summary

The Kotlin backend currently contains ~1,200 lines of extraction, parsing, and scraping logic spread across 7 files (WebScrapingService, DeterministicWebScraper, KrithiStructureParser, HtmlTextExtractor, GeminiApiClient, TempleScrapingService, ScrapeJsonSanitizer). The Python service (`tools/pdf-extractor/`) contains ~800 lines of parallel extraction logic for PDFs.

The two codebases duplicate domain heuristics — section detection regexes, metadata parsing, diacritic normalisation — in different languages, leading to the persistent MADHYAMAKALA bug and other divergences documented in the remediation retrospective.

**This document recommends consolidating all extraction (PDF + HTML) into the Python service**, leaving the Kotlin backend focused on domain logic, persistence, API routing, and orchestration.

---

## 2. What Currently Lives Where

### 2.1 Kotlin Backend — Extraction Logic (to be moved)

| File | Lines | Responsibility | External Deps |
|:---|:---|:---|:---|
| `WebScrapingService.kt` | ~460 | HTML fetch, Jsoup extraction, Gemini LLM prompt construction, response parsing, temple enrichment | Ktor HTTP, Jsoup, GeminiApiClient |
| `DeterministicWebScraper.kt` | ~150 | LLM-free HTML scraping fallback, metadata hints from URL/title patterns | Ktor HTTP, Jsoup |
| `KrithiStructureParser.kt` | ~535 | Section detection (Pallavi/Anupallavi/Charanam + 10 variants), language header detection, lyric variant extraction, metadata hint extraction, boilerplate filtering | Pure Kotlin regex |
| `HtmlTextExtractor.kt` | ~115 | Jsoup DOM traversal, block-element-aware text extraction, link annotation | Jsoup |
| `GeminiApiClient.kt` | ~470 | Gemini API HTTP client with rate limiting, retry, 429/503 handling, schema mode, JSON sanitisation | Ktor HTTP |
| `TempleScrapingService.kt` | ~100 | Temple/kshetra details scraping from nested URLs | Ktor HTTP, Jsoup |
| `ScrapeJsonSanitizer.kt` | ~50 | Gemini response JSON repair (trailing commas, markdown blocks) | Pure Kotlin |
| **Total** | **~1,880** | | |

### 2.2 Python Service — Current Extraction Logic (already in place)

| File | Lines | Responsibility | External Deps |
|:---|:---|:---|:---|
| `extractor.py` | ~180 | PyMuPDF text extraction with font metadata, OCR fallback trigger | PyMuPDF, pytesseract |
| `page_segmenter.py` | ~200 | PDF page-level Krithi boundary detection (bold/font-size heuristic) | None (pure Python) |
| `structure_parser.py` | ~215 | Section label detection (same domain patterns as KrithiStructureParser.kt) | None (pure Python) |
| `metadata_parser.py` | ~200 | Raga/tala/composer/deity/temple extraction from header text | diacritic_normalizer |
| `diacritic_normalizer.py` | ~140 | PDF garbled diacritic repair (Rules 1-8) | None (pure Python) |
| `velthuis_decoder.py` | ~290 | Velthuis Devanagari font glyph mapping | None (pure Python) |
| `worker.py` | ~200 | Queue polling, extraction orchestration | psycopg, httpx |
| `db.py` | ~100 | PostgreSQL queue operations | psycopg |
| **Total** | **~1,525** | | |

### 2.3 Kotlin Backend — Domain/Persistence Logic (stays)

| File | Responsibility |
|:---|:---|
| `ExtractionResultProcessor.kt` | Reads completed extraction results, dedup, Krithi creation/matching |
| `KrithiCreationFromExtractionService.kt` | Creates Krithi records from canonical extractions |
| `VariantMatchingService.kt` | Cross-language variant matching and confidence scoring |
| `NameNormalizationService.kt` | Transliteration-aware normalisation for matching keys |
| `DeduplicationService.kt` | Fuzzy title matching with metadata-aware thresholds |
| `ImportService.kt` | Import orchestration, approval workflow |
| All DAL repositories | Database persistence |
| All API routes | REST endpoints |

---

## 3. The Duplicated Heuristics Problem

The retrospective identified this as the root cause of the MADHYAMAKALA bug. Here's the concrete evidence:

### 3.1 Section Detection — Two Implementations

**Kotlin** (`KrithiStructureParser.kt:362-461`) — 100 regex patterns covering:
- Latin script: `pallavi`, `anupallavi`, `charanam`, `chittaswaram`, `madhyama kala`, etc.
- Single-letter abbreviations: `P`, `A`, `C`, `Ch`
- Devanagari: `पल्लवि`, `अनुपल्लवि`, `चरणम्`, `समष्टि चरणम्`, `मध्यम काल साहित्यम्`
- Tamil: `பல்லவி`, `அனுபல்லவி`, `சரணம்`
- Telugu: `పల్లవి`, `అనుపల్లవి`, `చరణం`
- Kannada: `ಪಲ್ಲವಿ`, `ಅನುಪಲ್ಲವಿ`, `ಚರಣ`
- Malayalam: `പല്ലവി`, `അനുപല്ലവി`, `ചരണം`

**Python** (`structure_parser.py:34-93`) — 7 pattern groups covering:
- Latin + garbled forms: `pallavi`, `anupallavi`, `charanam`, `caran.am`
- Devanagari: `पल्लवि`, `अनुपल्लवि`, `चरणम्`, `समष्टि`
- Madhyama Kala with sahityam variant

The Kotlin version has **far more patterns** (Indic script abbreviations, 5 Indic scripts). The Python version handles garbled PDF forms (`caran.am`). Neither is a superset of the other.

### 3.2 Metadata Parsing — Two Implementations

**Kotlin** (`KrithiStructureParser.extractMetadataHints()`) — Parses blogspot-style meta lines:
```
abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi
```

**Python** (`MetadataParser.parse()`) — Parses guruguha.org PDF-style headers:
```
Rāga: Kalyāṇi — Tāla: Ādi
```
Also handles garbled Utopia font patterns.

Again, neither is a superset. Both have domain-specific patterns the other lacks.

### 3.3 Impact of Divergence

When the Kotlin `MADHYAMAKALA` regex was fixed to handle `(madhyama kAla sAhityam)`, the Python `structure_parser.py` was not updated. When the Python `diacritic_normalizer.py` was enhanced with Rule 8, the Kotlin `NameNormalizationService` was not aware. Improvements in one language rotted the other.

---

## 4. Proposed Architecture: Unified Python Extraction Service

### 4.1 High-Level Design

```
                         ┌─────────────────────────┐
                         │   Kotlin Backend (Ktor)  │
                         │                          │
                         │  - API Routes            │
                         │  - Import Orchestration  │
                         │  - Deduplication         │
                         │  - Variant Matching      │
                         │  - Krithi CRUD           │
                         │  - Persistence (DAL)     │
                         └────────┬────────────────┘
                                  │ writes to extraction_queue
                                  │ reads from extraction_queue (DONE)
                                  ▼
                         ┌─────────────────────────┐
                         │   PostgreSQL             │
                         │   extraction_queue       │
                         └────────┬────────────────┘
                                  │ polls queue
                                  ▼
                         ┌─────────────────────────┐
                         │  Python Extraction Svc   │
                         │                          │
                         │  - PDF extraction        │
                         │  - HTML extraction (NEW) │
                         │  - Gemini LLM calls (NEW)│
                         │  - Section detection     │
                         │  - Metadata parsing      │
                         │  - Diacritic normalisation│
                         │  - Script detection      │
                         │  - Lyric variant extract  │
                         │  - Temple scraping (NEW) │
                         │                          │
                         │  Output: CanonicalExtractionDto │
                         └─────────────────────────┘
```

### 4.2 What Moves to Python

| Current Kotlin Component | Python Equivalent | Notes |
|:---|:---|:---|
| `HtmlTextExtractor` (Jsoup) | `BeautifulSoup4` or `lxml` | Direct replacement — BS4 is the Python standard |
| `KrithiStructureParser` (regex) | Merge into `structure_parser.py` | Unify all 100+ patterns into one file |
| `WebScrapingService` Gemini prompt | `google-generativeai` SDK | Native Python SDK, simpler than Ktor HTTP wrapper |
| `GeminiApiClient` (HTTP + retry) | `google-generativeai` SDK | SDK handles retries, rate limiting natively |
| `DeterministicWebScraper` | Built into the unified extractor | LLM-free path becomes a mode flag |
| `TempleScrapingService` | `httpx` + `beautifulsoup4` | Straightforward port |
| `ScrapeJsonSanitizer` | Not needed — Python `json` + SDK handle this | Gemini Python SDK returns structured objects |

### 4.3 What Stays in Kotlin

| Component | Reason |
|:---|:---|
| `ExtractionResultProcessor` | Orchestration — reads queue results, triggers dedup/creation |
| `KrithiCreationFromExtractionService` | Domain logic — creates Krithi records with FK resolution |
| `VariantMatchingService` | Domain logic — cross-language matching with confidence scoring |
| `NameNormalizationService` | Used by persistence layer for matching keys |
| `DeduplicationService` | Domain logic — Levenshtein with metadata-aware thresholds |
| `ImportService` | Orchestration — approval workflow, batch management |
| All DAL repositories | Persistence layer |
| All API routes | REST API surface |
| `TransliterationCollapse.kt` | Shared domain logic for matching keys |

---

## 5. Python Ecosystem Assessment

### 5.1 HTML Parsing

| Library | Maturity | Fit |
|:---|:---|:---|
| **BeautifulSoup4** | 20+ years, universal standard | Excellent — handles malformed HTML (blogspot), CSS selectors, DOM traversal. Direct replacement for Jsoup. |
| **lxml** | 15+ years, C-based speed | Excellent — 10-50x faster than BS4 for large documents. Can be used as BS4 backend. |
| **selectolax** | Newer, Modest ecosystem | Good performance but less mature for edge cases. |

**Recommendation**: `beautifulsoup4` with `lxml` as parser backend. This is the exact equivalent of Jsoup in the Python world, with arguably better handling of malformed HTML.

### 5.2 HTTP Client

| Library | Fit |
|:---|:---|
| **httpx** | Already in `pyproject.toml`. Async-capable, HTTP/2 support, excellent timeout handling. Direct replacement for Ktor HTTP client. |

**Recommendation**: Already using `httpx`. No change needed.

### 5.3 Gemini LLM Integration

The current Kotlin implementation is a hand-rolled 470-line HTTP client (`GeminiApiClient.kt`) that:
- Manually constructs JSON request bodies
- Implements custom rate limiting with cooldown multipliers
- Handles 429/503 with exponential backoff
- Parses raw JSON responses
- Sanitises LLM JSON output (trailing commas, markdown blocks)
- Manages schema mode vs prompt-only mode

**Python equivalent**: `google-generativeai` SDK (already in `pyproject.toml`):
```python
import google.generativeai as genai

model = genai.GenerativeModel("gemini-2.0-flash")
response = model.generate_content(
    prompt,
    generation_config=genai.GenerationConfig(
        response_mime_type="application/json",
        response_schema=schema_dict,
    ),
)
result = response.text  # Already parsed, no sanitisation needed
```

**Advantages**:
- SDK handles rate limiting, retries, and 429/503 automatically
- Schema mode is built-in (no manual JSON schema construction)
- Response parsing is handled by the SDK
- ~470 lines of Kotlin replaced by ~20 lines of Python
- The SDK is maintained by Google — stays current with API changes

### 5.4 Indic Text Processing

| Library | Already Used? | Capability |
|:---|:---|:---|
| `indic-transliteration` | Yes (in Python) | Script conversion between Devanagari, Tamil, Telugu, Kannada, Malayalam, IAST, Harvard-Kyoto, SLP1 |
| `regex` (PyPI) | No (uses stdlib `re`) | Unicode-aware regex with `\p{Devanagari}` property support — better than stdlib `re` for Indic script detection |
| `unicodedata` (stdlib) | Available | NFD/NFC normalisation — same as `java.text.Normalizer` |
| `IndicNLP` | Not yet | Sentence tokenisation, script detection — useful but not critical |

**Key point**: The `indic-transliteration` library is Python-native with no JVM equivalent. Currently the Kotlin backend doesn't do script conversion at all — it relies on the Python service for that. Moving HTML extraction to Python means ALL transliteration can use this library directly.

### 5.5 Unicode and Text Processing

Python has stronger text processing primitives for this workload:

| Capability | Python | Kotlin/JVM |
|:---|:---|:---|
| Unicode normalisation | `unicodedata.normalize('NFD', ...)` | `java.text.Normalizer.normalize(NFD)` |
| Regex Unicode properties | `\p{Devanagari}` via `regex` library | `\p{IsDevanagari}` — works but verbose |
| String manipulation | First-class, concise | Verbose, Java-style |
| Script detection | `unicodedata.name()`, `regex` `\p{Script=...}` | Manual Unicode range checks |
| Diacritic stripping | One-liner with NFD + category filter | Same, but more verbose |
| Pattern matching | Structural match statements (3.10+) | `when` expressions — comparable |

For text-heavy, regex-heavy, Unicode-heavy workloads like music lyric extraction, Python is the more natural fit.

---

## 6. Migration Plan

### Phase 1: HTML Fetching + Text Extraction (1-2 days)

**New file**: `tools/pdf-extractor/src/html_extractor.py`

Port `HtmlTextExtractor.kt` logic to Python using BeautifulSoup4:
- Same selector priority: `div.post-body` > `div.post` > `article` > `div.post-content` > `body`
- Same block-element-aware text extraction
- Same `<a>` link annotation
- Same boilerplate removal (script, style, nav, footer)
- Same 120K char limit

**Test**: Extract text from 5 blogspot URLs, compare output with current Kotlin extraction.

### Phase 2: Unified Structure Parser (2-3 days)

**Merge into**: `tools/pdf-extractor/src/structure_parser.py`

Consolidate all 100+ section detection patterns from `KrithiStructureParser.kt` into the Python `StructureParser`:
- Add all Indic script section headers (Tamil, Telugu, Kannada, Malayalam)
- Add single-letter abbreviations (`P`, `A`, `C`, `Ch`)
- Add language header detection (`DEVANAGARI`, `TAMIL`, etc.)
- Add lyric variant extraction logic
- Add Ragamalika sub-section detection
- Add boilerplate filtering

**Shared pattern config** (optional but recommended): Extract section patterns to a YAML file that both languages can reference during the transition period.

**Test**: Run against 20 blogspot pages + 20 PDF pages, verify section detection matches or exceeds current Kotlin output.

### Phase 3: Gemini LLM Integration (1-2 days)

**New file**: `tools/pdf-extractor/src/llm_extractor.py`

Replace the 470-line `GeminiApiClient.kt` + prompt construction with:
- `google-generativeai` SDK for API calls
- Same prompt template (metadata extraction)
- Same schema mode for structured output
- Same deterministic-first/LLM-fallback pattern

**Test**: Extract metadata from 10 blogspot URLs using both Kotlin and Python paths, compare results.

### Phase 4: Worker Queue Extension (1 day)

**Modify**: `tools/pdf-extractor/src/worker.py`

Extend the existing queue polling worker to handle `source_format = 'HTML'` in addition to `'PDF'`:
- When format is PDF: existing flow (PyMuPDF -> segmentation -> structure parsing -> metadata)
- When format is HTML: new flow (httpx fetch -> BS4 extract -> structure parsing -> metadata -> optional Gemini)

Both paths output the same `CanonicalExtractionDto` — the Kotlin consumer doesn't need to change.

### Phase 5: Kotlin Cleanup (1 day)

Remove from Kotlin:
- `WebScrapingService.kt` (460 lines)
- `DeterministicWebScraper.kt` (150 lines)
- `KrithiStructureParser.kt` (535 lines)
- `HtmlTextExtractor.kt` (115 lines)
- `GeminiApiClient.kt` (470 lines)
- `TempleScrapingService.kt` (100 lines)
- `ScrapeJsonSanitizer.kt` (50 lines)
- `ScrapeCache.kt` (~80 lines)
- Related Jsoup and Gemini dependencies from `build.gradle.kts`

**Total Kotlin reduction**: ~1,960 lines removed

Update the import/scraping routes to submit extraction requests to the queue instead of calling `WebScrapingService` directly.

### Phase 6: Docker + Deployment (0.5 day)

- Update `compose.yaml` to add volume mount for `tools/pdf-extractor/src` (development mode)
- Ensure `httpx` and `beautifulsoup4[lxml]` are in `pyproject.toml` (httpx already is)
- Update the `Dockerfile` if new system dependencies are needed (none expected)

---

## 7. Risk Assessment

### 7.1 Low Risk

| Risk | Mitigation |
|:---|:---|
| BS4 can't handle blogspot HTML | BS4 + lxml handles malformed HTML better than Jsoup |
| Python regex can't handle Indic scripts | Python `regex` library has full Unicode property support |
| Gemini SDK missing features | SDK is maintained by Google, covers all current features |
| Performance for HTML extraction | HTML extraction is I/O-bound (network fetch), not CPU-bound. Python is fine. |

### 7.2 Medium Risk

| Risk | Mitigation |
|:---|:---|
| Queue latency for HTML extraction | Currently HTML scraping is synchronous in the Kotlin request path. Moving to queue-based means the caller gets a job ID, not immediate results. May need to adjust import orchestration to poll for completion. |
| Two-service deployment complexity | Already running both services. No new operational burden. |
| Losing the in-memory `ScrapeCache` | Implement Redis or DB-based caching in Python, or accept cache-miss cost (HTTP fetch is ~1s, Gemini is ~5-10s). |

### 7.3 High Risk

| Risk | Mitigation |
|:---|:---|
| Breaking existing bulk import flow | The bulk import currently calls `IWebScraper.scrapeKrithi()` synchronously during import. Moving this to queue-based means rearchitecting the import flow to: (1) submit URLs to extraction queue, (2) wait for results, (3) then process. This is the biggest change. |

**Mitigation for the bulk import flow**: The import service can be modified to:
1. Submit all URLs to the extraction queue as a batch
2. Poll for completion (existing `ExtractionResultProcessor` pattern)
3. Process results through the existing dedup/creation pipeline

This actually **simplifies** the import flow — instead of two code paths (bulk import vs extraction pipeline), there's one: everything goes through the extraction queue.

---

## 8. Benefits Summary

### 8.1 Immediate Benefits

1. **Single source of truth for domain heuristics** — One set of section detection patterns, one metadata parser, one diacritic normaliser. No more divergence.
2. **~1,960 lines of Kotlin removed** — Simpler backend focused on domain, persistence, and API.
3. **Better Gemini integration** — Python SDK replaces 470-line hand-rolled HTTP client.
4. **Access to `indic-transliteration`** — Script conversion available for HTML extraction, not just PDFs.
5. **Unified extraction contract** — Both PDF and HTML produce `CanonicalExtractionDto`. No more `ScrapedKrithiMetadata` vs `CanonicalExtractionDto` duality.

### 8.2 Architectural Benefits

1. **One code path for all imports** — Bulk import, PDF extraction, and HTML extraction all go through extraction_queue. Eliminates the "two parallel systems" problem from the retrospective.
2. **Easier to test** — Python extraction can be tested against real URLs and PDFs independently. No JVM startup required for extraction testing.
3. **Faster iteration on extraction logic** — Python's REPL, Jupyter notebooks, and scripting make it faster to experiment with regex patterns and normalisation rules.
4. **Clear separation of concerns** — Python = extraction (turning raw content into structured data). Kotlin = domain (managing the Krithi lifecycle, persistence, API).

### 8.3 Long-Term Benefits

1. **ML/NLP integration ready** — If future extraction needs ML-based approaches (NER for deity/temple, script classification, layout analysis), Python's ecosystem (spaCy, Hugging Face, etc.) is unmatched.
2. **Docker-native scaling** — The Python extraction service can be scaled horizontally (multiple workers) independently of the Kotlin backend.
3. **Lower cognitive load** — Developers working on extraction logic only need Python. Developers working on domain logic only need Kotlin.

---

## 9. What NOT to Move

The following should remain in Kotlin:

1. **`NameNormalizationService`** — Used at persistence time for matching keys. Moving it would require cross-service calls for every Krithi creation.
2. **`TransliterationCollapse`** — Shared domain module used by DAL. Keep in Kotlin.
3. **`DeduplicationService`** — Needs database access for `findDuplicateCandidates`. Keep in Kotlin.
4. **`ExtractionResultProcessor`** — Orchestration that reads queue results and writes to domain tables. Keep in Kotlin.
5. **`VariantMatchingService`** — Domain logic that needs DB access for Krithi lookup. Keep in Kotlin.

The principle: **extraction** (raw content -> structured DTO) moves to Python. **Resolution** (structured DTO -> domain entities in the database) stays in Kotlin.

---

## 10. Effort Estimate

| Phase | Effort | Dependencies |
|:---|:---|:---|
| Phase 1: HTML fetch + text extraction | 1-2 days | None |
| Phase 2: Unified structure parser | 2-3 days | Phase 1 |
| Phase 3: Gemini LLM integration | 1-2 days | Phase 1 |
| Phase 4: Worker queue extension | 1 day | Phases 1-3 |
| Phase 5: Kotlin cleanup | 1 day | Phase 4 validated |
| Phase 6: Docker + deployment | 0.5 day | Phase 5 |
| **Total** | **6.5-9.5 days** | |

With the retrospective's recommended approach of **vertical slices** and **validation at each step**, the realistic timeline is **~2 weeks** including testing and edge case fixes.

---

## 11. Recommendation

**Proceed with consolidation.** The duplicated heuristics problem is a structural flaw that will keep causing bugs as the extraction logic evolves. Python is the stronger platform for text extraction, Unicode processing, and LLM integration. The `CanonicalExtractionDto` contract already exists as the bridge between the two services.

**Start with Phase 1** (HTML text extraction) as a vertical slice: extract text from one blogspot URL in Python, produce a `CanonicalExtractionDto`, have Kotlin consume it from the queue. Validate end-to-end before proceeding to Phase 2.

Apply the retrospective's key lesson: **one working slice first, then expand.**

---

*Analysis conducted 2026-02-12 by Claude Opus 4.6.*
