| Metadata | Value |
|:---|:---|
| **Status** | Archived |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-03 |
| **Author** | Sangeetha Grantha Team |

# Update Summary: Guru-Guha Blogspot Source Correction


---


## Date: 2026-01-19

## Changes Made

### Source Characterization Update

**Previous Understanding:**
- guru-guha.blogspot.com was characterized as unstructured blog posts about Murugan/Subramanya compositions
- Assumed it would require AI/LLM extraction due to prose format
- Estimated 300-800 krithis focused on Murugan deity

**Corrected Understanding:**
- guru-guha.blogspot.com hosts **alphabetical lists** for specific composers
- Specific URL identified: `https://guru-guha.blogspot.com/2009/04/dikshitar-kritis-alphabetical-list.html`
- **Focus: Muthuswami Dikshitar compositions** (not Murugan-specific)
- Structure: List-based format similar to Thyagaraja and Syama Krishna blogspot sources
- Can use **regex pattern matching** instead of AI extraction
- Estimated 300-700 Dikshitar krithis

### Technical Implementation Impact

**Scraper Implementation:**
- Changed from `GeminiService`-based AI extraction to pattern matching
- Removed unstructured blog post parsing logic
- Added list parsing with multiple regex patterns:
  - Pattern 1: `Name - Raga` format
  - Pattern 2: `Name (Raga)` format
- Composer is **implicit** (determined from URL/page title)
- Returns batch entries rather than single krithi per page

**Configuration Changes:**
```kotlin
// OLD
extractionStrategy = ExtractionStrategy.AI_EXTRACTION
requiresAIExtraction = true
implicitDeity = "Murugan"

// NEW
extractionStrategy = ExtractionStrategy.PATTERN_MATCHING
requiresAIExtraction = false
implicitComposer = "Muthuswami Dikshitar"
```

### Document Updates Made

1. **Data Source Comparison Matrix** (Section 2.1)
   - Updated structure: "List-based" (was "Unstructured blog posts")
   - Updated challenge: "Composer implicit (Dikshitar), parsing list format"
   - Updated volume: 300-700 krithis

2. **Scraper Implementation** (Section 7.5)
   - Complete rewrite of `GuruGuhaBlogScraper` class
   - Removed Gemini/AI extraction logic
   - Added `parseListEntries()` method with regex patterns
   - Added `extractComposerFromTitle()` helper

3. **Koog Integration Strategy** (Section 8)
   - Updated use cases: removed "unstructured blog extraction"
   - Changed to "semi-structured list extraction" (if needed)
   - Reduced emphasis on AI extraction for this source

4. **Configuration Examples** (Appendix A)
   - Updated `guruGuhaBlogConfig` with correct parameters

## Implications

### Positive Impacts:
✅ **Simpler implementation** - no AI extraction needed
✅ **Lower costs** - no LLM API calls for this source
✅ **Faster processing** - regex is faster than AI inference
✅ **More reliable** - deterministic parsing vs probabilistic AI
✅ **Consistency** - all three blogspot sources now use similar list-based approach

### Considerations:
⚠️ **May have other composer lists** - the blog might have additional pages for other composers
⚠️ **Pattern variations** - list format may vary between pages
⚠️ **Less metadata** - no lyrics, deity, or temple information in list format

## Recommendations

1. **Discovery Phase**: Crawl guru-guha.blogspot.com to find other composer-specific list pages
2. **Pattern Testing**: Test regex patterns on actual page content to ensure accurate parsing
3. **Fallback Strategy**: If list parsing fails, can still fall back to AI extraction for specific pages
4. **Volume Validation**: Verify actual number of Dikshitar krithis on the page

## Next Steps

- Implement URL discovery to find additional composer lists on guru-guha.blogspot.com
- Test regex patterns against actual HTML
- Update entity resolution to handle "Dikshitar" → "Muthuswami Dikshitar" mapping
- Consider this source as Medium priority (after karnatik.com, similar to other blogspot sources)
