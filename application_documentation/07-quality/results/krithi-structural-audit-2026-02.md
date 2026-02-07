# Data Quality Audit Results (Interim) - Feb 2026

## 1. Executive Summary
The initial audit phase has identified significant structural inconsistencies in the Krithi repertoire. While the musicological hypothesis states that Krithi structure should be invariant across languages, the database contains variants with widely diverging section counts.

## 2. Key Findings

### Structural Variance
A significant number of Krithis exhibit "Section Count Drift" across language variants. 

| Krithi ID | Title | Language | Section Count |
|:---|:---|:---|:---|
| 87996ef9 | aruNAcala nAtham | sa | 3 or 5 |
| 87996ef9 | aruNAcala nAtham | te | 2 or 4 |
| bd4cf402 | Aryaam Abhayaambaam | sa | 3 or 6 |
| 91079b86 | bAlAmbikayA kaTAkshitOaham | te | 1 or 2 |

### Case Study: bAlAmbikayA kaTAkshitOaham (91079b86)
Analysis of the `krithi_lyric_sections` for this krithi reveals several data quality anti-patterns:

1. **Section Inconsistency**: 
   - Some Malayalam (ml) and Kannada (kn) variants are split into 3 sections (Pallavi, Samashti Charanam 1, Samashti Charanam 2).
   - Other variants for the same languages merge all content into fewer sections.
2. **Metadata Pollution**: The `text` field in some lyric sections contains non-lyric metadata such as "Updated on 16 Dec 2016", "Kannada- Word Division", or translation notes like "tri-kUTa - refer to lalitA sahasra nAma 89".
3. **Redundant Variants**: There are multiple variants for the same language (e.g., 3 English variants, 2 Malayalam) that are nearly identical but have different section mappings.

## 4. Analysis of Authoritative Sources (Feb 2026)
Analysis of PDFs and specialized composer sites (Guruguha, Swathi Thirunal Festival) confirms that:
- **Dikshitar Compositions**: Standardized using technical markers like `(madhyamakālasāhityam)`. Failure to detect these results in Madhyama Kala being incorrectly merged into the preceding Charanam/Anupallavi.
- **Swathi Thirunal**: Highly structured with multiple numbered Charanams.
- **Transliteration Precision**: Sanskrit and Telugu sources provide the most accurate syllabic mapping. Tamil sources often use subscripts (1-4) which our current parser may not fully normalize.

## 5. Proposed Sourcing Hierarchy & Logic
To ensure structural integrity, the `ImportService` should adopt a "Composer-to-Authority" mapping:

| Composer | Primary Authority Source |
|:---|:---|
| Muthuswami Dikshitar | [guruguha.org](https://guruguha.org) |
| Tyagaraja | [thyagarajavaibhavam.blogspot.com](https://thyagarajavaibhavam.blogspot.com) |
| Swathi Thirunal | [swathithirunalfestival.org](https://swathithirunalfestival.org) |
| General / Others | [karnatik.com](https://karnatik.com), [shivkumar.org](https://shivkumar.org) |

### Logic Firm-up (Heuristics):
1. **Structural Voting**: If multiple sources are available, the structure with the **highest section count** (that matches known composer patterns) is chosen as the template.
2. **Technical Labeling**: `TextBlocker.kt` must be extended to recognize `madhyama kala`, `chittaswaram`, and `muktayi swaram` as first-class sections, even when parenthesized.
3. **Ghost Section Detection**: If a lyric variant has a significantly higher line count than its siblings but fewer sections, it is flagged for manual "Section Split" review.

## 6. Remediation Roadmap
1. **Pilot Fix**: Apply normalization logic to high-priority Dikshitar Krithis (e.g., Navavarnas).
2. **Metadata Purge**: Automated script to remove "Updated on...", "Meaning:", and "Word-by-word" blocks from the `text` field.
3. **Re-Extraction**: Re-run `TextBlocker` with the enhanced strategy for variants flagged with low section counts.

