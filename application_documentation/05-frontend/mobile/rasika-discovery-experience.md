| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-11-13 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Rasika: discovery and reading experience

---

This guide describes what Rasika does today and the principles that keep it honest, then lists what is still planned. Requirements and node status live in the [TRACK-140 spec](../../../conductor/tracks/TRACK-140-rasika-discovery-experience.md) and the [evidence report](../../10-implementations/track-140-rasika-discovery.md). Capability status across the whole product lives in the [feature map](../../01-requirements/features/README.md); architecture and build commands are in the [mobile implementation guide](./README.md).

> [!IMPORTANT]
> "Shipped" means present in the repository and covered by shared tests. Native-device execution and TalkBack/VoiceOver proof remain release gates, and nothing here certifies a production deployment.

## Product direction

Rasika should feel like opening a beautifully typeset music volume with a knowledgeable guide nearby. The first screen invites discovery, search gets an experienced user directly to a composition, and the reader is quiet enough for sustained attention.

The guiding journey is **discover a work → understand its context → read its source → keep it close**. Every label that represents a public entity is a route into the library: a raga leads to its compositions, a composition to its composer, a cycle to its ordered members, a temple to the kritis sung there.

## Current experience

### Navigation

Four tabs, each with its own back stack (maximum depth 8, after which the app offers to return to the tab start): **Home**, **Explore**, **Library**, **Settings**. Pushed destinations are Browse, Collections and Collection detail, Kshetras and Kshetra detail, Raga detail, Composer detail, and the Krithi reader. Back returns to the exact query, filters and page. Navigation lives in [`navigation/`](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/navigation), and the shell in [RasikaApp](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/RasikaApp.kt).

| Destination | What it does |
|:---|:---|
| **Home** | Search field first. Then, as each loads, one featured composition ("From the collection" with "Read composition"), shortcuts to Ragas and Composers, a Collections shelf and a Kshetras shelf. If the featured composition fails to load, Home stays quiet rather than showing an error panel, and search and shortcuts remain available |
| **Explore** | Search over Krithis, plus Browse (Ragas, Composers) and filters; opens entity pages and the reader |
| **Library** | Device-local favourites. Only identifiers and titles are stored; opening a favourite fetches the record online |
| **Settings** | Appearance (System, Light, Dark), text size (Small, Medium, Large, Extra large), preferred script (or Server default), with a lyric size sample |

### Search

Explore calls the V2 catalogue and the discovery search endpoints. The default is **All matches** (hybrid: titles, lyrics and meaning together). **Search options** offers **Related meanings** (semantic) and **Titles & lyrics** (lexical). Lexical search matches stored lyrics as well as titles (TRACK-159). Hybrid and semantic send the trimmed query with `limit` 30, are not paged, and are headed "Top matches"; the lexical catalogue path pages with "Load more" and shows "Showing N of M" only from server totals. Saved raga and composer filters apply to lexical search only, and the screen says so.

Hybrid and semantic results may begin with **collection and kshetra cards** (TRACK-152), for example "sixth enclosure" or "fire lingam". A card opens the collection or kshetra and is described to screen readers by kind, title, subtitle and recorded-gap state. Cards appear only when a score floor and gap condition are met (TRACK-157), so an ordinary title search shows kritis only. Details: [Search guide](../../03-api/search.md).

Filters use a sheet with explicit **Apply**, **Cancel** and **Reset**. Raga and composer pickers are searchable and reach every entry rather than a first page (TRACK-159). Homonymous ragas stay separate identities; names are never merged to fill a list.

**Lakshana facets** (TRACK-153) add reviewed discriminators under "Filter by": Vibhakti, Cycle, Tune, Region (mandalam) and Element. A facet appears only when it has a value in the current scope, counts follow the same predicates as the rows, and the screen notes that filters cover only compositions with reviewed data. Active facets show as removable chips.

### Browse and entity pages

Browse lists Ragas (Melakartas, Janya ragas, Other ragas) and Composers with a directory filter and published counts. Raga detail shows identity context, aliases, Arohanam and Avarohanam where stored, and the raga's compositions. Composer detail lists the composer's works. Absent optional fields are hidden, never inferred.

### Collections

Reviewed cycles such as Navavarana, Navagraha, Pancha Bhuta Sthala, Ratnatrayam and Ghana-raga Pancharatnam appear as ordered **Collections** (TRACK-150), with an overview and the stated order basis. A collection says plainly when it is an open set, when no fixed sequence is claimed, or when an attribution is disputed. Members that are not published are not shown.

### Kshetras

**Kshetras** (TRACK-151, TRACK-155) lists temples grouped by region (Desam) with an element filter, then a detail page with the temple note, sthala vriksha, tirtha and deity details where recorded, nested shrines, and the kritis sung there. Tiruvarur's long list renders lazily and the list keeps its place after Back. A temple with no kriti but a reviewed note shows a **Recorded gap** with the catalogue's reason, never an invented composition. **Open in Google Maps** appears only for temples with a reviewed place id (TRACK-154); there is no embedded map. Only places with published kritis or a reviewed note are listed, and copy never claims the list covers every kshetra. Details: [Rasika kshetra exploration](../../01-requirements/features/rasika-kshetra-exploration.md).

### Reader

Lyrics is the default view, with a **Details** view for source and metadata. [KrithiReaderScreen](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/reader/KrithiReaderScreen.kt) behaviour:

- One stored variant at a time, chosen by **Script** and **Source reading**. Selecting Tamil changes the displayed stored variant, not the original language. Sections are never stitched from different readings. While a new reading loads, the previous text stays, and a failure keeps it with a message.
- Stored section labels with **Jump to section**, repeated sections kept distinct, and a Varnam caption ("pallavi of the charanam").
- Ordered ragas, including Ragamalika, rather than a single raga badge, with raga associations bound to a section where stored.
- Tamil-script editions that use digits for Sanskrit sounds show them as attached superscripts; stored text is unchanged (TRACK-156).
- **Cycle context** (TRACK-150): the collections the kriti belongs to, its position and neighbours (Previous, Next), vibhakti case and occasion note, where reviewed.
- **Facet chips** (TRACK-153) for vibhakti, tune and region, each opening the matching filtered list, and a temple link into Kshetras.
- A completeness line that says whether a stored reading is complete or partial, and a caption that the reader shows stored lyrics, not notation.
- Bookmark control; text size follows Settings.

Musical form is shown only when established. Compositions whose form is `UNESTABLISHED` carry no form badge.

## Principles that still govern changes

- **Stored, reviewed data only.** Never infer a parent raga, scale, equivalence, temple or cycle membership merely to fill a card. Absent data is hidden or labelled unavailable.
- **Published only.** Rasika reads `/v2/catalogue` and published records; draft access belongs to the Curator Console.
- **No hidden persistence.** Favourites and preferences are local documents; there is no sync, offline lyric pack, or catalogue cache.
- **Truthful states.** Counts come from server totals; failed pages keep prior results with Retry; empty results say what was searched.
- **Accessible by default.** Large-text reflow, reduced-motion support, 48dp targets, selection shown by label or shape as well as colour. Visual tokens come from the [TRACK-138 visual design](./track-138-visual-design.md); Fraunces for Latin display titles, Work Sans for UI, and the painted cornice as a slim identity band.
- **Anonymous.** No account is needed. The app's only usage context is a random session id and a random per-interaction id, which are not tied to an account or device.

## How curators check it

[Mobile Preview](../admin-web/mobile-preview.md) in the Curator Console replicates these screens read-only so an edit can be checked before a rasika sees it, including search order, match reasons and a stale-index warning.

## Not yet built

| Area | Status |
|:---|:---|
| Tala, deity, language and musical-form directories and filters | Planned. V2 does not mount these endpoints, and the feature map tracks the boundary |
| Recents and "continue reading" | Not implemented. Home text refers to it, and any storage needs a bounded, versioned, clearable design first |
| Named collections of the user's own | Planned; the Library holds favourites only |
| Embedded map for kshetras | Follow-up to TRACK-151; today a link only |
| Sharing, playback, notation rendering, offline packs, account sync | Not promised; each needs rights, link or product decisions first |
| Tablet list-detail layout | After phone flows are complete |
| Native release gate | Journey tests exist; device execution and TalkBack/VoiceOver proof are still required |

## Acceptance journeys

1. Fresh install → useful Home → search a known title → read stored lyrics, without authentication.
2. Explore → raga identity → works → refine by composer → reader → Back restores results and position.
3. Search with spelling or diacritic variants, homonymous ragas and a Ragamalika secondary raga; identities and totals stay exact.
4. Search a theme ("fire lingam") → kshetra card → detail → a sung kriti → reader → cycle context → Previous or Next.
5. Apply a facet with another filter; AND semantics, zero results, stable paging and published-only counts hold.
6. Reader → switch between two same-script sources → section jumps → larger text; only the selected variant appears.
7. Save → force close → reopen Library → fetch the reader online; a network failure or withdrawn record never deletes a bookmark.
8. Slow then newer query, failed next page, withdrawn Home feature and unavailable preferred script all end in recoverable states.
9. VoiceOver and TalkBack complete discovery, save, read and back; large text and dark mode keep essential controls and Indic text intact. Verify on real native hosts.
10. Regression: authenticated curator search and edit keep draft access; anonymous endpoints never reveal drafts or source-internal fields.

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
