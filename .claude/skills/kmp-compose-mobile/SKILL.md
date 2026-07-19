---
name: kmp-compose-mobile
description: Kotlin Multiplatform and Compose Multiplatform conventions for the shared mobile modules. Use when editing modules/shared/domain or modules/shared/presentation, adding KMP targets or source sets, writing @Serializable DTOs, expect/actual declarations, or Compose UI shared across Android and iOS.
---

# KMP & Compose Multiplatform (modules/shared/)

Versions live in [current-versions.md](../../../application_documentation/00-meta/current-versions.md); shared rules in [CLAUDE.md](../../../CLAUDE.md). This skill adds the mobile-layer specifics.

## Module layout

- `modules/shared/domain` — pure Kotlin, `src/commonMain` only. `@Serializable` DTOs, serializers (e.g. `UuidSerializer`), domain support logic. No platform or UI dependencies; it still targets iosX64.
- `modules/shared/presentation` — Compose Multiplatform UI. **Scaffolded but empty: there is no `src/` directory yet** — only `build.gradle.kts`. The source sets below are declared in the build file and will exist once the first file lands; don't expect to find composables here today. Targets: `androidLibrary` (compileSdk 36, minSdk 24), `iosArm64`, `iosSimulatorArm64`. **iosX64 is intentionally dropped** — CMP 1.11.x stopped publishing x64 iOS artifacts; do not re-add it. Source sets: `commonMain` (Compose + domain), `androidMain` (okhttp client, activity-compose), per-target iOS source sets. iOS framework baseName is `presentation`.
- Compose dependencies are explicit catalog entries (`libs.compose.*`) — the `compose.*` plugin accessors are deprecated as of CMP 1.11; material3 rides its own version train.

## expect/actual

Keep `expect` declarations to a minimum — only for genuinely platform-specific behaviour (storage, logging, keychain). Prefer interface-based injection where an interface suffices, because it stays testable from commonTest. When you do use expect/actual, every declared target's source set must supply the `actual`.

## Compose conventions

- Hoist state: composables stay stateless, state lives in a presenter/holder; pass state down, events up.
- Unidirectional data flow: UI renders a single immutable state object, and mutations happen only in the presenter via `copy()` — never by mutating state a composable can reach. Domain and presentation logic live in `commonMain` so they're testable from `commonTest` with no UI or platform harness.
- Pass immutable DTO lists into composables — mutable collections defeat recomposition skipping.
- Load shared assets through the Compose Multiplatform resource API; never reference `R.drawable.*` or iOS bundle paths from `commonMain`.
- Flow collection needs a lifecycle-aware collector, and this module doesn't have one yet: `androidx.lifecycle:lifecycle-runtime-compose` (which supplies `collectAsStateWithLifecycle`) is **not** in `gradle/libs.versions.toml`. Add the catalog entry and the `androidMain` dependency as part of the first change that collects a flow in UI — don't reach for a bare `collectAsState`, which keeps collecting while the screen is backgrounded.

## Kotlin style (this layer)

Immutable-first (`val`, `data class`), expression bodies and `when`/`if` as expressions, scope functions for initialization blocks, `map`/`flatMap` flows over mutable loops — combined with clear class boundaries: UI state models never contain UI logic's dependencies, presenters take use-cases via constructor injection.

## Build

- `./gradlew :modules:shared:domain:build` / `:modules:shared:presentation:build`
- JVM toolchain is 25; all versions come from `gradle/libs.versions.toml` — never hardcode a version in a `build.gradle.kts`.
