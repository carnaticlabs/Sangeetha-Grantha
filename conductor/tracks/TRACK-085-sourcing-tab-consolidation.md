| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-085 |
| **Title** | Sourcing Tab Consolidation & Import Detail Cleanup |
| **Status** | Completed |
| **Created** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-084 |

# TRACK-085: Sourcing Tab Consolidation & Import Detail Cleanup

## Objective

Reduce the Sourcing section from 6 tabs to 4 by merging related pages, clean up the Bulk Import detail pane (remove hex IDs, format events, hide unused pipeline stages), and conditionally hide empty features across sourcing pages.

## Scope

### Tab Consolidation (6 → 4)
- Overview | Sources & Processing | Evidence & Verification | Collection Health
- New combined pages: SourcesAndProcessingPage, EvidenceAndVerificationPage
- Route updates and redirects in App.tsx
- SourcingLayout nav items update

### Bulk Import Detail Cleanup
- Remove hex batch IDs
- Filter out ENTITY_RESOLUTION pipeline stage when no job exists
- Format events as key-value list instead of raw JSON
- Remove Task ID row from log drawer, format duration

### Conditional Feature Hiding
- Hide Confidence/Reliability columns when no data
- Hide Voting column when no voting status exists
- Hide empty Quality Dashboard sections (Enrichment, Composer Matrix, Audit Results)

## Files Modified

- `src/components/sourcing/SourcingLayout.tsx`
- `src/pages/BulkImport.tsx`
- `src/pages/sourcing/SourcesAndProcessingPage.tsx` (NEW)
- `src/pages/sourcing/EvidenceAndVerificationPage.tsx` (NEW)
- `src/pages/sourcing/ExtractionMonitorPage.tsx`
- `src/pages/sourcing/SourceEvidencePage.tsx`
- `src/pages/sourcing/StructuralVotingPage.tsx`
- `src/pages/sourcing/QualityDashboardPage.tsx`
- `src/App.tsx`
