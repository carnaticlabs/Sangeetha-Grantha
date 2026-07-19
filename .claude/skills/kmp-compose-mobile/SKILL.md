---
name: kmp-compose-mobile
description: Kotlin Multiplatform and Compose Multiplatform conventions for the shared mobile modules. Use when editing modules/shared/domain or modules/shared/presentation, adding KMP targets or source sets, writing @Serializable DTOs, expect/actual declarations, or Compose UI shared across Android and iOS.
---

# KMP & Compose Multiplatform (modules/shared/)

Versions live in [current-versions.md](../../../application_documentation/00-meta/current-versions.md); shared rules in [CLAUDE.md](../../../CLAUDE.md). This skill adds the mobile-layer specifics.

## Module layout

- `modules/shared/domain` — pure Kotlin, `src/commonMain` only. `@Serializable` DTOs, serializers (e.g. `UuidSerializer`), domain support logic. No platform or UI dependencies; it still targets iosX64.
- `modules/shared/presentation` — Compose Multiplatform UI. Targets: `androidLibrary` (compileSdk 36, minSdk 24), `iosArm64`, `iosSimulatorArm64`. **iosX64 is intentionally dropped** — CMP 1.11.x stopped publishing x64 iOS artifacts; do not re-add it. Source sets: `commonMain` (Compose + domain), `androidMain` (okhttp client, activity-compose), per-target iOS source sets. iOS framework baseName is `presentation`.
- Compose dependencies are explicit catalog entries (`libs.compose.*`) — the `compose.*` plugin accessors are deprecated as of CMP 1.11; material3 rides its own version train.

## expect/actual

Keep `expect` declarations to a minimum — only for genuinely platform-specific behaviour (storage, logging, keychain). Prefer interface-based injection where an interface suffices, because it stays testable from commonTest. When you do use expect/actual, every declared target's source set must supply the `actual`.

## Compose conventions

- Hoist state: composables stay stateless, state lives in a presenter/holder; pass state down, events up.
- Pass immutable DTO lists into composables — mutable collections defeat recomposition skipping.
- Load shared assets through the Compose Multiplatform resource API; never reference `R.drawable.*` or iOS bundle paths from `commonMain`.

## Kotlin style (this layer)

Immutable-first (`val`, `data class`), expression bodies and `when`/`if` as expressions, scope functions for initialization blocks, `map`/`flatMap` flows over mutable loops — combined with clear class boundaries: UI state models never contain UI logic's dependencies, presenters take use-cases via constructor injection.

## Build

- `./gradlew :modules:shared:domain:build` / `:modules:shared:presentation:build`
- JVM toolchain is 25; all versions come from `gradle/libs.versions.toml` — never hardcode a version in a `build.gradle.kts`.
