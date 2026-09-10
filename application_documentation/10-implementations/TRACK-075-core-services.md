| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# Refactor Core Services

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).

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

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
