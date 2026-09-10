| Metadata | Value |
|:---|:---|
| **Status** | Spec accepted — supporting Draft Plan |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Rasika: a living library of Carnatic music

---

This is the experience proposal for [TRACK-140](../../../conductor/tracks/TRACK-140-rasika-discovery-experience.md), building on [TRACK-138](../../../conductor/tracks/TRACK-138-rasika-mobile-app.md). Seshadri accepted the Intent on 2026-09-09 and requested the detailed Spec plus a light-mode preview and Settings toggle. Seshadri accepted the detailed Spec on 2026-09-09. The track's accepted Spec now governs behaviour, including Claude's musicological review and its integration clarifications; this document remains supporting design context, not implementation evidence. The track now contains a Draft implementation Plan with a dependency graph and a repeatable verification/review/repair loop. The concept opens Light and offers visible System/Light/Dark controls in Settings. Its sample records, local interactions, and counts illustrate the experience, not live data or implemented API capabilities. Native fresh-install appearance remains System; the existing native preference values will be reused.

## Product direction

Rasika should feel like opening a beautifully typeset music volume with a knowledgeable guide nearby. The first screen invites discovery; search gets an experienced user directly to a composition; the reader becomes quiet enough for sustained attention. The classical character comes from typography, colour, language and editorial care, with temple ornament used sparingly.

The guiding journey is **discover a work → understand its context → read its source → keep it close**. Every metadata label that represents a public entity should be a route into the library. A raga should lead to its compositions, a composition to its composer, and a composer back to a refined search without losing context.

## Current implementation: what to preserve and improve

This is a source audit dated 2026-09-09, not a fresh native runtime certification.

| Area | Observed implementation | Proposed improvement |
|:---|:---|:---|
| App shell | [RasikaApp](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/RasikaApp.kt) connects Search, Browse, Favourites, Preferences and Reader | Home entry; Explore consolidates lookup; Library retains bookmarks; Settings becomes a stable destination |
| Search | [SearchScreen](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/search/SearchScreen.kt) has query + submit; displayed count uses loaded items. [SearchPresenter](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/search/SearchPresenter.kt) stores server total but fetches only the default page | Entity categories, exact-ID filters, server totals, loading next pages and preserved search state |
| Browse | [BrowseScreen](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/browse/BrowseScreen.kt) switches between two directories and associated compositions | Real detail destinations, metadata search, in-context search refinements and platform-back parity |
| Reader | [KrithiReaderScreen](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/reader/KrithiReaderScreen.kt) has a fixed 206dp artwork header, Lyrics/Details, stored variants and section rendering | Collapsible/content-sized header, section navigation, distinct script/source controls, linked metadata and predictable reading position |
| Brand | [RasikaTheme](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/theme/RasikaTheme.kt) already supplies shared fonts/tokens; Trinity artwork now exists in resources | Evolve this identity; avoid repeating a large artwork header on every dense screen |
| Public contract | [CatalogueApi](../../../modules/shared/mobile-data/src/commonMain/kotlin/com/sangita/grantha/shared/mobile/network/CatalogueApi.kt), [DTOs](../../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/catalogue/CatalogueDtos.kt), and [routes](../../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/CatalogueRoutes.kt) support krithi/raga/composer search and detail | Add deliberate public contracts for remaining metadata and curated discovery; do not wire the mobile app to admin reference endpoints |
| Existing proof | TRACK-138 retains incomplete verification/delivery ledger entries | Carry these forward explicitly and rerun affected journeys; do not treat the track registry status as native QA evidence |

The mobile skill's description of presentation as an empty scaffold is stale: the sources above are present. Existing code is the architectural starting point. No version upgrade is proposed.

## Information architecture

| Destination | Primary job | Contents |
|:---|:---|:---|
| **Home** | Start somewhere meaningful | Search, featured composition or theme, Explore shortcuts, conditional continue-reading |
| **Explore** | Find a work or musical entity | Search + Krithis / Ragas / Composers / Other; filters; results; entity pages |
| **Library** | Return to personally relevant works | Existing favourites first; later named collections and optional reading history |
| **Settings** | Shape the reading experience | Preferred stored script, text size, system/light/dark appearance; later history controls; about/source policy |

Reader and entity pages are pushed within the originating tab. Back returns to the exact query, filters, page and scroll position. A raga filter selected through a detail page has an explicit name and removable chip. The same entity UUID is used everywhere. Native iOS back gesture and Android system back follow the same logical stack.

### Home: a welcoming landing page

At the top: a small Sangeetha Grantha wordmark and the Rasika name. Below it, a clearly labelled search field: “Search krithis, ragas, composers…” with keyboard submission. Search stays visible before any editorial content.

Use one editorial feature, not an auto-advancing carousel. Example treatment: a saffron/teal panel, a small “From the collection” label, a composition title, its composer and raga, and “Read composition.” Use the existing artwork only when its subject is appropriate and the title remains readable; do not imply that a Trinity portrait depicts every composer.

Below: a small “Explore the tradition” group linking to Ragas, Composers and Other metadata. Show “Continue reading” only after a genuine prior open, and only once its minimal-storage design is accepted. Fresh users instead see a simple invitation to browse. Empty or small published catalogues must yield a useful search/browse state without fabricated features, statistics, daily picks or endless loading placeholders.

Editorial selection must have explicit ownership: published IDs chosen by a curator through an audited workflow, or a transparently labelled deterministic catalogue selection as an interim solution. An all-catalogue fallback must not be called “curated,” “popular” or “for you.” Resolve and revalidate publication when serving a feature.

### Explore: one place to search

Keep one committed query across category changes, with independent category results and clear scope. Initial release can use existing endpoints for Krithis/Ragas/Composers. Other metadata needs its own public search capability before appearing as live functionality.

| Scope | Result identity | Refinement / next action |
|:---|:---|:---|
| Krithis | Title, composer, ordered ragas, tala, form where returned | Raga + composer first; then tala, original language, musical form, deity and temple |
| Ragas | Canonical name, matching aliases and available identity context | Open identity page; view compositions for that exact raga UUID |
| Composers | Name, aliases, available context, published count | Open profile and compositions |
| Talas | Stored name and identity; attributes only where documented | View works in the exact tala |
| Deities | Stored deity name and contextual distinctions | View associated published compositions |
| Temples / Kshetras | Stored temple/place name and location context | View associated works; preserve the distinction between a temple and a geographical place |
| Languages | Original composition language | Filter works; do not confuse language with available display script |
| Musical forms | Krithi, Varnam, Swarajathi and only other supported values | Filter while retaining form-specific section conventions |

Use a filter sheet with an explicit Apply action, Reset, and active chips above results. Prefer one exact selection per facet initially; combine facets with AND. Resolve the supported within-facet semantics in the Spec if multiple selections are added. A query or filter change resets pagination. Display “Showing 30 of 146” only from actual server data; if another page fails, preserve existing results and offer Retry below them.

Do not make requests on every keystroke under the current explicit-submit contract. A new committed query cancels/supersedes prior requests. Query spelling/diacritic matching uses approved normalization and aliases. Homonymous ragas remain separate choices; do not silently merge identities or expand to nomenclature-equivalent ragas. Blank searches can browse a bounded list. Full-text lyric search, transliteration across arbitrary scripts and semantic search are separate capabilities, not implicit promises of this redesign.

### Raga and composer pages

Raga detail leads with name and identity context, followed by known aliases, stored Arohanam/Avarohanam, documented parent/melakarta relationships, explicit nomenclature links and compositions. Hide absent optional fields or mark them unavailable where the omission affects interpretation. Never infer a parent, scale, mood, performance time or equivalence merely to fill a card. For Ragamalika compositions, retain ordered and repeated memberships and only show section associations actually stored.

Composer detail leads with the stored name; dates/place appear only when available. Biography, portraits, signature/mudra and thematic collections need sourced, publishable content beyond the current detail DTO. Give the catalogue of works priority over an empty biography panel.

### Reader: make the text the centre

Use a compact, content-sized title block with composer and form, accessible Back and bookmark controls. On scroll, reduce the decorative footprint. Titles wrap at large text sizes. Place raga/tala links beneath the title; keep an ordered list for Ragamalika instead of forcing a single raga badge.

Lyrics is the default view. Keep an obvious Details/source view. A script selector narrows available stored readings; a separate reading selector identifies source/label when more than one variant uses that script. Selecting Tamil changes the displayed stored variant, not the original language. Never stitch sections from different readings.

Offer section jumps to stored Pallavi, Anupallavi, Charanam and other form-specific labels, retaining repeated/numbered sections. Preserve line breaks and script shaping. Show missing/partial/unknown completeness truthfully. Unsegmented source text is not silently parsed into an invented section structure. The prototype uses short, explicitly illustrative excerpts from existing fixtures and omits questionable fixture metadata; it is not musicological evidence.

Reading controls: text size, system/light/dark appearance and bookmark. Later native sharing should share a verified public link after canonical web/universal-link behaviour is defined; it must not ship a dead link. Playback controls appear only when a supported, rights-cleared recording exists. Lyrics-only Varnam/Swarajathi views must not imply that notation or the full composition has been rendered.

### Library and Settings

Keep existing favourites compatible and use one bookmark metaphor across cards and reader. Favourites reopening always fetches the record online. Confirmed unavailable records remain identifiable/removable; a network failure must not delete a saved item.

Named collections can later store composition UUIDs, short labels and order; recent-reading can store a bounded UUID/variant/section pointer and timestamp. Specify storage versioning, bounds, migration, clearing controls and opt-out before implementation. These are intentional extensions to the accepted bookmark-only persistence model, not a hidden catalogue cache. Do not promise offline reading or multi-device sync.

Settings surfaces preferred script, text size and appearance already supported by the app. Explain unavailable preferred scripts with a fallback to an available stored reading. Add history controls only with the recents feature. Keep the core app useful anonymously.

## Visual system: evolve the temple-library identity

The [existing visual design](./track-138-visual-design.md) is the token source of truth. Reuse cream `#FDF3E3`, saffron `#C1350F`, deep saffron `#A92815`, teal `#1F6B70`, dark ink and the current dark palette. Gold remains ornament rather than small text. Any approved type/accessibility changes must update the shared tokens and their web documentation together; do not create mobile-only replacements with the same names.

Use Fraunces for Latin display titles and Work Sans for UI; supply tested Indic fallback coverage. Retain the painted cornice as a slender identity band. Home can carry richer illustration; search lists use quiet paper surfaces; the reader is the calmest screen. Use spacing and typographic hierarchy before adding borders and cards. Keep dense catalogue results legible and let long titles wrap.

Proposed accessibility improvements to the currently small metadata/labels: 13–14sp metadata and 16sp main controls/body, scalable with system settings; final token sizes belong in the Spec. Maintain comfortable lyric line spacing and validate stacked Indic marks in each supported script. Support native larger text with reflow, per [Apple typography guidance](https://developer.apple.com/design/human-interface-guidelines/typography), and at least 48dp Android touch targets per [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults). Test actual layouts and focus order; a colour palette alone is not accessibility verification.

Respect reduced motion, use subtle existing transition timing, and avoid autoplay, parallax and ornamental motion. Selection needs labels/checkmarks or shape in addition to colour. Test phone widths around 320–430dp, large text, both appearances, keyboard/IME, safe areas and screen readers. Tablets can use a list-detail layout after phone flows are complete.

## Proposed delivery sequence

These are scope slices for discussion, not a file-level implementation Plan.

| Slice | User-visible result | Dependencies / exit evidence |
|:---|:---|:---|
| **1. Discover and read** | Home; four-tab shell; complete krithi/raga/composer search; exact raga/composer filters; paging; real entity pages; calmer reader; existing bookmarks | Existing API plus an explicit home-content decision. Native search → entity → reader → bookmark → back/restart journeys on both platforms; large-text/dark checks |
| **2. Complete metadata exploration** | Tala, deity, temple/kshetra, language and form discovery/filtering; metadata links in reader | Audit public-ready fields/junctions, extend DTOs/OpenAPI/public routes and queries, publication/count tests, AND-filter/paging tests and mobile journeys |
| **3. Personal library and editorial depth** | Named collections, optional recents, source-aware sharing when supported, curator-managed thematic features | Accepted local-storage changes, editorial publishing/audit support and valid deep-link destinations; migration, unavailable-item and privacy checks |
| **4. Broader companion opportunities** | Sourced learning guides; licensed recordings; notation/practice experiences; optional sync/offline packs | Separate product/rights/content and technical decisions. These are opportunities, not promised functionality in slices 1–3 |

The first three slices address the requested complete discovery experience; the other metadata categories are part of this track even though they require more than visual changes. No time estimate is assigned before corpus and API readiness are checked.

## Architecture and content readiness

Continue immutable presenter state, lifecycle-aware collection and typed per-tab navigation in shared Compose. Separate Home/Explore/entity/Reader state; avoid growing a single screen into the entire app. Reuse CatalogueRepository and current clients where contracts fit. Keep Android/iOS platform abstractions narrow.

Before promising metadata coverage, inspect which fields/relations are actually populated in published records. Existing database/admin fields do not automatically qualify for anonymous exposure. New public facets must use published associations, deduplicated stable IDs and bounded pagination; exact raga filtering continues to use junction membership. Counts must follow the same predicates as returned rows. No per-card network calls or unbounded home prefetch.

Curator-created features, if approved, need a publication lifecycle, ordering, withdrawal handling, source attribution and audited mutations. New tables/columns use Flyway. Existing curator editing flows must retain authorized draft access. Do not repair the corpus or weaken publication rules to populate the new screens.

Usage should distinguish home/directory opens from committed searches and reader views. Preserve anonymous instrumentation and avoid recording raw queries, lyrics or new persistent device fingerprints. Keep latency measurements separate from visit counts. Record device/network/dataset conditions before setting performance thresholds; suggested product targets are first useful content within 2 seconds and search results within 1.5 seconds on an agreed warm-service reference profile, to be validated in the Spec rather than claimed as current performance.

## Proposed acceptance journeys

1. Fresh install → useful Home → search a known title → read stored lyrics, without authentication.
2. Explore → raga identity → works → refine by composer → reader → back restores exact results and position.
3. Search with spelling/diacritic variants, homonymous raga choices and a Ragamalika secondary-raga match; retain exact identities and deduplicated totals.
4. Combine each newly supported metadata facet with another filter; verify AND semantics, literal wildcards, zero results, stable paging and correct published-only counts.
5. Reader → switch between two same-script sources → section jumps → increase type size; only selected-variant text appears and repeated sections remain distinct.
6. Save → force close → reopen Library → fetch reader online. Exercise network failure and a withdrawn record without losing bookmarks.
7. Slow response followed by a newer query, failed next page, Home feature withdrawal and unavailable preferred script all produce recoverable states.
8. VoiceOver and TalkBack complete discovery/save/read/back; large text and dark mode preserve essential controls and Indic text. Verify on real native hosts, not just this concept.
9. Regression: authenticated curator search/edit retains draft access; anonymous new and existing endpoints do not reveal drafts or source-internal fields.

## Intent decision and next gate

The **Home → Explore → Reader → Library** direction and shared temple-library identity are accepted. The accepted detailed Spec on TRACK-140 resolves scope more precisely: initial curated features are composition invitations; long-form learning/thematic articles and sharing await later contracts. It specifies three native appearance choices, exact metadata facets, bounded local collections/opt-in recents, and source/identity fidelity. The implementation Plan is now Draft, with a dependency graph, three release gates and a resumable build/test/review/repair loop. Plan acceptance is the next gate.

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
