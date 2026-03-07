| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-07 |
| **Author** | Sangeetha Grantha Team |

# Refactor Infrastructure Services

## Purpose
Split large infrastructure and parsing classes: `GeminiApiClient`, `WebScrapingService`, `KrithiStructureParser`, and `AuditRunnerService`.

## Implementation Details
- Extracted `GeminiModels`, `GeminiRetryStrategy`.
- Extracted `ScrapingPromptBuilder` and `SectionHeaderDetector`.
- Updated test files including deletion of obsolete `TempleScrapingServiceTest`.

## Code Changes
| File | Change |
|------|--------|
| `modules/backend/api/.../clients/*.kt` | Extracted Gemini properties |
| `modules/backend/api/.../services/ScrapingPromptBuilder.kt` | New extraction |
| `modules/backend/api/.../services/scraping/SectionHeaderDetector.kt` | New extraction |
| `modules/backend/api/.../tests/*.kt` | Modified and deleted old tests |

Ref: application_documentation/10-implementations/TRACK-076-infrastructure-services.md
