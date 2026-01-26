| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Web Source Detailed Analysis for Krithi Import


---

## 1. Overview

This document provides detailed technical analysis of each web source for Krithi import, including HTML structure, extraction strategies, and source-specific challenges.

---

## 2. Karnatik.com (https://karnatik.com/lyrics.shtml)

### 2.1 Site Structure

**Organization:**
- Main lyrics page: `/lyrics.shtml`
- Individual composition pages: `/c/` prefix (e.g., `/c1234.shtml`)
- Composer-based organization
- Alphabetical listings

**URL Patterns:**
- List pages: `https://karnatik.com/lyrics.shtml?composer=...`
- Individual compositions: `https://karnatik.com/c/[number].shtml`
- Search functionality available

### 2.2 HTML Structure Analysis

**Typical Composition Page Structure:**
```html
<div class="composition">
  <h1>Composition Title</h1>
  <div class="metadata">
    <span>Raga: [Raga Name]</span>
    <span>Tala: [Tala Name]</span>
    <span>Composer: [Composer Name]</span>
  </div>
  <div class="lyrics">
    <h2>Pallavi</h2>
    <p>Lyric text...</p>
    <h2>Anupallavi</h2>
    <p>Lyric text...</p>
    <h2>Charanam</h2>
    <p>Lyric text...</p>
  </div>
</div>
```

**Key Characteristics:**
- Consistent HTML structure
- Clear section headers
- Metadata in predictable locations
- Multiple script support (Devanagari, Tamil, Telugu, etc.)

### 2.3 Extraction Strategy

**Step 1: Discovery**
- Parse main lyrics page for composition links
- Follow composer-based listings
- Extract all `/c/[number].shtml` URLs

**Step 2: Scraping**
- Fetch individual composition pages
- Respect rate limits (robots.txt check)
- Handle pagination if present

**Step 3: Extraction**
- **Title**: `<h1>` or title metadata
- **Composer**: Metadata section or URL parameter
- **Raga**: Metadata section
- **Tala**: Metadata section
- **Sections**: Parse `<h2>` headers and following `<p>` tags
- **Language/Script**: Detect from content or metadata

**Step 4: Normalization**
- Clean HTML tags
- Preserve line breaks within sections
- Normalize section headers

### 2.4 Challenges & Solutions

| Challenge | Solution |
|:---|:---|
| **Rate Limiting** | Implement exponential backoff, respect robots.txt |
| **Pagination** | Follow "next" links, track visited URLs |
| **Multiple Scripts** | Detect script, create separate lyric variants |
| **Metadata Variations** | Use AI extraction for edge cases |
| **Broken Links** | Skip and log, continue processing |

### 2.5 Data Quality Assessment

- **Completeness**: 95%+ (most compositions have full metadata)
- **Accuracy**: 98%+ (professionally curated)
- **Consistency**: 98%+ (very structured)
- **Coverage**: ~10,000+ compositions

**Recommended Priority**: ⭐⭐⭐⭐⭐ (Highest - Start here)

---

## 3. Guru Guha Blog (https://guru-guha.blogspot.com/)

### 3.1 Site Structure

**Organization:**
- Blogspot-based blog
- Posts organized by date
- Tag-based categorization
- Search functionality

**URL Patterns:**
- Main blog: `https://guru-guha.blogspot.com/`
- Individual posts: `https://guru-guha.blogspot.com/[year]/[month]/[slug].html`
- Archive pages: `https://guru-guha.blogspot.com/[year]/[month]/`

### 3.2 HTML Structure Analysis

**Typical Blog Post Structure:**
```html
<article class="post">
  <header>
    <h1 class="post-title">Post Title</h1>
    <div class="post-meta">Date, Author</div>
  </header>
  <div class="post-body">
    <!-- Mixed content: explanations, lyrics, notation -->
    <p>Introduction text...</p>
    <div class="lyrics-section">
      <h3>Pallavi</h3>
      <p>Lyric text mixed with commentary...</p>
    </div>
    <!-- More mixed content -->
  </div>
</article>
```

**Key Characteristics:**
- Variable structure (varies by post)
- Mixed content (lyrics + commentary)
- May include notation or audio references
- Less structured metadata

### 3.3 Extraction Strategy

**Step 1: Discovery**
- Parse blog archive pages
- Extract post URLs from archive
- Use blog RSS feed if available
- Follow tag-based listings

**Step 2: Scraping**
- Fetch individual post pages
- Extract main content area
- Remove navigation, ads, sidebars

**Step 3: Extraction (AI-Powered)**
- **Challenge**: Distinguish lyrics from commentary
- **Solution**: Use Gemini to identify and extract structured lyrics
- **Metadata**: Extract from post title, tags, or content
- **Sections**: AI identifies section boundaries
- **Composer/Raga**: Extract from text or tags

**Step 4: Normalization**
- Remove commentary and explanations
- Extract pure lyrics
- Normalize section structure
- Handle mixed scripts

### 3.4 Challenges & Solutions

| Challenge | Solution |
|:---|:---|
| **Mixed Content** | AI-powered extraction to separate lyrics from commentary |
| **Variable Structure** | Flexible parsing, AI fallback |
| **Metadata Extraction** | AI extraction from post content and tags |
| **Section Identification** | AI identifies implicit section boundaries |
| **Blogspot Limitations** | Handle platform-specific quirks |

### 3.5 Data Quality Assessment

- **Completeness**: 70-80% (varies by post)
- **Accuracy**: 85-90% (good content, less structured)
- **Consistency**: 60-70% (highly variable)
- **Coverage**: ~500-1,000 posts

**Recommended Priority**: ⭐⭐⭐ (Medium - After Karnatik)

---

## 4. Syama Krishna Vaibhavam Blog

### 4.1 Site Structure

**Organization:**
- Blogspot blog focused on Syama Krishna compositions
- Alphabetical list page
- Links to individual compositions
- May link to external sources

**URL Patterns:**
- List page: `https://syamakrishnavaibhavam.blogspot.com/2011/03/alphabetica-list-of-kritis.html`
- Individual posts: Various patterns
- External links: May point to other sites

### 4.2 HTML Structure Analysis

**List Page Structure:**
```html
<div class="post-body">
  <h2>A</h2>
  <ul>
    <li><a href="...">Composition Title 1</a></li>
    <li><a href="...">Composition Title 2</a></li>
    <!-- ... -->
  </ul>
  <h2>B</h2>
  <!-- ... -->
</div>
```

**Individual Post Structure:**
- Variable (may be full lyrics or just title)
- May redirect to external sources
- Older content (2011) - link rot possible

### 4.3 Extraction Strategy

**Step 1: Discovery**
- Parse alphabetical list page
- Extract all composition links
- Validate links (check for 404s)

**Step 2: Scraping**
- Follow links to individual posts
- Handle external redirects
- Skip broken links

**Step 3: Extraction**
- **If full content**: Extract lyrics and metadata
- **If title only**: Use as discovery, cross-reference with Karnatik
- **If external link**: Follow if same domain, otherwise skip

**Step 4: Cross-Reference**
- Match titles with Karnatik.com
- Use as supplementary source
- Merge metadata if available

### 4.4 Challenges & Solutions

| Challenge | Solution |
|:---|:---|
| **Link Rot** | Validate links, skip broken ones, log for manual review |
| **Title-Only Entries** | Use for discovery, cross-reference with other sources |
| **External Links** | Follow if same domain, otherwise use as reference only |
| **Older Content** | Expect some data quality issues, prioritize newer sources |
| **Alphabetical Organization** | Parse list structure, extract all links |

### 4.5 Data Quality Assessment

- **Completeness**: 50-60% (many title-only entries)
- **Accuracy**: 80-85% (good for discovery)
- **Consistency**: 50-60% (highly variable)
- **Coverage**: ~1,000+ compositions (many may be duplicates)

**Recommended Priority**: ⭐⭐ (Low - Use for discovery, cross-reference)

---

## 5. Dikshitar Kritis List (Guru Guha Blog)

### 5.1 Site Structure

**Organization:**
- Part of Guru Guha blog (guru-guha.blogspot.com)
- Focused on Muthuswami Dikshitar compositions
- Alphabetical organization
- Older content (2009)
- **Critical for Trinity composer coverage** (Dikshitar is one of the Trinity)

**URL Patterns:**
- List page: `https://guru-guha.blogspot.com/2009/04/dikshitar-kritis-alphabetical-list.html`
- Individual posts: May link to other Guru Guha posts or external sources
- Same domain as main Guru Guha blog

### 5.2 HTML Structure Analysis

**Similar to other alphabetical list sources:**
```html
<div class="post-body">
  <h2>A</h2>
  <ul>
    <li><a href="...">Composition Title 1</a></li>
    <li><a href="...">Composition Title 2</a></li>
    <!-- ... -->
  </ul>
  <h2>B</h2>
  <!-- ... -->
</div>
```

**Key Characteristics:**
- Alphabetical list structure
- Links to individual compositions
- May have full content or just titles
- Links may point to other Guru Guha posts or external sources
- Same blogspot structure as other list sources

### 5.3 Extraction Strategy

**Step 1: Discovery**
- Parse alphabetical list page
- Extract all composition links
- Validate links (check for 404s)
- Note: Links may point to other Guru Guha posts

**Step 2: Scraping**
- Follow links to individual posts
- Handle both internal (Guru Guha) and external redirects
- Skip broken links
- Leverage existing Guru Guha blog handler if available

**Step 3: Extraction**
- **If full content**: Extract lyrics and metadata
- **If title only**: Use as discovery, cross-reference with Karnatik
- **If external link**: Follow if same domain, otherwise skip
- **Composer**: Always Muthuswami Dikshitar (known from source)

**Step 4: Cross-Reference**
- Match titles with Karnatik.com
- Use as supplementary source
- Merge metadata if available
- Important for Trinity composer completeness

### 5.4 Challenges & Solutions

| Challenge | Solution |
|:---|:---|
| **Link Rot** | Validate links, skip broken ones, log for manual review |
| **Title-Only Entries** | Use for discovery, cross-reference with other sources |
| **External Links** | Follow if same domain (Guru Guha), otherwise use as reference only |
| **Older Content** | Expect some data quality issues, prioritize newer sources |
| **Alphabetical Organization** | Parse list structure, extract all links |
| **Same Domain as Main Blog** | Reuse Guru Guha blog handler, differentiate by URL pattern |

### 5.5 Data Quality Assessment

- **Completeness**: 50-60% (many title-only entries)
- **Accuracy**: 80-85% (good for discovery)
- **Consistency**: 50-60% (highly variable)
- **Coverage**: ~500+ compositions (Dikshitar-specific)
- **Composer Coverage**: Critical for Trinity completeness

**Recommended Priority**: ⭐⭐⭐ (Medium - Important for Trinity composer coverage)

**Note**: This source is particularly valuable because:
1. Dikshitar is one of the Trinity composers (Tyagaraja, Dikshitar, Shyama Shastri)
2. Provides composer-specific discovery
3. Complements other Trinity sources (Thyagaraja Vaibhavam)
4. May have compositions not in Karnatik

---

## 6. Thyagaraja Vaibhavam Blog

### 6.1 Site Structure

**Organization:**
- Similar to Syama Krishna Vaibhavam
- Focused on Tyagaraja compositions
- Alphabetical organization
- Older content (2009)

**URL Patterns:**
- List page: `https://thyagaraja-vaibhavam.blogspot.com/2009/03/tyagaraja-kritis-alphabetical-list.html`
- Individual posts: Various patterns

### 6.2 HTML Structure Analysis

**Similar to Syama Krishna Vaibhavam:**
- Alphabetical list structure
- Links to individual compositions
- May have full content or just titles
- External links possible

### 6.3 Extraction Strategy

**Similar to Syama Krishna Vaibhavam:**
- Parse alphabetical list
- Extract composition links
- Handle link validation
- Cross-reference with Karnatik
- Use as supplementary source

### 6.4 Challenges & Solutions

**Same as Syama Krishna Vaibhavam**, plus:
- **Tyagaraja Focus**: Good for composer-specific discovery
- **Older Content**: Higher risk of link rot
- **Volume**: ~700+ compositions (smaller than Syama Krishna)

### 6.5 Data Quality Assessment

- **Completeness**: 50-60%
- **Accuracy**: 80-85%
- **Consistency**: 50-60%
- **Coverage**: ~700+ compositions

**Recommended Priority**: ⭐⭐ (Low - Use for Tyagaraja-specific discovery)

---

## 7. TempleNet (http://templenet.com/)

### 6.1 Site Structure

**Organization:**
- Comprehensive temple database
- Geographic organization (state, district)
- Deity-based organization
- Individual temple pages with detailed information

**URL Patterns:**
- Main page: `http://templenet.com/`
- State pages: `http://templenet.com/[state]/`
- Temple pages: `http://templenet.com/[state]/[temple].html`
- Deity pages: Various patterns

### 6.2 HTML Structure Analysis

**Typical Temple Page Structure:**
```html
<div class="temple-info">
  <h1>Temple Name</h1>
  <div class="location">
    <span>City: [City]</span>
    <span>State: [State]</span>
    <span>District: [District]</span>
  </div>
  <div class="deity">
    <h2>Primary Deity</h2>
    <p>Deity name and details...</p>
  </div>
  <div class="history">
    <h2>History</h2>
    <p>Historical information...</p>
  </div>
  <div class="names">
    <h2>Other Names</h2>
    <ul>
      <li>Alternative name 1</li>
      <li>Alternative name 2</li>
    </ul>
  </div>
</div>
```

**Key Characteristics:**
- Well-structured temple information
- Geographic metadata
- Deity associations
- Multilingual names
- Historical context

### 6.3 Extraction Strategy

**Step 1: Discovery**
- Parse main navigation for state listings
- Extract all temple page URLs
- Follow deity-based listings

**Step 2: Scraping**
- Fetch individual temple pages
- Extract structured information
- Handle geographic hierarchy

**Step 3: Extraction**
- **Temple Name**: Primary name and aliases
- **Location**: City, state, district, country
- **Deity**: Primary deity and associated deities
- **Coordinates**: Latitude/longitude if available
- **Names**: Multilingual names and aliases

**Step 4: Normalization**
- Create `Temple` entities
- Create `TempleName` entries for aliases
- Link to `Deity` entities
- Store geographic data

### 6.4 Integration with Krithi Import

**Matching Strategy:**
1. Extract temple names from Krithi lyrics/title
2. Match against TempleNet database
3. Use fuzzy matching for name variations
4. Consider deity context for disambiguation
5. Geographic hints if available

**Example:**
- Krithi mentions "Meenakshi"
- Match to "Meenakshi Amman Temple, Madurai"
- Use deity context (Meenakshi = Parvati)
- Geographic context if available (Madurai)

### 6.5 Challenges & Solutions

| Challenge | Solution |
|:---|:---|
| **Name Variations** | Fuzzy matching, multilingual name support |
| **Geographic Disambiguation** | Use location context from lyrics if available |
| **Deity Associations** | Match by deity name, handle multiple deities |
| **TempleNet Availability** | Pre-fetch and cache, fallback to manual matching |
| **Volume** | Batch processing, incremental updates |

### 6.6 Data Quality Assessment

- **Completeness**: 90%+ (comprehensive database)
- **Accuracy**: 95%+ (authoritative source)
- **Consistency**: 95%+ (well-structured)
- **Coverage**: ~1,000+ temples

**Recommended Priority**: ⭐⭐⭐⭐ (High - Essential for temple associations)

---

## 7. Cross-Source Comparison

### 7.1 Source Priority Matrix

| Source | Data Quality | Volume | Structure | Priority | Effort |
|:---|:---|:---|:---|:---|:---|
| **Karnatik.com** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **1st** | Low |
| **TempleNet** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **2nd** | Medium |
| **Guru Guha** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | **3rd** | High |
| **Dikshitar List** | ⭐⭐ | ⭐⭐ | ⭐⭐ | **4th** | Medium |
| **Syama Krishna** | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ | **5th** | Medium |
| **Thyagaraja** | ⭐⭐ | ⭐⭐ | ⭐⭐ | **6th** | Medium |

### 7.2 Recommended Import Order

1. **Phase 1**: Karnatik.com (foundation, highest quality)
2. **Phase 2**: TempleNet (essential for temple associations)
3. **Phase 3**: Guru Guha (supplementary, rich content)
4. **Phase 4**: Dikshitar List (Trinity composer coverage - important for completeness)
5. **Phase 5**: Syama Krishna & Thyagaraja (discovery, cross-reference)

**Note**: Dikshitar List is prioritized over other blog lists because:
- Dikshitar is a Trinity composer (critical for comprehensive coverage)
- Complements Thyagaraja source for Trinity completeness
- May have unique compositions not in Karnatik

### 7.3 Cross-Source Deduplication Strategy

**Matching Criteria:**
1. **Strong Match**: Same composer + same title + same incipit
2. **Medium Match**: Same composer + same title (verify incipit)
3. **Weak Match**: Same title + similar incipit (may be different compositions)
4. **Discovery Match**: Title-only match (use for discovery, verify with primary source)

**Priority for Conflicts:**
1. Karnatik.com (highest authority)
2. Guru Guha (good supplementary)
3. Dikshitar List (Trinity composer - use for discovery and completeness)
4. Other blog sources (use for discovery only)

---

## 8. Technical Implementation Notes

### 8.1 Scraping Best Practices

**Rate Limiting:**
- Respect `robots.txt` files
- Implement exponential backoff
- Use delays between requests (1-2 seconds)
- Batch processing to avoid overwhelming servers

**Error Handling:**
- Retry logic for transient failures
- Skip and log permanent failures (404s, etc.)
- Continue processing on individual failures
- Comprehensive error logging

**Caching:**
- Cache HTML content for retry scenarios
- Store extracted data to avoid re-scraping
- Version source handlers for structure changes

### 8.2 Source Handler Architecture

```kotlin
interface SourceHandler {
    suspend fun discoverUrls(): List<String>
    suspend fun scrape(url: String): RawContent
    suspend fun extract(content: RawContent): ExtractedMetadata
    fun getSourceId(): UUID
}

class KarnatikSourceHandler : SourceHandler {
    // Karnatik-specific implementation
}

class BlogspotSourceHandler : SourceHandler {
    // Generic Blogspot handler, configurable per blog
}
```

### 8.3 AI Extraction Prompts

**For Structured Sources (Karnatik):**
- Minimal AI needed
- Use for edge cases only
- Focus on validation

**For Unstructured Sources (Blogs):**
- Heavy AI usage required
- Extract lyrics from mixed content
- Identify section boundaries
- Extract metadata from text

**Prompt Template:**
```
Extract the following from this HTML content:
1. Composition title
2. Composer name
3. Raga name
4. Tala name (if mentioned)
5. Deity name (if mentioned)
6. Temple/Kshetra name (if mentioned)
7. Language and script
8. Sections (Pallavi, Anupallavi, Charanam) with text
9. Any notation (separate from lyrics)

Return as structured JSON matching this schema: [schema]
```

---

## 9. Data Quality Metrics by Source

### 9.1 Expected Extraction Accuracy

| Source | Title | Composer | Raga | Tala | Deity | Temple | Sections | Overall |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| **Karnatik** | 99% | 99% | 99% | 95% | 80% | 60% | 98% | **95%** |
| **Guru Guha** | 90% | 85% | 80% | 70% | 70% | 50% | 75% | **75%** |
| **Dikshitar List** | 85% | 95% | 70% | 60% | 60% | 40% | 50% | **65%** |
| **Syama Krishna** | 85% | 90% | 70% | 60% | 60% | 40% | 50% | **65%** |
| **Thyagaraja** | 85% | 95% | 70% | 60% | 60% | 40% | 50% | **65%** |
| **TempleNet** | N/A | N/A | N/A | N/A | 95% | 98% | N/A | **96%** |

### 9.2 Human Review Requirements

**Automatic Approval Threshold:**
- Karnatik: 95%+ confidence → Auto-approve
- Guru Guha: 90%+ confidence → Auto-approve
- Dikshitar List: 85%+ confidence → Auto-approve (composer known, similar to other lists)
- Other blog sources: 85%+ confidence → Auto-approve
- Below thresholds → Human review required

**Review Time Estimates:**
- Karnatik: <30 seconds per import (mostly verification)
- Guru Guha: 2-3 minutes per import (more validation needed)
- Dikshitar List: 2-3 minutes per import (composer known, but may need validation)
- Other blog sources: 3-5 minutes per import (significant validation)

---

## 10. Recommendations

### 10.1 Immediate Actions

1. **Start with Karnatik.com**: Highest ROI, lowest risk
2. **Build Generic Scraper**: Reusable for other sources
3. **Implement Source Handlers**: Modular architecture
4. **Add Comprehensive Logging**: Track extraction accuracy

### 10.2 Medium-Term

1. **TempleNet Integration**: Essential for temple associations
2. **Dikshitar List Handler**: Important for Trinity composer coverage
3. **Blog Source Handlers**: More complex but valuable
4. **Cross-Source Deduplication**: Critical for quality
5. **Quality Metrics Dashboard**: Monitor extraction accuracy

### 10.3 Long-Term

1. **Continuous Import**: Scheduled updates
2. **Source Monitoring**: Detect structure changes
3. **Automated Quality Checks**: Reduce human review
4. **Community Contributions**: Allow manual corrections

---

## 11. Conclusion

The web sources vary significantly in structure and quality. A **tiered approach** is recommended:

1. **Tier 1 (Karnatik, TempleNet)**: High-quality, structured sources - prioritize
2. **Tier 2 (Guru Guha)**: Good content, requires AI extraction - secondary priority
3. **Tier 3 (Dikshitar List)**: Trinity composer source - important for completeness, medium priority
4. **Tier 4 (Other Blog Lists)**: Discovery sources, cross-reference with Tier 1 - lower priority

**Note**: Dikshitar List is elevated to Tier 3 (separate from other blog lists) because:
- Dikshitar is a Trinity composer (critical for comprehensive coverage)
- Provides composer-specific discovery for important composer
- May have unique compositions not in Karnatik

**Key Success Factors:**
- Modular source handler architecture
- AI-powered extraction for unstructured sources
- Comprehensive error handling and logging
- Human review workflow for quality assurance
- Cross-source deduplication

---

## 12. References

- [Krithi Bulk Import Capability Analysis](../archive/krithi-bulk-import-capability-analysis-goose.md)
- [Generic Scraping Feature](../../generic-scraping.md)
- [Intelligent Content Ingestion](../../intelligent-content-ingestion.md)
