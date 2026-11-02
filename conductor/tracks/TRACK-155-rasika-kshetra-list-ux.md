| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-02 |
| **Author** | Sangeetha Grantha Team |

# Track: Rasika Kshetra List UX

**ID:** TRACK-155
**Status:** Completed (PR #59; follow-ups listed in the Progress Log)
**Owner:** Seshadri
**Created:** 2026-11-01
**Updated:** 2026-11-02

## Goal

Make the Rasika kshetra screens behave well with real data: the Kshetras list keeps its place after back navigation, and the Tiruvarur detail (114 kritis) scrolls like a normal list.

## Context

- **Reference:** [TRACK-151](TRACK-151-rasika-kshetra-exploration.md) (Rasika kshetra exploration, Completed). Its Progress Log lists both defects under "Findings, not yet fixed". Its Spec flagged "Kriti list size" and said a page parameter must be decided before the contract is fixed; see Spec item 8 for the decision this track records.
- **Code:** `modules/shared/presentation/.../kshetra/KshetraScreens.kt`, `.../RasikaApp.kt` (navigation host), `.../navigation/RasikaDestination.kt`. Conventions: `kmp-compose-mobile` skill.
- **Out of scope here:** TRACK-156 (Tamil lyric data, includes the stray "3" glyphs) and TRACK-157 (search follow-ups).

## Intent

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-11-02

### Problem

Found while walking TRACK-151 in the iOS simulator (iPhone 17, iOS 27.2):

1. On the Kshetras list (84 root temples), opening a temple and pressing back puts the list at the top again. The rasika loses their place in a long list.
2. The Tiruvarur Tyagarajaswami Temple detail (`01930000-0000-7000-8000-000000000001`: 114 direct kritis, 10 child shrines) is one very long list, and the scroll keeps moving long after a swipe has ended.

### Proposed outcome

1. Back from a temple (or from a kriti opened in a temple) returns to the same scroll position, with the element filter unchanged.
2. The Tiruvarur detail scrolls smoothly and stops when the finger stops; no kriti is hidden or reordered.

### Affected users and systems

Rasika users on iOS and Android. Module `modules/shared/presentation` only, unless Spec item 8 chooses paging (then the V2 temple detail endpoint, DTOs and OpenAPI as well).

### Constraints

- V2 only; no V1 change. No migration. No lakshana or seed data touched.
- Match how other Rasika lists behave (see Spec item 2: today they share defect 1).
- Accessibility kept: headings, content descriptions, minimum targets as in TRACK-151.
- Plan must be Accepted before any application code changes.

### Open questions

1. Fix only the kshetra screens, or the shared cause for every Rasika list (Spec item 4)?
2. Is "keep scroll position and filter on back" also wanted when the user re-opens the list from the Home shelf (a fresh visit), or only on back? Default proposed: only on back.

## Spec

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-11-02

Written from code reading and a live database query on 2026-11-01. The simulator was not driven in this drafting session, so the cause of defect 2 is a code-based hypothesis; Plan step 1 measures it before any change.

### Findings

1. **Defect 1 cause (confirmed by code).** The presenters are hoisted in `RasikaApp` (`remember { KshetraListPresenter(...) }`), so the loaded list and the element filter survive back navigation. The scroll position does not: `KshetrasScreen` uses `Modifier.verticalScroll(rememberScrollState())` inside `LoadStateContent`, and `RasikaApp` renders destinations with `AnimatedContent`, whose `contentKey` for `Kshetras` is the class name. Navigating to `KshetraDetail` removes the list from composition, so its `ScrollState` is discarded; on back, a new `ScrollState` starts at 0.
2. **Other lists share it.** `CollectionsScreen` (`verticalScroll(rememberScrollState())`) and `BrowseScreen`/`FavouritesScreen`/`SearchScreen` (`LazyColumn`, unhoisted state) are built the same way inside the same `AnimatedContent`, so they should reset too. Plan step 1 confirms this in the simulator. There is therefore no existing "good" list to copy; the fix sets the convention.
3. **Defect 2 cause (hypothesis).** `KshetraDetailScreen` is a plain `Column` with `verticalScroll`, not a lazy list: all 114 `KritiRow`s (each with `RasikaPressable`, semantics and a `RagaSequence`), the 10 child rows and the facts compose and lay out at once. That is a cost on open and for every re-measure. The "keeps moving" symptom may instead be the iOS fling deceleration over a very long content height rather than a frame drop. Step 1 measures both (frame times or a profiler trace, and a screen recording) and the fix is judged against the measurement.
4. **Detail loses its position too (predicted, same cause).** Opening a kriti from the Tiruvarur detail and pressing back should reset the detail to the top, after scrolling past up to 114 rows. Step 1 verifies; the fix covers it.
5. **Size is bounded.** Live query (published kritis grouped by `temple_id`): only 1 temple has 50 or more direct kritis, the maximum is 114 (Tiruvarur), the 90th percentile is 10. Payload size and server time for the Tiruvarur detail are not measured yet (no backend was running on 8080); Plan step 1 records them.

### Requirements

1. **List position.** After opening any temple from the Kshetras list and pressing back (or the system back gesture), the list shows the same scroll offset it had, on iOS and Android. The element filter selection is unchanged (already true).
2. **Filter change.** Selecting or clearing an element filter scrolls the list to the top, because its content changed.
3. **Detail position.** After opening a kriti or a child shrine from a temple detail and going back, that detail shows the same scroll offset.
4. **Scope of the shared cause (decision needed).** Option A, recommended: fix only the kshetra list and detail by hoisting their scroll state to `RasikaApp` (a `ScrollState` for the list; a `LazyListState` for the detail), passed as parameters with a default so previews and tests still work. Smallest change; touches `KshetraScreens.kt` and `RasikaApp.kt`. Option B: wrap the `AnimatedContent` content in a `SaveableStateHolder` keyed by `contentKey`, which restores `rememberSaveable` state (including `rememberScrollState` and `rememberLazyListState`) for every Rasika screen, and remove saved state when a destination is popped. Fixes Collections, Browse and others at once, but changes every screen and needs a leak/reset rule (stack depth is bounded at 8). Recommendation: A in this track; B recorded as a separate follow-up if Step 1 confirms the others reset.
5. **Detail rendering.** `KshetraDetailScreen` renders its content as one `LazyColumn`: header items (overview, map link, parent, facts), the shrine rows, then `items(detail.krithis, key = { it.id.toString() })`. Visual design, copy, order, headings semantics and content descriptions are unchanged. Only visible rows are composed.
6. **Deterministic deceleration.** After requirement 5, a swipe on the Tiruvarur detail stops within normal platform fling behaviour. If step 1 shows the fling is the platform default and not caused by composition cost, the Spec is revised before implementation (no speculative fix).
7. **No data loss.** The detail still shows all 114 kritis in the same order as the API (title, then id), and the 10 shrines.
8. **Paging decision (resolves the TRACK-151 flagged concern).** No page or limit parameter is added to `GET /v2/catalogue/temples/{id}`. Reason: the measured maximum is 114 rows for one temple; with a lazy list, cost scales with what is on screen, not with row count; changing the contract would affect TRACK-152 and TRACK-153 consumers and OpenAPI for no measured gain. Revisit trigger: any temple above about 300 direct kritis, or a measured payload above about 250 KB, after which a page parameter is proposed in a new track. If step 1 shows payload size or server time is the real cost, this item is reopened and the Plan changes.
9. **Rejected:** client-side "show more" truncation of the 114 kritis (hides data, adds a tap), pagination now, a new endpoint, moving state into a presenter (scroll offset is a view concern and does not belong in presenters).

### Design

- `RasikaApp` creates `val kshetraListScroll = rememberScrollState()` and `val kshetraDetailScroll = rememberLazyListState()` next to the kshetra presenters and passes them to the screens. Because `RasikaApp` stays in composition for the whole session, the states outlive the `AnimatedContent` content.
- A single `LazyListState` is shared by every `KshetraDetail` destination, which would carry one temple's offset into another (parent to child and back). So the detail state is kept per temple id: `remember { mutableStateMapOf<Uuid, LazyListState>() }` filled on first visit and capped by the bounded stack depth, or the offset is saved on leave and restored on return (Plan chooses; a test pins that opening temple B does not start scrolled).
- The element-filter handler in `KshetrasScreen` calls `scrollTo(0)` in a `LaunchedEffect(state.element)` skipped on first composition.
- `LoadStateContent` already wraps the content slot; the lazy list replaces the inner `Column` and keeps `Modifier.fillMaxSize()`. Section labels become full-width `item`s (key-prefixed strings) so `heading()` semantics stay.
- No change to presenters, repository, DTOs, backend, OpenAPI, copy or tokens.

### Flagged concerns

- **iOS scroll behaviour in Compose Multiplatform.** Fling and bounce are platform-driven; a lazy list may not change the "keeps moving" feel if it is the default deceleration. Hence requirement 6 and the measurement-first step.
- **Per-temple state growth.** Bounded by stack depth 8 and by clearing on return to the tab root; the Plan names the rule.
- **Back stack collapse.** `RasikaNavigator.open` ignores a repeated destination and caps depth at 8; no change, but the tests exercise Kshetras to Detail to Detail to back to back.
- **Accessibility.** Lazy composition must not drop headings or descriptions of rows that are off screen for screen readers in a way that breaks navigation; Plan includes a VoiceOver/TalkBack pass on the Tiruvarur detail.
- **No lakshana or data change**; `carnatic-musicologist` review not needed.

### Open questions carried forward

Intent 1 (scope A or B) and Intent 2 (back only, or also fresh visits). The Spec assumes A and back-only until answered.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-11-02

Accepted 2026-11-02; implementation may start. Open questions are answered as the Spec assumed (Option A, kshetra-only; back-only restore).

### Files that change

| Path | Change |
|:---|:---|
| `modules/shared/presentation/.../kshetra/KshetraScreens.kt` | `KshetrasScreen` takes a hoisted `ScrollState`; filter change resets it. `KshetraDetailScreen` becomes a `LazyColumn` with a hoisted/per-temple `LazyListState`; stable keys |
| `modules/shared/presentation/.../RasikaApp.kt` | Own the scroll states beside the kshetra presenters and pass them down |
| `modules/shared/presentation/src/commonTest/.../kshetra/KshetraScrollStateTest.kt` (new) | Per-temple state holder tests (see Tests) |
| `conductor/tracks/evidence/TRACK-155/` (new) | Step 1 measurements and simulator evidence notes |
| `conductor/tracks.md` | Status only |

No backend, DAL, OpenAPI, migration, domain DTO, or Python change (unless step 1 reopens Spec item 8).

### Order of work

1. **Measure first (no code change).** `make mobile-ios`; start the backend only if none answers on 8080 (the main stack database `sangeetha-grantha-db-1` is on 5432; do not stop a backend this work did not start). On iPhone 17 / iOS 27.2: record the baseline for (a) list back-navigation reset, (b) Collections and Browse back-navigation reset, (c) Tiruvarur detail back from a kriti, (d) Tiruvarur open time and fling behaviour (screen recording plus frame time or Instruments trace), (e) `curl` payload size and time for the Tiruvarur detail. Write results to the Progress Log and `evidence/TRACK-155/`. Revisit the Spec if (d) or (e) contradict the hypothesis.
2. **Hoist scroll state** for the list (requirements 1, 2) and a per-temple holder for the detail (requirement 3); presenters untouched.
3. **Lazy detail** (requirements 5 to 7).
4. Tests, then the simulator walk-through, then docs and track close.

Steps 2 and 3 can land as separate commits; step 1 first.

### Tests

- **Presenter tests (existing, must stay green):** `KshetraPresentersTest` already pins that the list loads once and the filter survives a refresh; add one case that `loadIfNeeded()` called again after re-entering the screen does not refetch and keeps `element`.
- **Unit tests (new, common):** `KshetraScrollStateTest` covers the per-temple state holder: opening temple A, scrolling, opening B starts at the top, returning to A restores A's offset, and the holder stays within the stack-depth bound.
- **UI tests:** `modules/shared/presentation` has no Compose UI-test dependency today. The Plan adds `compose.uiTest` for the host/JVM target and a test that composes `KshetrasScreen` with a fixture list, scrolls, simulates navigating away and back via `RasikaApp` with the `RasikaUiTestHarness` fake API, and asserts the offset; and a Tiruvarur-fixture test that only a bounded number of `KritiRow`s are composed at once. If the dependency cannot be added to this module without a version-catalog change, the Plan stops and asks (versions go through `gradle/libs.versions.toml` and `current-versions.md`), and the simulator walk-through below becomes the proof for the UI behaviour.
- `make test`, `./gradlew :modules:shared:presentation:allTests`, Android assemble per CI, `make check-docs`, `git diff --check`.

### Simulator evidence required (iPhone 17, iOS 27.2, Xcode 27.2, `make mobile-ios`)

Each as a short recording or paired screenshots saved under `conductor/tracks/evidence/TRACK-155/`, with the commands used:

1. Kshetras list: scroll to the middle (for example the Pandya group), open a temple, back: same row at the same position. Repeat with the element filter active, and with the swipe-back gesture.
2. Select and clear an element filter: list returns to the top.
3. Tiruvarur detail: scroll to the 60th kriti, open it, back: same position; open a child shrine, back: same position; open a different temple: starts at the top.
4. Swipe test on Tiruvarur: one swipe, the scroll stops within normal fling time (before and after recording; frame-time figures from step 1 versus after).
5. VoiceOver pass on Tiruvarur detail: headings, a kriti row's description, and the shrine rows are reachable.
6. Collections and Browse behaviour recorded as unchanged (or fixed, if Option B is chosen).

### Risks

- The "keeps moving" symptom is platform fling, not composition cost; mitigated by measuring first and by Spec requirement 6.
- A shared `LazyListState` leaks one temple's offset into another; mitigated by the per-temple holder and its test.
- Hoisting into `RasikaApp` grows an already large function; acceptable for two states, noted if Option B is later chosen.
- `AnimatedContent` crossfade over a `LazyColumn` could flash an empty list on return; the simulator walk-through (item 1) checks it.

### Proof (completion evidence, fixed here)

Step 1 baseline numbers, the six simulator items above, the new unit and UI tests green, `make test` output, `make check-docs`, and CI green on the PR.

## Implementation Checklist

- [x] Intent accepted
- [x] Spec accepted
- [x] Plan accepted
- [x] Step 1 baseline measured and logged (defect 1 reproduced; payload measured; frame-time trace and Collections/Browse baseline not captured, see log)
- [x] List scroll state hoisted; filter resets to top
- [x] Per-temple detail scroll state
- [x] Detail converted to `LazyColumn`
- [x] Tests and simulator evidence (VoiceOver pass not done)
- [x] Track closed

## Progress Log

- **2026-11-01**: Track created from two findings in TRACK-151's Progress Log. Intent, Spec and Plan drafted from code reading (`KshetraScreens.kt`, `RasikaApp.kt`, `RasikaDestination.kt`) and one database query (1 temple with 50 or more direct published kritis; maximum 114; 90th percentile 10). No simulator run, no application code changed. Waiting for Seshadri to accept Intent, then Spec, then Plan.
- **2026-11-02**: Intent, Spec and Plan accepted by Seshadri (relayed in chat); status In Progress. Implementation started with the baseline measurement.
- **2026-11-02**: Implemented. `KshetraScrollStates` (new, owned by `RasikaApp`) holds the list `ScrollState` and one `LazyListState` per temple, and `sync()` drops positions whose screen has left the stack, so only back restores. `KshetrasScreen` takes the hoisted state and scrolls to the top when the element filter changes. `KshetraDetailScreen` is now one `LazyColumn` with stable keys; copy, order and semantics are unchanged. No presenter, backend, OpenAPI or data change.
- **2026-11-02 baseline**: `GET /v2/catalogue/temples/{Tiruvarur}` = 44,169 bytes in 85 ms (warm, one request), so the payload and server are not the cost; the Spec 8 decision (no page parameter) stands. Before the fix, back from a temple put the list at the top (screenshots in `evidence/TRACK-155/`).
- **2026-11-02 after**: back restores the list (a row opened mid-list and returned to the same place), the Tiruvarur detail (scrolled deep into its 114 kritis, kriti opened, back, same place within 2 px), a list restored again after back from Tiruvarur, a fresh visit from Home starts at the top, filter on then cleared returns to the top. Tests: `KshetraScrollStatesTest` (6) and one new `KshetraPresentersTest` case; `make test` and `make test-mobile` pass.
- **Not done, and why**: (1) no before/after frame-time trace of Tiruvarur and no direct measurement of the "keeps moving after a swipe" symptom: synthetic swipes in the simulator do not reproduce a human fling, so Spec requirement 6 (stops within normal fling) is not proven; the lazy list removes the composition cost but if the long deceleration is the platform default it will still be there on a device. (2) No VoiceOver pass. (3) No Collections/Browse baseline; by the same cause they should reset on back and are left unchanged (Option B follow-up). (4) No Compose UI test: the module has no `compose.uiTest` dependency and adding one needs a version-catalog change, which the Plan said to ask about first; the holder logic is unit tested and the walk-through covers the rest. (5) The detail screen still reloads the temple when re-entered (skeleton flash) because `KshetraDetailPresenter.open` always refetches; position is restored regardless. Also seen, unrelated: Explore search showed "Could not reach the catalogue" for "Tiruvarur" while the temple endpoints answered.
- **2026-11-02**: PR #59 merged after all 11 CI checks passed (including the Android debug APK and iOS simulator host build). Track completed. Open follow-ups: confirm the post-swipe feel on a real touch, a VoiceOver pass, and the same scroll-on-back fix for Collections and Browse (Spec item 4 Option B).
