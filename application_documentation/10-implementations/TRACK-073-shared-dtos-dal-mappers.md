| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-07 |
| **Author** | Antigravity |

# Refactor Shared DTOs and DAL Mappers

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
