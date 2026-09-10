| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Rasika — mobile product requirements

---

Rasika helps listeners, students, performers, and teachers find Carnatic compositions and read their stored lyric variants with clear source context. It is the public Android/iOS client for Sangeetha Grantha; the Curator Console owns catalogue changes.

This PRD describes the current discovery/reading release and separates later product requirements. [TRACK-138](../../../conductor/tracks/TRACK-138-rasika-mobile-app.md) established the mobile foundation; [TRACK-140](../../../conductor/tracks/TRACK-140-rasika-discovery-experience.md) governs the new experience and acceptance gate.

## 1. Product principles

- Put search and readable text within easy reach.
- Preserve the identity of the chosen script, source, and lyric variant.
- Show incomplete or unavailable content honestly.
- Keep ordered raga membership and meaningful section labels intact.
- Make local bookmarks useful without requiring a public account.
- Support large text, reduced motion, and assistive technology through explicit verification.

## 2. Navigation and current scope

| Tab | Reader's purpose | Current behavior |
|:---|:---|:---|
| **Home** | Begin discovery | Search entry independent of discovery loading; available feature or collection content |
| **Explore** | Find compositions and related entities | Compositions/Ragas/Composers under one submitted query; UUID filters and paging |
| **Library** | Return to saved works | Device-local bookmarks persisted across restart |
| **Settings** | Adjust reading environment | System/Light/Dark appearance with persistence |

The implementation exists in shared Compose UI and native hosts. Native runtime journeys, TalkBack, and VoiceOver remain acceptance obligations; code presence is not store-release certification.

## 3. Search, browse, and entity pages

The user submits a query explicitly. Explore must maintain a coherent query while switching among compositions, ragas, and composers. Composer/raga filters use exact IDs; draft/apply/cancel/reset interactions must not silently commit unintended changes. Removing an applied filter chip commits that removal.

Paging must preserve loaded items when a later page fails and offer recovery. Opening a raga/composer must use the correct entity detail and link to works through the exact ID, not a display-name approximation.

Raga pages may show aliases, melakarta/parent lineage, scales, and nomenclature links where supplied. Composer pages may show aliases and biographical metadata where available. Directory counts reflect published compositions visible under the current contract.

**Later scope:** tala, deity, temple, language, and musical-form directories, richer thematic navigation, and conversational/semantic mobile experiences. Their presence in OpenAPI or DTO definitions is not evidence of a mounted V2 route or current Explore tab.

## 4. Source-faithful reader

The reader obtains metadata and available variants, then loads one selected variant. Display the selected language/script and available source/variant label. Multiple variants in the same script must remain distinguishable.

During a variant switch, keep the previous text labeled until replacement lyrics load successfully. Preserve its state on failure. If the composition has no readable variant, present an informative empty state rather than fabricated text. If the composition becomes unavailable, handle that explicitly.

Use stored section labels before fallback section-type labels. Show per-section raga association only when the response carries a real section link. A Ragamalika composition's ordered raga list is not enough to guess which raga belongs to each section.

`UNESTABLISHED` means the composition's form has not been established. Show no guessed form badge. The public reader does not display editorial notes, internal author IDs, or workflow state.

**Outside the current reader:** audio playback, live transliteration generation, annotations, notation rendering, tala animation, and audio/text synchronization.

## 5. Library and persistence

Bookmarks are local to the device. Saving/removing a bookmark should be reflected in Library and survive restart. Stored summaries help identify saved works; opening one still respects current catalogue availability.

The local document store serializes writes and protects corrupt/future-version documents from silent overwrite. Persistence failures must be surfaced and UI state rolled back where appropriate.

**Not promised:** a complete offline lyric cache, offline catalogue packs, cross-device sync, collections, or recently viewed history. Those are later releases, not consequences of having local bookmarks.

## 6. Appearance and accessibility

Appearance supports System, Light, and Dark. Large text must wrap navigation/chrome and keep content and actions usable. Reduced motion suppresses tab crossfade, press scale, and loading shimmer.

Shared tests and native automation validate supported state/semantics. They do not replace device execution or manual TalkBack/VoiceOver use. Acceptance includes navigation, search, paging, reader/variant switching, bookmark restart, large text, and reduced motion on the native hosts. See [implementation evidence](../../10-implementations/track-140-rasika-discovery.md).

## 7. Public data and transport

Rasika uses `/v2/catalogue` exclusively for its current catalogue client. Core routes cover discovery, compositions, composition lyrics, ragas, and composers. Successful responses carry `Cache-Control: no-store`.

V2 includes published `UNESTABLISHED` compositions; catalogue V1 retains its older exclusion. Public reads use allowlisted catalogue DTOs rather than editorial `KrithiDto`. List parameters are `query`, `composerId`, `ragaId`, zero-based `page`, and `pageSize` where applicable. Unsupported/repeated parameters are rejected.

The app requires no public user account. Release transport uses HTTPS; explicit debug configuration permits emulator/simulator local HTTP. See [API contract](../../03-api/api-contract.md) and [mobile implementation guide](../../05-frontend/mobile/README.md).

## 8. Technology and ownership

| Layer | Responsibility |
|:---|:---|
| Kotlin Multiplatform + Compose Multiplatform | Shared application and UI |
| `modules/shared/domain` | Public catalogue DTOs and shared domain types |
| `modules/shared/mobile-data` | Ktor V2 client, repositories, local storage, fixtures |
| `modules/shared/presentation` | Presenters, screens, navigation, accessibility semantics |
| `modules/mobile/androidApp`, `modules/mobile/iosApp` | Native hosts, platform configuration, native journey tests |
| Backend Ktor + PostgreSQL | Published catalogue and discovery |
| **Flyway Community** | Backend schema/reference migrations; not an on-device migration runner |
| GitHub Actions | Shared tests and native build checks |

Dependencies are listed in [Current Versions](../../00-meta/current-versions.md). Mobile bookmark storage and server-side Flyway migrations are separate responsibilities.

## 9. Acceptance and measurement

| Outcome | Evidence |
|:---|:---|
| Find a work | Known title and alias searches; exact filters; empty and failed requests |
| Browse further | Multi-page results, retry on page failure, correct entity-to-works navigation |
| Read faithfully | Stored labels, source/script selection, incomplete content, honest variant-loading state |
| Return later | Save → Library → restart → open → remove |
| Read accessibly | Native large-text/reduced-motion checks plus TalkBack/VoiceOver assessment |
| Protect privacy and visibility | Public DTO allowlist, published-only reads, local persistence behavior |
| Release reliably | Shared JVM tests, Android/iOS builds, recorded native runtime journeys |

Measure search outcomes and crashes only when appropriate instrumentation and a dated dataset exist. Earlier no-result/crash targets remain targets; this PRD does not assert a measured release rate.

## 10. Deferred releases

TRACK-140 later releases include additional metadata exploration, collections/recents, and broader editorial Home experiences. Public account sync, offline packs, media, public web, and store submission require additional scope and acceptance decisions.

[Main PRD](../product-requirements-document.md) · [Rasika discovery specification](../../05-frontend/mobile/rasika-discovery-experience.md) · [Quality](../../07-quality/README.md)

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../features/README.md)
