| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-24 |
| **Author** | Sangeetha Grantha Team |

# Goal
Batch 3a — Upgrade Kotlin `2.3.0` → `2.4.0` (stable) across the backend, shared KMP domain, and mobile. The Compose compiler plugin is version-locked to Kotlin, so Compose Multiplatform must be validated in the same change.

# Scope (`gradle/libs.versions.toml`)
- **Kotlin** `2.3.0` → `2.4.0` — `kotlin`, all `kotlin*` plugins, and `composeCompiler` (ref'd to `kotlin`).
- **Compose Multiplatform** `1.10.0` → verify the version paired with Kotlin 2.4.0 (check JetBrains compatibility matrix before bumping).
- Re-check coupled libs at Kotlin 2.4: kotlinx-coroutines `1.10.2`, serialization `1.10.0`, Koin `4.x`, Ktor `3.x`.

# Implementation Plan
1. Bump `kotlin` in `libs.versions.toml`; resolve compose compiler / CMP compatibility from the JetBrains matrix.
2. `./gradlew :modules:backend:api:build` — fix any language-level deprecations / warnings (project targets zero warnings, TRACK-099).
3. Build the shared KMP domain + mobile targets; confirm Compose MP compiles.
4. `make test` (backend) + integration tests via Testcontainers.
5. Sync version docs (`current-versions.md`, `tech-stack.md`, `getting-started.md`, plus Memory tech-stack note).
6. Commit per commit-policy.

# Risks
- Widest blast radius of all batches — backend + KMP + mobile + Compose compiler.
- Compose compiler/CMP version must be confirmed against Kotlin 2.4 before bumping; mismatch breaks the build.
- Best run as a dedicated session with full test cycles; not a quick drop-in.
