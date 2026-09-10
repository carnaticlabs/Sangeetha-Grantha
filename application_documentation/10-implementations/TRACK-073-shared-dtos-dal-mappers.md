| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# Refactor Shared DTOs and DAL Mappers

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).

## Purpose
Split large 300+ line DTO and mapper files into focused domain-specific files to improve maintainability and discoverability.

## Implementation Details
- Split `SourcingDtos.kt` into `SourcingDtos.kt` and `EvidenceVotingDtos.kt` (and others as noted in the track).
- Split `DtoMappers.kt` into `CoreEntityMappers.kt`, `ImportDtoMappers.kt`, and `KrithiDtoMappers.kt`.

## Code Changes
| File | Change |
|------|--------|
| `modules/shared/domain/.../SourcingDtos.kt` | Split |
| `modules/shared/domain/.../EvidenceVotingDtos.kt` | New file |
| `modules/backend/dal/.../DtoMappers.kt` | Deleted |
| `modules/backend/dal/.../CoreEntityMappers.kt` | New file |
| `modules/backend/dal/.../ImportDtoMappers.kt` | New file |
| `modules/backend/dal/.../KrithiDtoMappers.kt` | New file |

Ref: application_documentation/10-implementations/TRACK-073-shared-dtos-dal-mappers.md

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
