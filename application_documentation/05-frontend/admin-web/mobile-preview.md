| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-02 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Mobile Preview

---

Mobile Preview is a read-only replica of the Rasika mobile app inside the Curator Console. It lets a curator see, right after an edit, how a krithi will look on mobile and what a rasika will find when they search. It never changes data.

## Where to find it

- **In the editor:** open a saved krithi and choose the **Mobile Preview** tab. It opens the reader for that krithi, shows whether the search index is out of date for it, and offers **Search for this krithi**.
- **Whole app:** **Mobile Preview** in the sidebar (`/mobile-preview`). `?krithi=<id>` opens it on one krithi.

The preview refreshes after a save in the editor, when the window regains focus, and on **Refresh preview**. A finished search is not re-run automatically, because that can cost an embedding; it shows "Edits saved since this search. Run it again" instead.

## What it shows

| Mobile screen | Preview |
|:---|:---|
| Home, Explore (Krithis, Ragas, Composers), Browse facets | Same data, same ordering |
| Krithi reader: Lyrics and Details, script and source choices, section labels, Varnam caption, ragas bound to a section, ragamalika membership, cycle context | Same rules, ported from `SectionLabels`, `TamilNotation` and the presenters |
| Raga, composer, collection and kshetra screens | Same DTOs |
| Favourites, bookmarks, device text size, system appearance | Not replicated: on-device. Appearance and text size are preview-local toggles saved in this browser only |

Krithis that are not published are visible in the preview and carry a **Not published** banner; mobile users cannot see them.

## Search

Search runs the same code the mobile app calls (`RasikaSearchService`). Modes are **All matches** (hybrid, default), **Related meanings** (semantic) and **Titles & lyrics** (lexical), behind **Search options**. Searches run only on submit. The result order is mobile's: the exact-title krithi first, then up to three collection and kshetra cards, then the other krithis.

Curator additions:

- **Why this matched** on each krithi: the matched passage and its meaning, text and combined scores.
- **Why collections and kshetras appear**: the active embedding profile, the floor a card must clear, and every candidate with the condition that kept it out.
- **Include unpublished**: off by default (what mobile gets). When on, indexed draft and in-review krithis can appear and are badged **Not visible on mobile**; their documents also take part in ranking, so the order can differ slightly.
- **Search index status** for the krithi being edited: *Not in the search index*, *Index out of date* or *looks current*, with reasons and both timestamps.

### How current is search?

Titles & lyrics reads the live catalogue tables and is current immediately. All matches and Related meanings read `search_documents` and `document_embeddings`, which only the Python worker writes (`embed_catalogue.py`; see [embeddings](../../09-ai/embeddings.md)). A saved edit is therefore searchable by meaning only after the worker has re-indexed it.

The status check compares timestamps. It can say "out of date" after an edit that did not change the indexed text, and it cannot see a raga or composer rename. It is worded "may be out of date" for that reason.

### Cost and limits

- Hybrid and Semantic can embed the query (one paid Gemini call, cached per query string for the server's lifetime). Titles & lyrics and every catalogue read make no Gemini call.
- Preview search is limited to 20 requests per minute per curator, on top of the global limit.
- Preview routes are admin-only, write nothing (no `AUDIT_LOG` rows), and are not recorded in the mobile usage log.

## Differences between mobile and the rest of the console

| Topic | Mobile | Elsewhere in the console |
|:---|:---|:---|
| Krithi visibility | Published only | The krithi list and admin search show every workflow state |
| Search endpoint | `POST /v2/catalogue/search/*` with cards and exact-title ordering | The list's Hybrid and Semantic use `POST /v1/search/*`: no cards, no exact-title ordering |
| Filters on discovery | Raga and composer apply to Titles & lyrics only | The list sends them to Hybrid and Semantic as well |
| Titles & lyrics | Matches titles, incipits, composers and ragas, not lyric text, despite the label (finding D7) | Admin search has a lyric parameter |
| Raga picker | Loads the first 30 of 1,014 ragas (finding D8) | Full reference data |

D7 and D8 were confirmed against the dev catalogue and are mobile behaviours; the preview reproduces them and flags D8 in the filter sheet. They are candidates for separate tracks.

## API

Read-only, admin role required. All sit outside `/v1/catalogue` and `/v2/catalogue`.

| Route | Purpose |
|:---|:---|
| `GET /v1/admin/preview/catalogue/*` | The V2 reads with draft and in-review krithis visible |
| `GET /v1/admin/preview/mobile/*` | The unchanged published-only reads, identical to `/v2/catalogue/*` |
| `POST /v1/admin/preview/search/{hybrid,semantic}` | The V2 search wrapped with an explanation and the unpublished ids |
| `GET /v1/admin/preview/krithis/{id}/index-status` | Index freshness for one krithi |

See the [OpenAPI document](../../../openapi/sangita-grantha.openapi.yaml) and [catalogue search](../../03-api/search.md).

## Keeping it faithful

The TypeScript port is guarded two ways. Golden files under `modules/frontend/sangita-admin-web/src/components/mobile-preview/fixtures/` are read by both Vitest and the Kotlin test `PreviewGoldenFixturesTest`; and `mobilePreviewDtoDrift.test.ts` compares every mirrored TypeScript DTO with its Kotlin `data class`. Server results are checked by integration tests that compare the preview with the V2 responses.

Source: [TRACK-158](../../../conductor/tracks/TRACK-158-mobile-preview.md).

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
