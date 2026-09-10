| Metadata | Value |
|:---|:---|
| **Status** | In progress |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | TRACK-138 coordinator |
| **Document Type** | Evidence record |
| **Track** | [TRACK-138](../../conductor/tracks/TRACK-138-rasika-mobile-app.md) |

# TRACK-138 Rasika mobile MVP — implementation evidence

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).

Living report for Node C onward. Not a claim that the MVP is complete.

## Compatibility recorded 2026-09-05

| Tool | Observed |
|:---|:---|
| JDK | Temurin 25.0.1 |
| Gradle | 9.7.1 |
| Android SDK | `local.properties` `sdk.dir`; platforms 34–37.0; compileSdk 37 |
| Android bytecode target | JVM 17 (toolchain remains 25) |
| Xcode | 26.6 (17F113) |
| iOS deployment target (planned) | 16.0 pending first `linkDebugFrameworkIosSimulatorArm64` |
| Simulators | iPhone 17 family available (Shutdown at inspection) |

## Node C increment

- Public catalogue DTOs in `modules/shared/domain/.../catalogue/CatalogueDtos.kt`
- OpenAPI `/v1/catalogue/*` (unpublished until Node B)
- Modules: `:modules:shared:mobile-data`, `:modules:mobile:androidApp`
- Shared Search / Browse / Favourites / Reader / Preferences UI + Rasika theme
- Hosts currently use `FixtureCatalogueApi` so the shell is reviewable before Node B
- Debug Android cleartext limited to `10.0.2.2` / `127.0.0.1` / `localhost`

Proof commands and outcomes are appended as they are run.

### 2026-09-05 Node C first slice

| Command | Result |
|:---|:---|
| `./gradlew :modules:shared:mobile-data:jvmTest` | Pass (12 tests after session-inactivity assertion fix) |
| `./gradlew :modules:shared:presentation:jvmTest` | Pass (navigator + search presenter) |
| `./gradlew :modules:mobile:androidApp:assembleDebug` | Pass. APK `modules/mobile/androidApp/build/outputs/apk/debug/androidApp-debug.apk` (~20 MiB) |
| `./gradlew :modules:shared:presentation:linkDebugFrameworkIosSimulatorArm64` | Pass. Android bytecode target JVM 17; iOS deployment target 16.0 in the Xcode project |
| `xcodebuild -list -project modules/mobile/iosApp/RasikaApp.xcodeproj` | Target/scheme `RasikaApp` |
| `make check-docs` | Fails on untracked TRACK-138 files until they are added to git (checker uses `git ls-files`) |

Hosts now inject `KtorCatalogueApi` (see Node B + M below). `FixtureCatalogueApi` remains for tests and offline UI review.

## Node B + M increment (2026-09-05)

### Catalogue endpoints

| Method | Path | Notes |
|:---|:---|:---|
| GET | `/v1/catalogue/krithis` | Published-only; `query`/`composerId`/`ragaId`; page 0/size 30/max 100; raga via `krithi_ragas` |
| GET | `/v1/catalogue/krithis/{id}` | Published reader + variant inventory; default reading is unambiguous primary else language/script/UUID |
| GET | `/v1/catalogue/krithis/{id}/lyrics/{variantId}` | Ownership + published 404; unsegmented text when no sections |
| GET | `/v1/catalogue/ragas` | Directory + published junction counts + matching aliases |
| GET | `/v1/catalogue/ragas/{id}` | Stored raga + aliases + parent/nomenclature |
| GET | `/v1/catalogue/composers` | Directory + published counts + matching aliases |
| GET | `/v1/catalogue/composers/{id}` | Stored profile + aliases + count |

Unknown params / malformed UUIDs → `CatalogueErrorDto` 400. Missing/unpublished/wrong-owner → same 404 shape. `Cache-Control: no-store`.

Legacy `/v1/krithis/search` and `/v1/krithis/{id}` are published-only for anonymous/non-admin. Admin JWT can still read drafts. `GET /v1/admin/krithis/search` and `GET /v1/admin/krithis/{id}` added.

### Host wiring

- Android: `androidLiveCatalogue()` → `KtorCatalogueApi` + OkHttp, debug URL `http://10.0.2.2:8080`
- iOS: `iosLiveCatalogue()` → `KtorCatalogueApi` + Darwin, debug URL `http://127.0.0.1:8080`
- Tests keep `FixtureCatalogueApi`

### Proof

| Command | Result |
|:---|:---|
| `make test` | Pass (DAL + API, including MoneyPath visibility update) |
| Catalogue/visibility/usage tests | Pass |
| `python3 -m unittest discover -s tools/tests -p test_catalogue_usage_report.py` | Pass |
| `assembleDebug` | Pass after hiding HttpClient behind `androidLiveCatalogue()` |
| `linkDebugFrameworkIosSimulatorArm64` | Pass |

Usage JSONL recorder + `tools/catalogue-usage-report.py` landed. A live report sample waits on Node A traffic. Admin web still uses public routes with JWT (drafts remain available to admins).

### Live API smoke after `make dev-down` / `make dev` (2026-09-05)

Against `http://localhost:8080` (current binary, not the 15h-stale container):

- `GET /v1/catalogue/krithis?pageSize=2` → 200, `Cache-Control: no-store`, total 1: `viSva nAthaM bhajEhaM` (`70623fa6-f90b-44f6-a61f-9136936e8be2`), ragamalika junction list preserves repeated raga names
- Reader → 6 variants, default UUID present; lyrics → 3 stored sections
- Raga directory total 1014; composer query `tyagaraja` → Tyagaraja
- `publishedOnly=false` on catalogue → 400 `VALIDATION_ERROR`

### Node A Android live journeys (2026-09-05, later)

Created AVD `Rasika_API34` (`system-images;android-34;google_apis;arm64-v8a`). First install crashed: manifest `.MainActivity` resolved to `com.sangita.grantha.rasika.MainActivity` while the class is `com.sangita.grantha.mobile.MainActivity`. Fixed FQCN; rebuilt APK.

| Journey | Result |
|:---|:---|
| Search `visva` | Live card `viSva nAthaM bhajEhaM`; ragamalika repeats preserved in UI |
| Open reader | 6 stored readings (LATIN/EN, DEVANAGARI/SA, TAMIL/TA, TELUGU/TE, KANNADA/KN, MALAYALAM/ML) |
| Switch to Tamil | Stored Tamil Pallavi text (not generated) |
| Save favourite + Favourites tab | Bookmark listed |
| Force-stop + relaunch | Favourite still listed |
| SharedPreferences | Only `krithiId` + short label + bookkeeping; no lyrics/catalogue cache |
| Browse ragas | Live directory (Abheri…); most counts 0 because only 1 published kriti |

Backend `catalogue-usage` for that session (same session UUID across search/reader/lyrics): search 200, reader 200, lyrics 200. Report from extracted Docker JSONL: 12 attempts, 3 completed searches, 2 reader views, 3 approximate sessions (`build/track-138/usage/live-android-session.jsonl`, gitignored).

Live gaps: only one published composition, so combined filters/paging/zero-result were not demonstrated on-device (fixtures cover them). Airplane-mode and instrumented `RasikaJourneyTest` not run.

### iOS host compile (H, not I)

`xcodebuild` with a concrete simulator UUID fails: Xcode 26.6 reports **iOS 26.5 is not installed**. Installed runtimes: iOS 17.0, 26.0, 26.1, 26.2. `xcodebuild -destination 'generic/platform=iOS Simulator'` **BUILD SUCCEEDED**; product `build/track-138/ios-derived/Build/Products/Debug-iphonesimulator/Rasika.app`. `verify-ios.sh` now falls back to that generic destination. Live iOS journeys remain blocked.

### Additional tests / CI

- `CatalogueServiceTest` 8; `KrithiReaderPresenterTest` 5; `FavouritesPresenterTest` 3; admin `client.test.ts` 2
- CI: `mobile-jvm`, `mobile-android` (`setup-android` + `assembleDebug`), `mobile-ios` (`verify-ios.sh` on macos-15)

Ref: application_documentation/05-frontend/mobile/track-138-visual-design.md

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
