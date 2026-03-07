| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-07 |
| **Author** | Antigravity |

# Refactor Core Services

## Purpose
Split large core orchestration services into sub-300-line domain specific modules: `ImportService`, `ExtractionResultProcessor`, etc.

## Implementation Details
- Extracted `ImportReportGenerator`, `LyricVariantPersistenceService`, `VariantScorer`, `StructuralVotingProcessor`, and `KrithiMatcherService`.
- Reorganized `ReferenceDataService` and added `AuditDataModels`, `AuditSqlQueries`.
- Updated several corresponding dal repositories.
- Tests separated and refactored respectively.

## Code Changes
| File | Change |
|------|--------|
| `modules/backend/api/.../services/*.kt` | Extracted and Split |
| `modules/backend/dal/.../repositories/*.kt` | Modified to accommodate split |
| `modules/backend/api/.../tests/*.kt` | Modified |

Ref: application_documentation/10-implementations/TRACK-075-core-services.md
