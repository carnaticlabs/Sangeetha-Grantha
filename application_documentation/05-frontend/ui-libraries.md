| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-11-13 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Frontend UI Libraries

---

The libraries each client actually uses, taken from the Curator Console's [package.json](../../modules/frontend/sangita-admin-web/package.json) and the shared [version catalog](../../gradle/libs.versions.toml). Exact versions are in [Current Versions](../00-meta/current-versions.md). Before adding a library, check this list: the console deliberately keeps a very small runtime dependency set.

## 1. Curator Console (admin web)

**Stack:** React, TypeScript, Vite, Tailwind CSS v4, Bun.

| Category | Library or approach | Purpose |
|:---|:---|:---|
| UI | **React** and **React DOM** | Function components |
| Server state | **TanStack Query** | Fetching, caching and invalidation; the client is created in [App.tsx](../../modules/frontend/sangita-admin-web/src/App.tsx) |
| Routing | **React Router** | Client-side routes, including the redirects for retired list pages |
| Styling | **Tailwind CSS v4** through `@tailwindcss/postcss` | Utility classes. Design tokens are defined in an `@theme` block in [index.css](../../modules/frontend/sangita-admin-web/src/index.css), with dark values redefined under `:root[data-theme="dark"]` |
| Fonts | Google Fonts: Fraunces, Work Sans, Noto Sans, Public Sans | Loaded from [index.html](../../modules/frontend/sangita-admin-web/index.html) and `index.css` |
| Icons | **Material Symbols Outlined** (icon font) | Referenced by name in components such as the sidebar |
| Forms, dialogs, toasts, selects | In-house components in [components/](../../modules/frontend/sangita-admin-web/src/components) | For example `ConfirmationModal`, `Toast`, `SearchableSelectField` and the form helpers. There is no form, validation or headless-UI library |
| API access | A typed client in [api/](../../modules/frontend/sangita-admin-web/src/api) using `fetch` | Authentication, editorial and preview requests |

Development tooling (not shipped): **Vite** with the React plugin, **TypeScript**, **ESLint** with typescript-eslint, **Vitest** with Testing Library and jsdom for component tests, and **Playwright** for browser journeys. See the [console guide](./admin-web/ui-specs.md) for commands and the [E2E guide](../07-quality/qa/e2e-testing.md).

## 2. Rasika (Kotlin Multiplatform)

**Stack:** Kotlin Multiplatform, Compose Multiplatform, shared by Android and iOS.

| Category | Library | Purpose |
|:---|:---|:---|
| UI | **Compose Multiplatform** (runtime, foundation, ui) | Shared screens |
| Components | **Compose Material 3** | Material components and theming |
| Icons | **Material Icons Extended** | Standard icons |
| Resources | **Compose Resources** | Bundled fonts (Fraunces, Work Sans) and drawables |
| Navigation | In-house `RasikaNavigator` with typed destinations, plus the Compose back handler and **navigation-event** library for platform back | One back stack per tab; there is no Jetpack Navigation graph |
| Lifecycle | **AndroidX Lifecycle** runtime for Compose | Lifecycle-aware state collection |
| Networking | **Ktor client** with content negotiation | OkHttp on Android, Darwin on iOS, and a mock engine in tests |
| Data | **kotlinx.serialization**, **kotlinx.coroutines**, **kotlinx.datetime** | DTOs, async work, time |
| Local storage | In-house serialized document store | Favourites and preferences |
| Images | None | Artwork is bundled; there is no remote image loader |

Screen structure is described in the [Rasika UI reference](./mobile/ui-specs.md).

## 3. Shared design identity

The two clients use the same visual identity (cream paper, saffron and teal, Fraunces for display titles, Work Sans for UI) but define it separately: Tailwind `@theme` tokens in the console and Compose theme tokens in Rasika. The mobile tokens are documented in the [TRACK-138 visual design](./mobile/track-138-visual-design.md). A change to a shared colour or type decision needs the same change in both places; nothing generates one from the other.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
