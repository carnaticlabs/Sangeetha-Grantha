| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Comprehensive Remediation & Sourcing Logic Plan (Feb 2026)

This document outlines the specific checklist and logic for correcting the data quality issues identified in the [Structural Consistency Audit](./results/krithi-structural-audit-2026-02.md).

## 1. Governance & Tracks
- **[TRACK-040](../../conductor/tracks/TRACK-040-krithi-remediation-deduplication.md)**: Physical cleanup of the existing database.
- **[TRACK-041](../../conductor/tracks/TRACK-041-enhanced-sourcing-logic.md)**: Logical hardening of the `ImportService` and `KrithiStructureParser`.

## 2. Technical Checklist

### Remediation (Cleanup)
- [ ] **Near-Match Deduplication**:
    - Identify `krithi_lyric_variants` for the same `krithi_id` and `language`.
    - Compare text using Levenshtein distance or Jaccard similarity.
    - If similarity > 0.95, merge variants and re-map `krithi_lyric_sections`.
- [ ] **Structural Alignment**:
    - Select "Canonical Variant" (highest section count + composer match).
    - Map "Orphaned" variants to the canonical section IDs.
- [ ] **Metadata Stripping**:
    - Clean `krithi_lyric_sections.text` of technical noise:
        - `(?i)^Updated on.*`
        - `(?i)^Meaning:.*`
        - `(?i)^Notes:.*`
        - `(?i)^Kannada - .*`

### Sourcing Logic (Hardening)
- [x] **Authority Source Mapping**:
    - `Muthuswami Dikshitar` -> `guruguha.org` (highest priority for structure).
    - `Tyagaraja` -> `thyagarajavaibhavam.blogspot.com`.
    - `Swathi Thirunal` -> `swathithirunalfestival.org`.
- [x] **"Structural Voting" Engine**:
    - When ingesting a Krithi from multiple URLs, the parser must score each source's structure.
    - Score = (Count of detected standard sections) + (Presence of technical markers like Madhyama Kala).
- [x] **Parser Enhancements (`KrithiStructureParser.kt`)**:
    - Expand regex for `MADHYAMAKALA` to include parenthesized forms: `\((?:madhyama\s+kAla|m\.k)\)`.
    - Support Tamil subscript normalization (e.g., `க₁` -> `க`).

## 3. Implementation Roadmap

### Phase 1: Automated Cleanup (TRACK-040)
- Goal: Reduce database noise by 80%.
- Deliverable: Migration script or Admin tool for bulk deduplication.

### Phase 2: Logic Hardening (TRACK-041)
- Goal: Prevent future drift.
- Deliverable: PR updating `ImportService` and `KrithiStructureParser`.

## 4. Acceptance Criteria
- [ ] No Krithi has variants with mismatching section counts (exception: ragamalikas with partial script availability).
- [ ] All Dikshitar Krithis correctly preserve `MADHYAMA_KALA` as a distinct section.
- [ ] Duplicate English variants with word-division notes are merged or archived.