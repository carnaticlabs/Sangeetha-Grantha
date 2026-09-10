| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# UI-to-API map

---

This map connects implemented user journeys to route families. It is not a substitute for request DTOs or a count of all available endpoints. See [API contract](./api-contract.md) for exact boundaries.

| Surface / journey | API family | Notes |
|:---|:---|:---|
| Rasika Home | `/v2/catalogue/discovery` | Search entry remains independent of discovery success |
| Rasika Explore compositions | `/v2/catalogue/krithis` | Query, composer/raga UUID filters, zero-based paging |
| Rasika raga/composer directories | `/v2/catalogue/ragas`, `/composers` | Directory query/paging; detail by ID |
| Rasika reader | `/v2/catalogue/krithis/{id}` and `/lyrics/{variantId}` | Metadata followed by one stored variant |
| Rasika Library/Settings | Local repositories/store | No public account or sync endpoint |
| Console login | `/v1/auth/token`, `/v1/auth/refresh` | Stored role claims |
| Console lexical list/reference selectors | Legacy `/v1/krithis/search` and reference reads | Different DTOs from Rasika |
| Console Hybrid/Semantic | `/v1/search/hybrid`, `/semantic` | Ranked overview/passage matches |
| Console composition editor | `/v1/admin/krithis`, `/v1/admin/variants`, notation routes | Metadata, structure, variants, notation and related reads |
| Console imports/review | `/v1/admin/imports` | Source requests, review, reingest, validation |
| Console bulk import | `/v1/admin/bulk-import` | Upload, batches, jobs/tasks/events, controls/export |
| Console sources/processing | `/v1/admin/sourcing/sources`, `/extractions` | Source and run detail |
| Console evidence/verification | `/v1/admin/sourcing/evidence`, `/voting`, `/variants` | Source evidence, structural decisions, variant matching |
| Console curator issues | `/v1/admin/curator` | Section issues and raga-resolution queue |
| Console quality | `/v1/admin/sourcing/quality`, dedicated `/v1/admin/quality` routes | Some sourcing responses are placeholders; dedicated audits are distinct |
| Console reference management | `/v1/admin/composers`, `/ragas`, `/talas`, `/deities`, `/temples`, `/tags` | Per-entity contracts |

UI routes and API routes are different namespaces. The [console screen guide](../05-frontend/admin-web/ui-specs.md) documents redirects from older review/extraction/voting list pages. [App.tsx](../../modules/frontend/sangita-admin-web/src/App.tsx) and [Routing.kt](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Routing.kt) are the executable maps.

Backend user/role routes do not mean the placeholder browser pages are complete. V2 schemas for later metadata directories do not mean those routes are mounted.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
