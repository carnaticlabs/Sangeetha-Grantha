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
- **`shared/domain/`**: Contains pure domain DTOs, network models, and business logic. It must remain 100% platform-independent and library-minimal.
- **`shared/presentation/`**: Contains Compose Multiplatform UI components, themes, and screen definitions.

## 3. Platform Abstractions (`expect` / `actual`)
- Keep `expect` declarations to an absolute minimum. Use them only when platform-specific behavior (e.g., local storage, platform logging, keychains) is required.
- Prefer interface-based dependency injection over `expect`/`actual` where possible.
- If using `expect`/`actual`, ensure that the signature matches perfectly and that all platform modules (`iosMain`, `androidMain`) implement the `actual` counterpart.

## 4. Compose Multiplatform Best Practices
- **State Hoisting**: Keep composables stateless by hoisting state to the parent or a presenter/ViewModel component. Pass state down and events up.
- **Immutability**: Avoid passing mutable state lists/maps directly. Use immutable DTO lists to prevent unnecessary re-compositions.
- **Lifecycle-Aware State Collection**: When collecting flows inside Compose, use lifecycle-aware collection helpers to avoid background resource consumption.

## 5. Multiplatform Resources
- Use the Compose Multiplatform Resource API for loading shared assets (images, fonts, localizable string keys).
- Do not reference Android-specific `R.drawable.*` or iOS-specific bundle resource paths in `commonMain` code.
