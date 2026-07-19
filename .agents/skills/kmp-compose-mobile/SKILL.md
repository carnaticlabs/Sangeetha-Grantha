---
name: kmp-compose-mobile
description: Guidelines for Kotlin Multiplatform (KMP) and Compose Multiplatform Mobile Frontend development.
---

# KMP & Compose Multiplatform Mobile Frontend Guidelines

This skill enforces best practices and "gold-standard" architecture when working in the shared multiplatform modules of the Sangeetha Grantha application.

## 1. Programming Paradigm: OOP + Functional Hybrid
To ensure maximum code readability, type safety, and clean architecture, Kotlin code in the mobile layer must adhere to the following design standards:
- **Functional Idioms**: Favor immutability (`val` over `var`). Use scoping functions (`apply`, `let`, `run`, `also`, `with`) appropriately to clean up initialization and transformation blocks. Use expressions (e.g. `when` or `if` as expressions) instead of statements.
- **Object-Oriented Design**: Enforce solid encapsulation, clear class boundaries, interfaces for abstraction, and proper separation of concerns. Do not mix data models with UI logic.
- **Example of Gold-Standard Kotlin**:
  ```kotlin
  data class KrithiUiState(
      val title: String,
      val raga: String,
      val sections: List<SectionDto> = emptyList(),
      val isLoading: Boolean = false
  ) {
      val isPlayable: Boolean get() = sections.isNotEmpty() && !isLoading
  }

  class KrithiPresenter(
      private val fetchKrithiUseCase: FetchKrithiUseCase
  ) {
      fun loadKrithi(id: UUID): Flow<KrithiUiState> =
          fetchKrithiUseCase.execute(id)
              .map { dto -> 
                  KrithiUiState(
                      title = dto.title.uppercase(),
                      raga = dto.ragaName,
                      sections = dto.sections
                  )
              }
              .onStart { emit(KrithiUiState(title = "", raga = "", isLoading = true)) }
  }
  ```

## 2. Multiplatform Code Organization
The multiplatform components reside in `modules/shared/`:
- **`shared/domain/`**: Contains pure domain DTOs, network models, and business logic. It must remain 100% platform-independent and library-minimal. `src/commonMain` only; it still targets iosX64.
- **`shared/presentation/`**: Intended for Compose Multiplatform UI components, themes, and screen definitions — but it is currently **scaffolded and empty: there is no `src/` directory yet**, only `build.gradle.kts`. Don't expect to find composables here today; the source sets below exist in the build file and materialize once the first file lands.
  - Targets: `androidLibrary` (compileSdk 36, minSdk 24), `iosArm64`, `iosSimulatorArm64`. **iosX64 is intentionally dropped** — Compose Multiplatform 1.11.x stopped publishing x64 iOS artifacts; do not re-add it. iOS framework baseName is `presentation`.
  - Compose dependencies are explicit catalog entries (`libs.compose.*`) — the `compose.*` plugin accessors are deprecated as of CMP 1.11; material3 rides its own version train.

## 3. Platform Abstractions (`expect` / `actual`)
- Keep `expect` declarations to an absolute minimum. Use them only when platform-specific behavior (e.g., local storage, platform logging, keychains) is required.
- Prefer interface-based dependency injection over `expect`/`actual` where possible.
- If using `expect`/`actual`, ensure that the signature matches perfectly and that all platform modules (`iosMain`, `androidMain`) implement the `actual` counterpart.

## 4. Compose Multiplatform Best Practices
- **State Hoisting**: Keep composables stateless by hoisting state to the parent or a presenter/ViewModel component. Pass state down and events up.
- **Unidirectional Data Flow**: UI renders a single immutable state object, and mutations happen only in the presenter via `copy()` — never by mutating state a composable can reach. Domain and presentation logic live in `commonMain` so they're testable from `commonTest` with no UI or platform harness.
- **Immutability**: Avoid passing mutable state lists/maps directly. Use immutable DTO lists to prevent unnecessary re-compositions.
- **Lifecycle-Aware State Collection**: Flow collection needs a lifecycle-aware collector, and this module doesn't have one yet — `androidx.lifecycle:lifecycle-runtime-compose` (which supplies `collectAsStateWithLifecycle`) is **not** in `gradle/libs.versions.toml`. Add the catalog entry and the `androidMain` dependency as part of the first change that collects a flow in UI; don't reach for a bare `collectAsState`, which keeps collecting while the screen is backgrounded.

## 5. Multiplatform Resources
- Use the Compose Multiplatform Resource API for loading shared assets (images, fonts, localizable string keys).
- Do not reference Android-specific `R.drawable.*` or iOS-specific bundle resource paths in `commonMain` code.
