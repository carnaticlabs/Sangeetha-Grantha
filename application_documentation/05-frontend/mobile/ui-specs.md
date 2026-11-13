| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-11-13 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Design reference |

# Rasika UI reference (KMP)

---

## 1. Scope

How the shared Compose UI is organized and what each screen is made of. Rasika is the read-only, anonymous listener client; catalogue changes belong to the Curator Console.

## 2. Technology in use

| Concern | Choice |
|:---|:---|
| Language and UI | Kotlin Multiplatform, Compose Multiplatform with Material 3, shared by Android and iOS |
| Navigation | A small typed `RasikaNavigator` with one back stack per tab (maximum depth 8) and `RasikaDestination` values; platform back through the Compose back handler and navigation-event library |
| State | Presenters expose immutable state flows; screens render state and send intents |
| Network | Ktor client against `/v2/catalogue` and the discovery search endpoints; OkHttp on Android, Darwin on iOS |
| Local storage | A serialized local document store for favourites and preferences (no database engine) |
| Dependencies | Hand-wired container (`MobileAppContainer`), not a DI framework |
| Images | Bundled Compose resources and drawn ornaments; no remote image loader |

Versions are listed in [Current Versions](../../00-meta/current-versions.md).

## 3. Navigation map

```text
Home ─ search · featured composition · Ragas / Composers · Collections shelf · Kshetras shelf
Explore ─ search (All matches / Related meanings / Titles & lyrics) · filters · facets · Browse
Library ─ favourites
Settings ─ appearance · text size · script
pushed from any tab: Browse, Collections → Collection, Kshetras → Kshetra,
                     Raga, Composer, Krithi reader
```

## 4. Screens and shared components

| Screen | Source folder | Main parts |
|:---|:---|:---|
| Home | [home/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/home) | Search field, feature panel, entity shortcuts, collection and kshetra shelves |
| Search and Explore | [search/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/search), [explore/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/explore) | Mode options, results, collection and kshetra cards, raga and composer pickers, facet filters |
| Browse | [browse/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/browse) | Raga and composer directories with counts and a local filter |
| Entities | [entities/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/entities) | Raga and composer detail with their works |
| Collections | [collections/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/collections) | List, ordered detail, shelf, and the reader's cycle-context section |
| Kshetras | [kshetra/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/kshetra) | Region-grouped list with element filter, detail, shelf, Maps link |
| Reader | [reader/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/reader) | Lyrics and Details views, script and source choice, section jumps, section labels, Tamil digit display |
| Library | [favourites/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/favourites) | Device-local favourites |
| Settings | [preferences/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/preferences) | Appearance, text size, script, lyric size sample |
| Shared parts | [components/](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/components) | Krithi and macro-hit cards, raga sequence, load-state content, chrome and ornaments |

All user-facing strings live in `RasikaCopy`, `CollectionCopy` and `KshetraCopy`, so wording and screen-reader descriptions change in one place.

## 5. Design system

The [TRACK-138 visual design](./track-138-visual-design.md) is the token source. In short: cream paper surfaces, saffron and teal accents, Fraunces for Latin display titles, Work Sans for UI, tested Indic fallbacks, a slim painted cornice, and a calm reader. Dark mode is a full palette, not an inversion. Appearance has three states (System, Light, Dark), text size has four, and reduced motion removes crossfade, press scale and shimmer.

## 6. Conventions

- **States:** every screen renders loading, empty, error and offline states from shared components. A failed later page keeps loaded results and offers Retry.
- **Honesty:** counts come from server totals; missing optional data is hidden, not guessed; completeness is stated.
- **Errors and offline:** a network failure never deletes a favourite, and favourites reopen online.
- **Fixtures and tests:** [mobile-data fixtures](../../../modules/shared/mobile-data/src/commonMain/kotlin/com/sangita/grantha/shared/mobile/fixture) and a UI test harness support shared and native journey tests; see the [quality guide](../../07-quality/README.md).
- **Preview:** [Mobile Preview](../admin-web/mobile-preview.md) ports the presenters' rules for curators, so a rule change here usually needs the same change there.

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
