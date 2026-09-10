| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.5.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Technology stack and ownership

---

This guide explains why each part of the stack exists and where it is configured. [Current Versions](../00-meta/current-versions.md) contains exact dependency pins/resolutions; source manifests remain authoritative when those values change.

| Area | Technology | Responsibility / source |
|:---|:---|:---|
| Shared language/domain | Kotlin Multiplatform, kotlinx.serialization | Shared DTOs and domain types in [shared/domain](../../modules/shared/domain) |
| Mobile UI | Compose Multiplatform | Shared screens/presenters in [presentation](../../modules/shared/presentation) |
| Mobile data | Ktor client and local document storage | V2 networking/repositories in [mobile-data](../../modules/shared/mobile-data) |
| Native hosts | Android app and Swift/iOS host | Platform setup and native tests in [modules/mobile](../../modules/mobile) |
| Backend | Ktor, Koin, coroutines | HTTP, dependencies, orchestration in [backend/api](../../modules/backend/api) |
| Persistence | Exposed DSL, JDBC, HikariCP | Transactional repositories in [backend/dal](../../modules/backend/dal) |
| Database/search | PostgreSQL, pgvector, trigram indexes | Canonical data, queue, history, lexical/vector retrieval |
| **Migrations** | **Flyway Community** | Versioned schema + repeatable reference seeds; Make/Compose and JVM test API |
| Admin web | React, TypeScript, Tailwind, Vite, TanStack Query | Editorial UI and server state in [admin module](../../modules/frontend/sangita-admin-web) |
| Extraction | Python, Pydantic, source parsers, optional Gemini | [Worker](../../tools/krithi-extract-enrich-worker/README.md) |
| Embeddings | Gemini embedding clients + profile-aware index tools | Overview/passage indexing and query vectors |
| Verification | JUnit, Testcontainers, Vitest, Playwright, pytest, Ruff, mypy | [Quality gates](../07-quality/README.md) |
| Local orchestration | Makefile, Docker Compose, mise | Reproducible services and developer commands |
| CI | GitHub Actions | [.github/workflows](../../.github/workflows) |

## Source manifests

- [Gradle version catalog](../../gradle/libs.versions.toml): Kotlin/backend/mobile libraries.
- [Frontend package manifest](../../modules/frontend/sangita-admin-web/package.json): browser dependencies and scripts.
- [Worker pyproject](../../tools/krithi-extract-enrich-worker/pyproject.toml) and [uv.lock](../../tools/krithi-extract-enrich-worker/uv.lock): Python requirements and resolutions.
- [mise](../../.mise.toml): developer tool pins; [Compose](../../compose.yaml): service images and topology.

The worker requires Python 3.14 or newer. Rust is still pinned in mise for archived tooling context but is not part of the active migration workflow. Both the Rust CLI and interim custom Python migration runner are archived under [archive/tools](../../archive/tools).

## Implemented versus proposed

Rasika, public catalogue V1/V2, curator/sourcing, versioned canon, raga identity, and hybrid/semantic search are implemented to the boundaries described in the [feature map](../01-requirements/features/README.md). Native device acceptance, some sourcing responses, later metadata exploration, and production deployment remain incomplete.

AWS/GCP diagrams describe deployment options rather than a running production environment. OAuth/OTP and graph exploration are not dependencies of the current working release.

[System architecture](./backend-system-design.md) · [Flyway decision](./decisions/ADR-013-db-migration-with-flyway.md) · [Onboarding](../00-onboarding/getting-started.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
