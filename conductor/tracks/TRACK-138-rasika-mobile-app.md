| Metadata | Value |
|:---|:---|
| **Status** | Plan accepted — Nodes B/M/H/A in Verify; I blocked on iOS 26.5 runtime |
| **Version** | 1.4.0 |
| **Last Updated** | 2026-09-05 (visual alignment) |
| **Author** | Codex, for Seshadri |
| **Owner** | Seshadri |
| **Priority** | Proposed P1 — first Rasika mobile experience |

# TRACK-138: Rasika Mobile App — Search, Browse and Read

---

## Goal

Deliver a minimal working Android and iPhone application for a Rasika to search Kritis and Ragas, browse Kritis by Raga or Composer, read stored lyrics in available scripts, and save favourites locally.

## Context

- [Detailed product and feasibility analysis](../../application_documentation/01-requirements/mobile/rasika-mvp-analysis.md).
- [Agent coordination graph and implementation kickoff prompt](../../application_documentation/09-ai/track-138-agentic-build-guide.md).
- [Existing public mobile PRD](../../application_documentation/01-requirements/mobile/prd.md) and [mobile UI specification](../../application_documentation/05-frontend/mobile/ui-specs.md) describe a broader unimplemented product.
- [TRACK-108](./TRACK-108-semantic-search.md) remains a separate semantic retrieval initiative. This release has no dependency on embeddings or conversational AI.
- The shared domain and Compose build scaffold exist; application hosts, screens, navigation and local persistence do not yet exist.

## Intent

**Intent Status: Accepted**

**Accepted by:** Seshadri (explicit user acceptance in this task)

**Accepted at:** 2026-09-05

### Problem

The user wants “a working mobile application for a Rasika or a Carnatic Music Enthusiast to search Kritis, Ragas, Kritis by Raga, Kritis by Composer,” with a minimal interface and multiplatform capability. The application need not use the conversational interface discussed earlier. The current project has a curator web console and shared Kotlin models, but no runnable mobile application for this audience.

### Proposed outcome

An Android and iPhone app using the existing catalogue, with Search, Browse and Favourites as its primary navigation. A user can find a composition directly, browse a raga or composer and open associated Kritis, inspect stored metadata and sectioned lyrics, select an available script/variant, and save a local bookmark.

The first engineering release includes an installable Android debug build and a runnable iOS simulator host using real backend responses, with run instructions and evidence that the same core journeys work on both platforms. Physical-device/beta/store distribution is recorded separately rather than implied by a successful simulator build.

### Confirmed user preferences — 2026-09-05

- Android and iPhone, with Android validated first.
- Online search with locally saved favourites.
- English interface; lyrics in all available stored scripts.
- Keep the dataset on the server; searches, browsing and Kriti openings reach the API. Measure visits and usage as the Rasika audience grows.
- Use a build dependency graph and coordinated agents for delivery, as explicitly confirmed by Seshadri; no LangGraph or runtime AI orchestration dependency is requested.

### Accepted scope

- Name/incipit search and explicit composer/raga filters.
- Searchable Raga and Composer directories and associated Kriti lists.
- A read-only Kriti detail/lyric screen, preserving sections, variants and ragamalika order.
- Device-local favourites, preferred script, text size and appearance.
- Loading, empty, offline/error and retry states.
- Public-safe server contracts, search correctness and device validation necessary to make these flows work.
- Server-side request/search/view measurement and approximate anonymous sessions, with clear separation between API hits and visits.

### Affected users and systems

Rasikas, Carnatic learners and other catalogue readers. Shared domain/presentation modules, proposed mobile data and app-host modules, public Ktor routes/services/DAL, API documentation, and mobile build/test configuration. Curator web search may require a compatibility adjustment when correcting the current public visibility behavior.

### Constraints

- Use KMP + Compose Multiplatform, reusing existing repository foundations and the central version catalogue.
- Public app access is read-only and published-only, enforced server-side; no embedded administrative credentials.
- Favourites store only bookmark IDs and small labels. Catalogue metadata and lyrics are fetched from the server; no persistent catalogue/lyric downloads or full-dataset preload.
- Use bounded pagination and requests tied to actual user interactions; session and hit measurements must not double-count retries as new visitors.
- Display stored data faithfully; preserve raga identity and ordered membership, section structure, original language, script and source variants.
- Follow Flyway, `DatabaseFactory.dbQuery`, backend audit, testing and restart requirements for any implementation changes.
- No automatic publication of catalogue records, deployment, store submission or paid service setup is included in the current analysis request.

### Out of scope for this release

Conversational/semantic search, generated translations, audio/voice, notation rendering, uploads, account sync, full offline catalogue, social features and recommendation feeds.

### Open questions

- Intent, no-login operation, three-tab navigation and the analysis's “Decisions and remaining questions” recommendations were explicitly accepted on 2026-09-05; do not ask for those decisions again.
- Set the minimum supported iOS version during the compatibility/device review.
- Confirm a reachable hosted API and signing identity when physical-device/beta distribution becomes the target.

## Spec

**Spec Status: Accepted**

**Accepted by:** Seshadri (explicit user acceptance in this task)

**Accepted at:** 2026-09-05

### Requirements

| ID | Requirement | Acceptance evidence |
|:---|:---|:---|
| R1 | Android and iPhone share KMP/Compose screens and state. Android behavioral validation occurs first; both hosts must run before completion. | Installable Android debug APK and runnable iOS simulator app using the live development backend |
| R2 | Search Kritis by title/incipit with explicit composer and raga filters; searchable Raga and Composer directories lead to associated Kritis. | Known-record, alias, combined-filter, empty-result and pagination journeys |
| R3 | All public content and association counts are published-only, enforced by the server. Admin access remains explicitly authorized. | Anonymous, invalid-token, non-admin and admin integration cases; public IDs cannot expose drafts through lyrics/detail |
| R4 | Raga filtering uses any ordered raga membership and deduplicates compositions. Composer/raga/query filters combine with AND semantics. | Secondary-raga ragamalika, repeated membership, distinct counts and stable page ordering tests |
| R5 | Reader shows stored metadata and ordered lyric sections, preserving source/script variants and original language. | Representative section/variant fixtures and Android/iOS script switching |
| R6 | English UI; available stored scripts only; no generated missing lyrics, translations or transliterations. | Missing-script/content states, exact stored-text checks and clear selected variant |
| R7 | Search, Browse and Favourites tabs preserve useful navigation state. Preferences cover script, text size and system/light/dark theme. | Back navigation, large text, theme and restart checks |
| R8 | Local favourites contain only composition UUID, a short display label and bookmark bookkeeping; preferences are local. No persistent catalogue/lyric cache or bulk prefetch. | Restart and local-storage inspection; favourite detail fetched online |
| R9 | Committed searches, directory navigation, Kriti opening and favourite reopening reach the API. Back-stack rendering/recomposition does not itself generate new hits. | Request traces for real interactions, cancellation and repeated rendering |
| R10 | Measure request traffic, searches, zero-result rate, Kriti views and approximate anonymous sessions separately; retries/pagination do not inflate visits. | Deterministic usage report from a test session and documented metric definitions |
| R11 | Loading, empty, missing/withdrawn content, timeout and connection-failure states are actionable. Late responses cannot overwrite a newer query. | Mock-engine/state tests and device fault scenarios |
| R12 | No login, admin credential, AI dependency or new analytics provider is required in the mobile app. Release transport is HTTPS; debug endpoint configuration is environment-specific. | Package/config review and emulator/simulator connectivity proof |

### Design

**Ownership and modules.** Keep serializable public DTOs in `modules/shared/domain`. Add `modules/shared/mobile-data` for the catalogue client and local bookmark/preferences interfaces. Implement common UI, navigation and immutable state in the existing `modules/shared/presentation`. Add independent hosts under `modules/mobile/androidApp` and `modules/mobile/iosApp`. Ktor routes delegate to services and repositories; Exposed stays inside `DatabaseFactory.dbQuery`. Backend edits continue to use the current version catalogue and applicable layer skills.

**Public read contract.** Introduce a coherent `/v1/catalogue` namespace for mobile summaries/readers. Existing public endpoints must also enforce their publication boundary; a new namespace does not excuse leaving draft exposure in the old route. Give the admin web client an explicitly authorized search path when adapting that behavior.

| Endpoint | Contract |
|:---|:---|
| `GET /v1/catalogue/krithis` | Optional `query`, `composerId`, `ragaId`; page default 0, pageSize default 30/max 100; stable summaries and total |
| `GET /v1/catalogue/krithis/{id}` | Public reader metadata, complete ordered ragas and variant inventory; no administrative author IDs/raw payloads |
| `GET /v1/catalogue/krithis/{id}/lyrics/{variantId}` | One stored variant with section type, label, order and text; enforce variant ownership and current composition visibility |
| `GET /v1/catalogue/ragas` | Optional query; bounded paging, canonical names, approved matching aliases and published composition counts |
| `GET /v1/catalogue/ragas/{id}` | Stored raga reference data, aliases and documented relations; associated works use the filtered Kriti endpoint |
| `GET /v1/catalogue/composers` | Optional query; bounded paging, names, approved matching aliases and published composition counts |
| `GET /v1/catalogue/composers/{id}` | Public stored profile and catalogue count; associated works use the filtered Kriti endpoint |

Search query length is capped at 200 Unicode code points. Invalid IDs, unsupported parameters and out-of-range paging return a consistent validation error; missing, unpublished and wrong-owner detail/variant requests return the same public 404 shape. Do not accept a client parameter to enable unpublished data. Bound all list operations and avoid per-card queries. Document schemas, nullability, errors and pagination in OpenAPI before parallel client/backend work consumes them.

Summaries include composition UUID/title/incipit, composer UUID/name, ordered raga references and optional tala. Reader metadata includes musical form, original language and available variant UUIDs with language/script/transliteration scheme, primary status, label and public source reference where documented. Do not expose private editorial notes merely because a DTO already carries them. Use the explicit stored primary variant when unambiguous; otherwise apply a documented deterministic fallback and show the selected reading. Preferred display script never changes original language or asserts source authority.

**Search correctness.** Canonical normalization plus approved aliases supports ordinary case/diacritic variants. Preserve raga identity distinctions under ADR-017; no reference creation, resolution-queue mutation or guessed alias insertion occurs during search. Treat query wildcards as literal input. Raga filters use junction membership, retaining order in the returned ragamalika; the title/UUID ordering used for pagination must also determine final assembled result order. Do not silently fold a missing junction into a single-raga representation; report corpus defects for curation.

Raga results and selections retain stable UUID identity. If a name or alias matches multiple identities, show all candidates with available stored mela/parent/tradition context; never select one automatically. Nomenclature-equivalent relations remain separately labelled links between distinct identities; they neither expand an exact raga filter nor combine counts. Deduplicate composition results and counts only. Preserve every stored raga membership occurrence in `orderIndex` order, including repeated ragas and optional section associations, and return/display the stored ragamalika flag. Do not infer section transitions where associations are absent.

**Reader and data integrity.** Preserve repeated/numbered sections, source distinctions and stored text. Sections are keyed by IDs and ordered metadata, not parsed again by the client. Every displayed lyric section belongs to the selected variant UUID. Multiple readings in the same language/script remain separately selectable using available source/reading labels. Script switching may select another stored variant, whose identity is shown; never stitch sections across variants to fill gaps. “Primary” means the stored default, not independently verified textual authority.

Absent lyrics are unavailable; completeness is unknown unless stored evidence establishes a partial or complete reading. Preserve musical-form-specific section labels, repeated Charanams and existing subsection structure without inventing missing sections. Varnam and Swarajathi records retain their musical-form label; explain that the reader displays available lyrics and does not render notation or establish completeness of the whole composition. Raga scales and tala properties are displayed only when documented. Musical disagreement, contradictory ragamalika flags and incomplete membership sequences are reported for curation rather than repaired automatically.

**State and persistence.** Use lifecycle-aware immutable state and a single typed navigation stack strategy. A newer query/filter cancels or supersedes old requests, clears pagination, and excludes late results. Normal back navigation restores bounded screen state. Reopening a reader/favourite or explicitly refreshing revalidates with the server. Store small bookmark labels/UUIDs, preferences and format version only; never persist raw HTTP catalogue responses or lyrics. A temporary network failure must not remove bookmarks; confirmed unavailable content is labelled and remains removable.

**Traffic and visits.** Use existing Micrometer and structured logs, with low-cardinality route/status/action tags. Attach a random in-memory session UUID to real mobile requests; begin a new session on process launch or after 30 minutes of inactivity. Session IDs are untrusted analytics metadata, never credentials or metric labels. Correlate one logical interaction ID across retries and its related requests. Report API attempts separately from successful searches/views, group pagination under its originating search, and count a visitor session once in the reporting interval even if it spans many reads. Label session counts approximate; do not claim unique people. Logs carry route category/action/status/timing/session/interaction references, not raw search text, lyrics, administrative tokens or device fingerprints. Use a small log-derived report; define bounded retention and development-traffic separation in the Plan. Metrics failures must not block catalogue access.

**Platform baseline.** Reuse the current Kotlin/Compose/AGP pins; Android library minSdk is currently 24. Validate minimum iOS deployment target and SDK compatibility during the build spike and document the result before calling that node verified. Those technical compatibility choices are delegated by the accepted Intent; no new device-preference question is required unless compatibility would exclude a user-specified device. Keep Android app and KMP library plugin boundaries separate. Inject debug/release base URLs; do not hardcode device `localhost` or permit broad cleartext exceptions in release builds.

**Proof and release boundary.** First show Android search → reader → stored script switch → favourite → restart using representative published data. Add Raga/Composer and fault journeys, then demonstrate parity on iOS. Emulators/simulators prove the engineering milestone; physical signing, remote hosting and store distribution remain separate follow-ups. If the live corpus cannot support a required case, use fixtures for deterministic tests and report the live-data gap explicitly. Do not publish drafts or reset the development corpus to make a demo pass.

### Acceptance checks

Map R1–R12 to concrete tests in the Plan. Required checks include backend `make test` and `make test-integration`, shared mobile tests, Android app assembly and emulator journeys, iOS host build/simulator journeys, and `make check-docs`. If admin client changes, run `make test-frontend`, the frontend build and its affected browser flow. Restart the stack according to the Kotlin/shared restart skill before live API verification. Discover the actual platform task names from the implemented Gradle/Xcode project rather than treating guessed commands as proof.

The final independent review covers Bugs, Security and Compliance per `REVIEW.md`. Resolve important findings with focused regression checks, then rerun affected integration journeys. Completion requires artifacts and runtime evidence from both platforms, not just a successful shared-library compilation or a review agent's assertion.

### Flagged concerns

- Current public search/detail behavior and admin-client dependence need a coordinated correction.
- Published corpus coverage is unmeasured. Public visibility cannot be relaxed to fill an empty screen.
- Existing mobile skill/docs contain stale SDK/scaffold details; verify actual build files and use scoped compatibility fixes.
- Multiple agent writers must not concurrently edit shared DTOs, OpenAPI, Gradle/version catalog, migrations or the track. The coordinator assigns ownership and integrates changes.
- Source rights/curation, production hosting and device signing are not implicitly approved by this Spec.

### Domain review

Completed a read-only `carnatic-musicologist` subagent review on 2026-09-05 against Domain Model §6 and ADR-017's accepted refinements. Incorporated four material clarifications: same-name raga identity selection, repeated ragamalika membership versus composition deduplication, reading identity versus script choice, and evidence-based lyric completeness across musical forms. No incorrect musical assertions were identified in the proposed Spec. This is design review, not a live catalogue audit; no musical records were queried or changed.

### Open questions carried forward

No unresolved product-choice question was left at Spec acceptance. iOS compatibility and actual published-data readiness are implementation checks; hosted API/signing remain follow-ups. Seshadri confirmed a build dependency graph and coordinated agents. The linked execution guide defines that development workflow without adding AI to the mobile runtime.

## Plan

**Plan Status: Accepted**

**Accepted by:** Seshadri (explicit “get started with the build” in this task)

**Accepted at:** 2026-09-05

### Execution boundary and decisions

This Plan implements the accepted Spec, R1–R12. Seshadri authorized implementation by requesting the TRACK-138 build to start on 2026-09-05. After acceptance, the coordinator executes the graph through working Android and iOS simulator delivery, using the linked agentic build guide. Ordinary implementation choices, focused repairs and local verification proceed without repeated approval. Material departures from the Spec return for a decision; update this Plan in the same change when implementation details diverge.

Use the existing KMP/Compose, Material3, Ktor, coroutines and serialization pins. Add a small typed navigation stack implemented in common Kotlin, constructor-injected repositories and native preference stores. Do not introduce a navigation framework, dependency-injection framework, local database or AI service. Keep JDK 25 as the build toolchain while validating an Android-supported bytecode target; do not assume Android accepts JVM 25 bytecode. Preserve presentation's `iosArm64` and `iosSimulatorArm64` targets; leave domain's existing targets intact.

The **baseline requires no schema migration, data backfill or new dataset**. Serving fields, raga identity/alias tables and junction tables already exist. If measured query plans justify an index, the coordinator allocates the next Flyway migration version, records the evidence and updates this Plan. No committed migration changes, automatic publication, ingestion deduplication changes, corpus reset or speculative index rollout.

### Inspected integration points

- Presentation contains only its Gradle scaffold; no Android or iOS application host exists. Root settings include domain/presentation/backend only. Mobile work therefore includes real hosts and tests, not just screens.
- Current public search can include drafts; public detail is unguarded. The admin client uses these public reads, and its `getKrithi` admin parameter is ignored. New catalogue routes alone cannot close this boundary.
- Legacy public notation treats any JWT principal as administrative. Correct that authorization path as part of the same boundary change, without adding notation to the mobile UI.
- Existing search filters the primary raga and does not preserve final page ordering. Search must use junction membership and stable ID-page hydration.
- Import approval currently creates a DRAFT composition. An existing API test assumes immediate public visibility; preserve the actual import/publication distinction and correct that test explicitly.
- The variant creation helper consolidates composition/language/script pairs. Reader tests for multiple stored readings must create distinct isolated fixture rows without changing ingestion behavior.
- Logging is configured programmatically in `config/LogbackConfig.kt`, not an XML resource. Existing Micrometer instrumentation counts requests but does not implement the accepted logical interactions/session report. Development SQL debug logging can contain parameter values and needs explicit treatment.

### File ownership and change inventory

All paths below are repository-relative. Prefixes expand literally; each listed new file is a proposed deliverable, not a claim that it exists. One writer owns a file at a time.

| Prefix | Exact directory |
|:---|:---|
| API | `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api` |
| DAL | `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal` |
| DTO | `modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model` |
| DATA | `modules/shared/mobile-data/src/commonMain/kotlin/com/sangita/grantha/shared/mobile` |
| UI | `modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation` |
| AT | `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api` |
| DT | `modules/backend/dal/src/test/kotlin/com/sangita/grantha/backend/dal/integration` |
| WEB | `modules/frontend/sangita-admin-web/src` |

**Coordinator: contracts, build foundation and integration**

- Add `DTO/catalogue/CatalogueDtos.kt`; modify `openapi/sangita-grantha.openapi.yaml`. Define explicit public fields, paged responses, reader/variant/section ownership, errors and interaction headers. Do not expose existing editorial DTOs wholesale.
- Modify `settings.gradle.kts`, `gradle/libs.versions.toml`, `modules/shared/presentation/build.gradle.kts`; add `modules/shared/mobile-data/build.gradle.kts`. Register the mobile data and Android app modules. Add a JVM target to mobile-data for fast client/storage-contract tests; mobile UI execution remains Android/arm64 iOS.
- Add Ktor MockEngine using the existing Ktor pin; resolve and pin compatible multiplatform lifecycle and native UI-test artifacts during C. Verify lifecycle-aware collection on both platforms before choosing the final artifact/version; an Android-only dependency is insufficient. Existing Kotlin/Compose/AGP pins remain unless the compatibility proof identifies a specific necessary correction. Any pin change includes the required version-documentation sync.
- Modify `Makefile` to expose `test-mobile`, `mobile-android` and `mobile-ios` using the task names proven during C; add `tools/mobile/verify-ios.sh` for reproducible Xcode build/test invocation with a supplied simulator UUID. Modify `.github/workflows/ci.yml` for Android build/tests and a macOS arm64 iOS build/test job, preserving all existing gates.
- Add `application_documentation/10-implementations/track-138-rasika-mobile-mvp.md` as the implementation evidence report. Update `application_documentation/01-requirements/mobile/prd.md`, that directory's `README.md`, `application_documentation/05-frontend/mobile/ui-specs.md`, the build guide, this track and registry to describe the implemented MVP and deferred broader features accurately.

**Backend specialist: catalogue and compatibility**

- Add `DAL/repositories/CatalogueRepository.kt`, `DAL/models/CatalogueDtoMappers.kt`; modify `DAL/SangitaDal.kt` and `DAL/repositories/KrithiSearchRepository.kt`.
- Add `API/services/CatalogueService.kt`, `API/routes/CatalogueRoutes.kt`, `API/routes/CatalogueParameters.kt`; modify `API/di/AppModule.kt`, `API/plugins/Routing.kt`, `API/routes/PublicKrithiRoutes.kt`, `API/routes/AdminKrithiRoutes.kt`, `API/services/KrithiService.kt`, `API/plugins/StatusPages.kt` and `API/plugins/Caching.kt`.
- Modify `WEB/api/client.ts` to use explicitly authorized admin search/detail endpoints in this admin-only client; update `WEB/hooks/useKrithiData.ts` only if needed to make call intent explicit. Existing page layout is unchanged. Verify `WEB/pages/KrithiList.tsx` and the curator review/editor journeys through the browser.
- Add `API/plugins/CatalogueUsage.kt`, `API/services/CatalogueUsageRecorder.kt`; modify `API/plugins/RequestLogging.kt`, `API/config/LogbackConfig.kt`, `API/plugins/Metrics.kt` and `API/App.kt` for telemetry wiring and safe log defaults.
- Add `tools/catalogue-usage-report.py`, `tools/tests/test_catalogue_usage_report.py`; use Python standard-library JSON/date/argument parsing, without coupling to the extraction worker. Modify `.gitignore` only if the chosen local analytics-output location is not already ignored.

**Mobile specialist: common data, state and UI**

- Add `DATA/config/MobileApiConfig.kt`; `DATA/network/CatalogueApi.kt`, `KtorCatalogueApi.kt`, `MobileHttpClient.kt`, `CatalogueFailure.kt`; `DATA/repository/CatalogueRepository.kt`, `FavouritesRepository.kt`, `PreferencesRepository.kt`; `DATA/storage/BookmarkStore.kt`, `PreferencesStore.kt`, `LocalSettingsCodec.kt`; `DATA/usage/MobileSession.kt`, `InteractionContext.kt`.
- Add `UI/RasikaApp.kt`, `UI/di/MobileAppContainer.kt`; `UI/navigation/RasikaDestination.kt`, `RasikaNavigator.kt`; presenter/screen pairs under `UI/search`, `UI/browse`, `UI/reader`, `UI/favourites`, `UI/preferences`: `SearchPresenter.kt`/`SearchScreen.kt`, `BrowsePresenter.kt`/`BrowseScreen.kt`, `KrithiReaderPresenter.kt`/`KrithiReaderScreen.kt`, `FavouritesPresenter.kt`/`FavouritesScreen.kt`, `PreferencesPresenter.kt`/`PreferencesScreen.kt`.
- Add `UI/components/KrithiCard.kt`, `LoadStateContent.kt`, `UI/theme/RasikaTheme.kt` and `modules/shared/presentation/src/commonMain/composeResources/values/strings.xml` for the English UI. Use system fonts initially, with on-device script shaping checks; no decorative asset generation dependency.

**Platform specialist: native adapters, hosts and integration tests**

- Add `modules/shared/mobile-data/src/androidMain/kotlin/com/sangita/grantha/shared/mobile/platform/AndroidMobileDependencies.kt` and the corresponding `src/iosMain/.../platform/IosMobileDependencies.kt`. These own platform network engines and preference-store implementations; the mobile specialist owns their common interfaces.
- Add `modules/mobile/androidApp/build.gradle.kts`, `src/main/AndroidManifest.xml`, `src/main/kotlin/com/sangita/grantha/mobile/MainActivity.kt`, `src/debug/AndroidManifest.xml`, `src/debug/res/xml/network_security_config.xml`. Use application ID `com.sangita.grantha.rasika` for the local engineering build; store registration is not implied.
- Add `modules/mobile/iosApp/RasikaApp.xcodeproj/project.pbxproj`, `RasikaApp.xcodeproj/xcshareddata/xcschemes/RasikaApp.xcscheme`, `RasikaApp/RasikaApp.swift`, `RasikaApp/ComposeView.swift`, `RasikaApp/Info.plist`, `Configuration/Debug.xcconfig`, `Configuration/Release.xcconfig` and `RasikaAppUITests/RasikaJourneyTests.swift`.
- Add `modules/shared/presentation/src/iosMain/kotlin/com/sangita/grantha/shared/presentation/MainViewController.kt` as the Swift bridge using the existing `presentation` framework name.
- Add Android tests under `modules/mobile/androidApp/src/androidTest/kotlin/com/sangita/grantha/mobile/`: `RasikaJourneyTest.kt`, `LocalStorageContractTest.kt`. The root coordinator retains ownership of common build configuration even when the platform specialist proposes changes.

**Test ownership follows the layer owner**

- Backend adds `DT/CatalogueSearchTest.kt`, `CatalogueReaderTest.kt`; `AT/routes/CatalogueRoutesTest.kt`, `PublicVisibilityTest.kt`; `AT/services/CatalogueServiceTest.kt`, `CatalogueUsageRecorderTest.kt`; and `modules/backend/test-support/src/main/kotlin/com/sangita/grantha/backend/testsupport/CatalogueFixtures.kt` shared by DAL/API tests.
- Backend modifies `AT/routes/MoneyPathApiTest.kt` deliberately: replace its plain-text 404 expectation with the uniform JSON error contract; assert public 404 after import approval creates a Draft, then publish through the existing authenticated workflow in the isolated test and assert public 200. Retain mutation/audit assertions. Add `WEB/api/client.test.ts` for authorized read route selection.
- Mobile adds `CatalogueApiTest.kt`, `MobileSessionTest.kt`, `LocalSettingsCodecTest.kt` under matching `modules/shared/mobile-data/src/commonTest/kotlin/com/sangita/grantha/shared/mobile/` packages, and `SearchPresenterTest.kt`, `BrowsePresenterTest.kt`, `KrithiReaderPresenterTest.kt`, `FavouritesPresenterTest.kt`, `RasikaNavigatorTest.kt` under matching presentation `src/commonTest` packages.
- Set the repository's `SANGITA_ALLOW_TEST_EDITS=1` opt-in for deliberate test edits. No deletion/weakening of tests to obtain a pass. Fixtures use isolated Testcontainers/Flyway databases and existing reference identities or clearly synthetic test data, not mutations of the live corpus.

### Order of work and exit gates

#### C — Contracts and build foundation (coordinator; specialists provide bounded spikes)

1. Preserve the current diff and unrelated files. Record actual available JDK/SDK/Gradle/Xcode, Android emulator and arm64 iOS simulator capabilities. Inspect existing published-data coverage read-only without starting ingestion or modifying publication state.
2. Freeze DTOs and OpenAPI against the accepted Spec. Define canonical alias eligibility from existing curated alias records and provenance/confidence fields; exclude unresolved proposals and resolution queues. Do not invent an approval column or merge ambiguous identities. Use `raga_match_key` for existing raga normalization and read-only composer alias lookup.
3. Define exact API validation and result semantics: 200-code-point queries, page 0/size 30 defaults, size 100 cap, safe Long offsets, unknown parameters 400, malformed UUID 400, missing/unpublished/wrong-owner 404. Stable summary order is normalized title then UUID; ragas retain stored occurrence order independently.
4. Register modules and make minimal hosts compile and launch a shell. Prove Android application-plugin/KMP-library separation and iOS framework linkage. Select the lowest supported iOS deployment target demonstrated by the actual dependency build, and record it. Resolve lifecycle/test pins and the supported Android bytecode target here, before feature development.
5. Freeze common platform factory/storage/config interfaces and the single-writer map. Record actual Gradle test task names, Xcode scheme/destination, CI runner compatibility and any external blockers. Publish serialization fixtures to downstream owners.

**Exit:** contract fixtures/types compile; both native host foundations are demonstrated; executable test tasks and dependency choices are recorded. No feature code is delegated against an unstable contract. If one host is externally blocked, continue independent backend/common work only against the verified interfaces; retain that host's blocker and do not mark C or delivery complete.

#### B — Public backend, admin compatibility and measurement (after contract freeze)

1. Implement public repository reads with composition publication and variant/section ownership in the query. Page distinct composition IDs, then batch hydrate; counts and raga lists must not produce per-card queries. Exact raga selection matches any junction occurrence, never a relation-expanded set. Preserve repeated memberships and stored section associations.
2. Implement all seven catalogue endpoints, public DTO allowlists and a deterministic server default reading: the unambiguous stored primary, otherwise stable language/script/variant UUID order. Return the complete inventory and default UUID. The mobile client overlays its local script preference on that inventory, choosing an unambiguous primary among matching candidates or the same stable fallback; no new preference query parameter is required. Show the chosen reading; never consolidate same-script variants or infer completeness. When structured sections are absent but original unsegmented lyric text is stored, return/display that text as an unsegmented reading without inventing section boundaries. Set `Cache-Control: no-store` on catalogue responses and explicit legacy public reader responses where applicable.
3. Coordinate legacy search/detail/notation publication checks with authorized admin search/detail routes and the admin-client migration. Keep the legacy published-response envelope compatible where feasible, but remove private fields and use uniform errors. Invalid/non-admin JWTs cannot unlock private reads. Preserve existing privileged notation through its authorized route.
4. Return generic internal errors to public callers; retain diagnostically useful server errors without raw query/lyric/token payloads. Exercise the corrected MoneyPath expectations and admin draft-edit journey.
5. Implement telemetry and report as described below; prove operation when telemetry recording fails. Complete backend and affected web checks before live mobile integration.

**Exit:** visibility, identity, paging, reader and traffic tests pass; public and admin routes work together; no unresolved important regression in the existing curator workflow.

#### M — Shared client and minimal UI (parallel with B after contract freeze)

1. Build Ktor client with injected URL/engine, bounded request timeout, structured failures and proper query encoding. Default to explicit user retry; preserve interaction identity if a transport retry occurs. Disable persistent response caching, including the native Darwin URL cache; no catalogue prefetch or background polling.
2. Implement small versioned local settings using SharedPreferences/NSUserDefaults behind interfaces. Bookmark payload: UUID, label capped at 160 Unicode code points, creation timestamp and schema version. Preferences: script, text size and theme. Corrupt storage is recoverable without downloading content; network errors never remove favourites. Store labels in the local list and fetch the reader online when selected.
3. Implement typed destinations for three tabs, Raga/Composer detail/filtered lists and Kriti reader, with per-tab bounded state. Presenters use StateFlow, cancellation and generation checks. A committed search/filter resets paging; late responses cannot replace a newer result. Back-stack restoration does not issue a request; explicit open, retry or refresh does.
4. Implement available-script and source-reading selection independently, ordered sections/ragas, ambiguity labels and evidence-based unavailable/unknown/partial states. Include preferences, actionable errors, large-text layout, accessible control names and platform back behavior.
5. Execute common tests with MockEngine/virtual clocks and controlled response order. Make loading/empty/error outcomes reviewable without waiting on the live corpus.

**Exit:** tests execute and pass; accepted UI flows are present against contract fixtures; no persisted catalogue or lyrics; state and rendering do not create artificial traffic.

#### H — Native host completion (parallel with B/M)

Complete host dependency injection, lifecycle teardown/foreground handling, system theme, back integration and native storage adapters. Use debug Android emulator API URL `http://10.0.2.2:8080` and iOS simulator URL `http://127.0.0.1:8080`, configurable for other devices. Keep debug cleartext/ATS exceptions restricted to development configuration. Release configuration requires an explicitly supplied HTTPS URL and fails clearly if absent; do not substitute an invented hosted service. Use no embedded admin secret or signing identity. Wire native journey tests, test accessibility identifiers and CI artifact capture (14-day artifact retention).

**Exit:** both apps build and run the shared screens; platform storage/lifecycle checks pass; release transport configuration is verified independently of unrequested physical signing/store distribution.

#### A — Live Android integration (requires verified B/M/H)

Coordinator restarts the shared stack using the required workflow, waits for backend health and admin availability, and records representative published IDs available at that time. Install the debug APK, then exercise title/incipit search, Raga/Composer browsing, combined filters/paging, reader script/reading choice, favourite/save/restart/reopen and network failure/retry. Inspect the app's local storage and backend request/interaction evidence. Include ragamalika and multiple same-script readings in deterministic tests; if absent from the live corpus, identify that limitation explicitly instead of changing data to fill it.

**Exit:** Android runtime journeys and storage/traffic checks pass against the current backend binary; APK, logs/screenshots and actual commands recorded. Empty or mock-only screens are not completion evidence.

#### I — Live iOS parity (after A)

Build and launch the shared scheme on an available arm64 simulator; run the same core journeys and native UI tests. Check script shaping, large text, lifecycle resume and NSUserDefaults persistence separately, plus absence of persistent network cache. Record destination UUID, deployment target, build/test commands and outputs. Physical-device signing remains a separate distribution step.

**Exit:** iOS live core-flow evidence and native test results available; framework compilation alone is insufficient.

#### Q and D — Independent review, repair and delivery

A freed specialist slot performs read-only Bugs, Security and Compliance review using `REVIEW.md`, with focused domain checks against the accepted Spec. Coordinator assigns important fixes to owners, reruns affected checks and updates the requirement evidence matrix. Capture the final Android APK, Xcode project/scheme/run recipe, selected simulator, test results, usage-report example and limitations in the implementation report. Keep raw local logs/artifacts out of Git; link their absolute paths in the handover.

Mark the track complete only when R1–R12 and both local platform milestones are met. CI configuration and run results are reported separately: unavailable macOS runners or externally blocked CI runs must be labelled, never presented as successful. Preserve existing backend/web/worker checks. No commit, push, production deployment, paid runner purchase or store submission is part of this Plan.

### Traffic definitions, retention and test oracle

Use `X-Rasika-Session-ID` and `X-Rasika-Interaction-ID` UUID metadata; bound/validate them and ignore malformed analytics fields without denying a valid catalogue read. Session IDs are created only in memory on launch or after 30 minutes of inactivity. An explicit retry of a failed action shares its interaction ID; explicit refresh/open begins a new interaction. Pagination carries the original search interaction. Server route handlers assign trusted action categories; client metadata never determines auth or whether a response succeeded.

Generate a trusted unique usage-event UUID for each HTTP attempt. A log record retains that UUID when copied or reread; deduplicate repeated report inputs by event UUID, never by session/interaction alone. Distinct transport retries retain separate event UUIDs. Record all catalogue HTTP attempts separately from successful logical actions deduplicated by environment/session/interaction/action. A search is counted once when its first page succeeds; zero-result rate uses those completed searches. A reader view is counted once when its metadata request succeeds; fetching its selected lyrics under the same interaction does not add another view. Directory operations have their own action category. Missing interaction metadata still counts requests but is reported as unattributed activity, not fabricated sessions. Count distinct valid session IDs within the requested report window; do not add daily distinct counts to infer a monthly total or call sessions unique people.

Add a dedicated non-additive JSON-lines usage logger, separate from ordinary application logs: daily/10 MiB file rotation, at most seven days and 100 MiB of archives, plus the bounded active file. Run cleanup on startup/rollover; disclose that this is operational retention, not a cryptographic deletion guarantee. Default local output is inside ignored `build/track-138/usage/`. Tag environment from trusted server configuration; the report excludes development/test by default and requires `--include-development` for the local demo. Hosted log collection is deferred with hosted deployment.

Use asynchronous bounded logging; dropped-event counts are observable, recording failure cannot fail a catalogue response, and the report declares incomplete coverage when drops are known. Do not add session/interaction IDs, raw queries or Kriti IDs as Micrometer labels. Usage records contain a server-generated event UUID, timestamps, environment, bounded route/action/status, elapsed time, validated correlation IDs and result count, with no query, lyric, token or device fingerprint.

Prevent indirect query disclosure: format public catalogue request logs without raw query strings and disable parameter-expanding SQL debug/slow-query logging in the API's default runtime connection settings in `API/App.kt`, retaining query counters and application timing. Document that development SQL-value logs become unavailable by default; do not redesign `DatabaseFactory.dbQuery` or transaction nesting solely for analytics. Check representative error logs for raw public input as part of the logging test/review.

Add a deterministic report fixture across two sessions: one zero-result search (one request); a second search with a failed first-page attempt, successful retry and successful next page (three requests); a reader metadata failure and successful retry (two requests); its lyric fetch (one request); and a directory request (one request). Expected counts are eight request attempts, two successful logical searches, one zero-result search (50%), one reader view and two approximate sessions. Test invalid IDs, duplicate log inputs, report-window boundaries, inactivity/new sessions, recording failures and development exclusion separately. Report output is JSON plus a small human-readable summary; no analytics dashboard or external provider is required.

### Requirement-to-proof matrix

| Requirement | Automated proof | Runtime / review evidence |
|:---|:---|:---|
| R1 | Both host builds; shared tests; native journey suites | Installed Android APK then running iOS simulator host |
| R2 | CatalogueSearchTest, CatalogueRoutesTest, Search/BrowsePresenterTest | Direct and alias search, Raga/Composer navigation, combined filters and paging |
| R3 | PublicVisibilityTest; reader ownership/withdrawal tests; admin client test | Anonymous public read and authenticated curator draft search/edit |
| R4 | Secondary/repeated membership, homonyms, literal wildcards, distinct count and tied-title page tests | Stored raga identity/context and ordered ragamalika display |
| R5 | CatalogueReaderTest and KrithiReaderPresenterTest | Selected source reading, exact stored sections and musical-form labels |
| R6 | Same-script distinct variants, unavailable script and completeness-unknown cases | Available stored script rendering and large-text checks on both platforms |
| R7 | RasikaNavigatorTest and presenter state tests | Per-tab/back behavior, system/light/dark theme, text size and restart |
| R8 | LocalSettingsCodecTest, FavouritesPresenterTest, native store checks | Storage inspection after use/restart; favourite reopening reaches API |
| R9 | MockEngine request count, cancellation/late-response and interaction tests | Network traces for user actions versus recomposition/back rendering |
| R10 | MobileSessionTest, CatalogueUsageRecorderTest, Python report tests | Deterministic eight-attempt report and separate live usage example |
| R11 | Timeout/error/404/stale-response tests | Airplane/network interruption, retry and unavailable-record flows |
| R12 | URL validation and native transport configuration checks | Debug connectivity, release URL validation, package/dependency review |

### Proof commands and evidence rules

The existing commands below are verified from repository build files. Run them during implementation, not as a substitute for preparing this Plan:

```bash
make test
make test-integration
./gradlew :modules:backend:api:build
make test-frontend
make check-docs
```

From `modules/frontend/sangita-admin-web`, additionally run `bun run typecheck`, `bun run test:unit` and `bun run build`. `make test-frontend` currently invokes `bun test`, whereas the frontend's configured unit runner is Vitest; run the canonical Make command and the explicit script, record any mismatch/failure, and correct a demonstrated orchestration mismatch in the coordinator-owned Makefile rather than hiding it. `make test` already includes integration tests, but the explicit integration command remains required by the Spec.

The following are **planned commands**, not claims of existing registrations or passed builds. C must verify them and put exact replacements into this Plan/Make wrappers if the toolchain registers different tasks:

```bash
./gradlew :modules:shared:mobile-data:tasks --all
./gradlew :modules:shared:presentation:tasks --all
./gradlew :modules:mobile:androidApp:tasks --all
./gradlew :modules:shared:mobile-data:jvmTest
./gradlew :modules:shared:presentation:iosSimulatorArm64Test
./gradlew :modules:mobile:androidApp:assembleDebug
./gradlew :modules:mobile:androidApp:connectedDebugAndroidTest
./gradlew :modules:shared:presentation:linkDebugFrameworkIosSimulatorArm64
xcodebuild -list -project modules/mobile/iosApp/RasikaApp.xcodeproj
xcodebuild -showdestinations -project modules/mobile/iosApp/RasikaApp.xcodeproj -scheme RasikaApp
python3 -m unittest discover -s tools/tests -p test_catalogue_usage_report.py
```

The iOS wrapper runs `xcodebuild build` and `xcodebuild test` with that project/scheme, Debug configuration, `-destination 'platform=iOS Simulator,id=<discovered UUID>'`, isolated DerivedData and `.xcresult` paths. Record actual test counts; zero executed tests is not a pass for the corresponding requirement. Use `adb install -r <actual APK path>` and native test/launch commands against the selected emulator; archive evidence paths after execution.

Before live journeys, run `make dev-down`, then start the named `full-stack` launch configuration (`make dev`) and verify `http://localhost:8080/health` and `http://localhost:5001`. Serialize this operation under the coordinator. If a Flyway index migration becomes necessary, use `make migrate`/`make migrate-status` and the existing migration tests; never `make db-reset` or `make clean` as verification. Use the verify-import skill if any approved data/lyric-structure mutation is later introduced, rather than treating a read-only reader change as a bulk import.

Finish with `git diff --check` and `make check-docs`. Run `make agent-evals` only if agent configuration changes; this Plan does not require such changes. Record command exit status, material output, test counts, artifact paths and platform destination in the implementation report. Do not claim unexecuted commands passed.

### Risks, rejected options and handling

| Risk / option | Decision and response |
|:---|:---|
| Highest integration risk: stricter public reads break curator access | Change server admin routes, public boundary and client routing as one tested package; include draft editor and import-approval/publication regression |
| Highest mobile risk: unproven AGP/Kotlin/Native/Xcode combination | Early two-host spike; resolve only demonstrated incompatibilities; do not postpone iOS linkage until after all Android UI work |
| Missing published corpus examples | Read-only inventory; fixture coverage plus explicit live-data limitation; never relax publication or fabricate musical content |
| API count/order or alias ambiguity errors | Distinct ID paging, batch hydration, exact UUID filters, published counts and identity-aware fixtures; do not call mutating resolvers |
| Public logging leaks query data or yields misleading visits | Dedicated bounded usage events, query-safe request logs, SQL-value debug logging disabled by default, report-window deduplication and drop visibility |
| Native OS cache violates online-only intent | Disable persistent HTTP caches explicitly; inspect actual Android/iOS storage after reader use, not only bookmark serialization |
| Shared files/build processes collide across agents | Coordinator owns contracts/Gradle/CI/track; disjoint source-set writers; serialize stack/device/build resources where needed |
| Full offline store / LangGraph / semantic features | Rejected for this MVP: unnecessary dependencies and conflict with accepted server-fetched conventional search scope |
| Rewrite ingestion or normalize musical identities while building reader | Rejected: catalogue reader must preserve stored identity/readings; report curation issues separately |
| New navigation or DI framework | Rejected for the small fixed screen set; typed stack and constructor injection keep implementation/test surface small |
| Unavailable hosted CI simulator, signing or remote API | Record exact external limitation; finish independent local work; no paid infrastructure or store submission implied |

### Acceptance requested

Accepting this Plan authorizes the coordinator and bounded specialists to implement the listed MVP, run local builds/tests and required stack restarts, repair important findings, update execution evidence and deliver Android/iOS simulator artifacts. It does not authorize commits, remote deployment, data publication/reset, paid services or store distribution. No unresolved product-choice question remains before acceptance; technical compatibility and corpus readiness are explicit early execution checks.


## Execution graph ledger

This is the coordinator's durable execution state. The file-level Plan above is Draft; the graph and scheduling rules are in the linked build guide. Dispatch product work only after Plan acceptance. Update this table in place rather than keeping conflicting copies in agent conversations.

| Node | State | Owner | Evidence / next condition |
|:---|:---|:---|:---|
| G0 — Intent | Done | Coordinator | Seshadri's explicit acceptance, 2026-09-05 |
| G1 — Spec | Done | Coordinator | Seshadri explicitly accepted the Spec, 2026-09-05 |
| G2 — Plan | Done | Coordinator | Seshadri authorized implementation by requesting the build, 2026-09-05 |
| C — Contracts and build foundation | Done | Coordinator | DTOs/OpenAPI, mobile-data + presentation + androidApp/iOS framework. Proof from 2026-09-05 first slice. |
| B — Public backend and usage reporting | Verify | Coordinator | Catalogue API + visibility + `CatalogueServiceTest` (8). Admin web `searchKrithis`/`getKrithi` now hit `/admin/krithis/*` (`client.test.ts` 2). Live usage sample from Android session: 12 attempts / 3 searches / 2 reader views / 3 approx sessions. Dedicated JSONL file appender still console-only. |
| M — Shared mobile data and UI | Verify | Coordinator | Live hosts + fixture tests. Presenter suite: Search 3, Browse 3, Reader 5, Favourites 3, Navigator 3. `LocalSettingsCodecTest` already present. Compose chrome rebuilt to `track-138-visual-design.md` tokens (custom tab bar, paperRaised fields, saffron chips, bookmark cards). HTML prototypes + `trinity.png` were not in the tree at alignment time. |
| H — Native hosts | Verify | Coordinator | Android APK launches after FQCN manifest fix. iOS `Rasika.app` **BUILD SUCCEEDED** for `generic/platform=iOS Simulator`. Device-id `xcodebuild` fails: Xcode 26.6 wants iOS 26.5 runtime (installed: 17.0 / 26.0–26.2). |
| A — Android live integration | Verify | Coordinator | Created AVD `Rasika_API34`; installed debug APK. Live journeys: search `visva` → ragamalika `70623fa6-…` → reader (6 scripts) → Tamil stored lyrics → favourite → restart persists UUID+label only. Browse raga directory live. Gaps: 1 published kriti (no paging/combined-filter live), no airplane-mode, no instrumented `RasikaJourneyTest`. |
| I — iOS live parity | Verify | Coordinator | iOS 26.5 runtime now installed. `Rasika.app` **BUILD SUCCEEDED** for `iPhone 17` `C0104896-…` (not generic-only). Launch required `CADisableMinimumFrameDurationOnPhone` in Info.plist. Live journeys: search → reader (`viSva nAthaM bhajEhaM`, Devanagari Pallavi), Browse raga directory, Preferences chips. Tamil chip + bookmark HID taps were unreliable; favourite/restart persistence not evidenced on iOS yet. Screenshots in `build/track-138/ios-screens/` (gitignored). |
| Q — Independent final review and repair | Pending | Unassigned | Requires verified A and I |
| D — Delivery | Pending | Coordinator | Requires Q, complete R1–R12 evidence and actual artifacts |

## Progress Log

- **2026-09-05:** Created track at the user's request; inspected mobile scaffolding, public APIs and local tool availability. Recorded three confirmed product preferences and prepared the detailed analysis. No app/backend code changes, builds, deployments or commits performed.
- **2026-09-05:** Incorporated the user's clarification: catalogue/lyrics stay on the server, favourites are lightweight local bookmarks, and real server usage/visits must be measurable. Added proposed instrumentation and acceptance scenarios to the analysis.
- **2026-09-05:** Seshadri explicitly accepted the Intent and the analysis's “Decisions and remaining questions” recommendations. Recorded acceptance and prepared a Draft Spec. Formal Plan and application implementation remain pending the required subsequent acceptance gates.
- **2026-09-05:** Confirmed build-graph semantics; completed the required read-only domain review and incorporated its four contract clarifications. Prepared the agent coordination guide, dependency graph, completion ledger and reusable kickoff/resume prompt. No application build loop, scheduler or product implementation has been started.
- **2026-09-05:** Documentation validation passed: `make check-docs` reported “doc links OK — every relative Markdown link resolves”; temporary Git metadata included the new documentation without changing the real staging state. `git diff --check` passed. No product tests/builds were run for this documentation-only setup.

- **2026-09-05:** Seshadri explicitly accepted the Spec. Completed read-only backend/mobile specialist planning and repository inspection; prepared the concrete Draft Plan, file ownership, dependency gates, telemetry definitions, R1–R12 evidence matrix and proof commands. No product implementation or builds performed. Independent Plan review clarified per-attempt event IDs and server-default versus client-preferred reading selection.

- **2026-09-05:** Draft Plan documentation validation passed: `make check-docs` reported “doc links OK — every relative Markdown link resolves”; new documents were included through temporary Git metadata with real staging unchanged. `git diff --check` passed. Product checks listed in the Plan remain unexecuted until implementation.

- **2026-09-05:** Seshadri requested the TRACK-138 build to start (agentic guide + visual design). Plan recorded as Accepted on that instruction. Node C first slice: catalogue DTOs/OpenAPI, `mobile-data` + presentation UI + Android host, Rasika theme/navigation/Search-Browse-Favourites-Reader against fixtures. `mobile-data`/`presentation` jvmTest pass; Android `assembleDebug` produces `androidApp-debug.apk`; iOS `linkDebugFrameworkIosSimulatorArm64` and Xcode scheme list succeed. No commit.

- **2026-09-05:** Node B + wire M: live `/v1/catalogue` (published-only, junction ragas, paging, Cache-Control no-store). Legacy public search/detail hide drafts unless admin JWT. Hosts swapped to `KtorCatalogueApi`. `make test` passed; catalogue/visibility/usage tests passed; `assembleDebug` and iOS framework link passed. Usage report script + recorder landed. Stack restarted; live catalogue smoke 200 against 1 published ragamalika. Node A device journeys blocked: no Android emulator/AVD attached. No commit.

- **2026-09-05:** Continued after B/M: `CatalogueServiceTest`, reader/favourites presenter tests, admin client route migration + Vitest. Created AVD `Rasika_API34`; fixed Android activity FQCN (`com.sangita.grantha.mobile.MainActivity`). Live Android search/reader/Tamil/favourite/restart/browse against current backend. iOS `Rasika.app` generic-simulator build succeeded; device destinations blocked on missing iOS 26.5. CI jobs added for mobile JVM/Android/iOS. No commit.

- **2026-09-05:** Visual alignment pass. Replaced Material 3 stand-in chrome with concert-programme tokens from `track-138-visual-design.md` (paper/saffron/goldLine, custom tab bar, paperRaised search fields, saffronSoft chips, bookmark affordance, reader display type). `application_documentation/05-frontend/mobile/design/prototype.dc.html`, `screens.dc.html`, and `composeResources/drawable/trinity.png` were not present in the workspace, so HTML layout/trinity artwork could not be pixel-matched. No commit.

- **2026-09-05:** Node I unblocked. iOS 26.5 (`23F77`) + iPhone 17 `C0104896-CD64-4533-A919-1E5F22036561`. `verify-ios.sh` now builds a concrete 26.5 destination (generic fallback removed). First launch crashed until `CADisableMinimumFrameDurationOnPhone` was added to `Info.plist`. Live Search → reader and Browse directory against `127.0.0.1:8080`; Preferences reachable. Tamil/favourite HID taps not reliable enough to count as journey evidence. No commit.

Ref: application_documentation/01-requirements/mobile/rasika-mvp-analysis.md
