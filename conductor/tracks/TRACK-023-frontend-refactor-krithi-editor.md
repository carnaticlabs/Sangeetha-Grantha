# Conductor Track: Krithi Editor Refactoring
**ID:** TRACK-023
**Status:** Completed
**Owner:** UI Team

## Objective
Decompose the monolithic `KrithiEditor.tsx` component into smaller, manageable, and typed components.

## Scope
-   Refactor `KrithiEditor.tsx` (1,860 lines).
-   Extract Tabs into separate components.
-   Implement `useReducer` for state management.
-   Remove all `any` types.

## Checklist
- [x] **Component Decomposition**
    - [x] Create `src/components/krithi-editor/MetadataTab.tsx`
    - [x] Create `src/components/krithi-editor/StructureTab.tsx`
    - [x] Create `src/components/krithi-editor/LyricsTab.tsx`
    - [x] Create `src/components/krithi-editor/TagsTab.tsx`
    - [x] Create `src/components/krithi-editor/AuditTab.tsx`
- [x] **State Management**
    - [x] Create `src/hooks/useKrithiEditorReducer.ts`
    - [x] Migrate `useState` to `useReducer`.
- [x] **Data Handling**
    - [x] Create `src/hooks/useKrithiData.ts` (Fetch & Save)
    - [x] Create `src/utils/krithi-mapper.ts`
    - [x] Create `src/utils/krithi-payload-builder.ts`
- [x] **Main Component Integration**
    - [x] Refactor `src/pages/KrithiEditor.tsx` to use the new components and hooks.

## Acceptance Criteria
- [x] `KrithiEditor.tsx` is < 300 lines.
- [x] All 15+ `any` types are removed.
- [x] Full functionality preserved (Edit, Save, Load).

## Post-completion fix: Sections and tags not reappearing after navigate-back (2026-01-28)
- **Issue:** After saving Structure (sections) and Tags, navigating away (e.g. Dashboard → Krithis → select same krithi) showed empty sections and tags. Data was persisted; the editor never loaded it on re-open.
- **Cause:** `GET /admin/krithis/:id` returns only core metadata (no sections/tags). Editor state was synced from that response only; sections and tags were lazy-loaded only when switching to Structure/Lyrics tabs. Tags were never loaded when opening the Tags tab (only the global tag list was loaded).
- **Fix (in `KrithiEditor.tsx`):**
  1. On initial load, when `serverKrithi` and `krithiId` are set, fetch sections and tags in parallel and merge into `SET_KRITHI` payload so they appear without having to switch tabs.
  2. When opening the Tags tab, call `loadKrithiTags(krithiId)` and dispatch `UPDATE_FIELD('tags', tags)` so assigned tags are shown (fallback if initial load hadn’t completed).
- **Backend:** No change. PUT `tagIds` and POST sections were already persisted; the fix is purely frontend loading/display.

## Post-completion note: 429 spam regression on sections/tags endpoints (2026-01-29)
- **Symptom:** While editing the krithi `Endaro Mahanubhavulu`, the backend logs showed a burst of `429 Too Many Requests` for the sections and tags endpoints:

  ```text
  2026-01-29 08:09:52,530 INFO  [eventLoopGroupProxy-4-7] io.ktor.server.Application - 429 Too Many Requests: GET - /v1/admin/krithis/fb866901-fe1b-445e-bfdb-f5f0158102a4/sections in 2ms
  2026-01-29 08:09:52,532 INFO  [eventLoopGroupProxy-4-8] io.ktor.server.Application - 429 Too Many Requests: GET - /v1/admin/krithis/fb866901-fe1b-445e-bfdb-f5f0158102a4/tags in 3ms
  ```

- **Root Cause:** The `useKrithiData` hook defined `loadSections`, `loadKrithiTags`, `loadVariants`, and `loadKrithi` with `useCallback` but included `toast` in their dependency arrays. Because `toast` can change identity between renders, these callbacks were re-created frequently. In `KrithiEditor.tsx`, an effect depends on `loadSections` and `loadKrithiTags` to eagerly hydrate sections/tags when `serverKrithi` changes; when the callback identities kept changing, React re-ran the effect repeatedly, spamming `GET /v1/admin/krithis/{id}/sections` and `/tags` until the global Ktor `RateLimit` plugin started returning 429.

- **Fix (in `useKrithiData.ts`):**
  - Updated `loadKrithi`, `loadSections`, `loadVariants`, `loadKrithiTags`, and `saveKrithi` to have stable identities by removing `toast` from their dependency arrays and documenting this with inline comments, while still calling `handleApiError(error, toast)` internally.
  - Suppressed exhaustive-deps warnings where appropriate to avoid reintroducing unstable dependencies that would retrigger the spam.

- **After Fix (healthy pattern):** Opening the editor now results in a bounded set of calls (one `GET /krithis/{id}`, one `GET /v1/admin/krithis/{id}/sections`, one `GET /v1/admin/krithis/{id}/tags`, plus on-demand tab loads). Logs show 200s instead of 429 spam, e.g.:

  ```text
  2026-01-29 08:17:23,931 INFO  [DefaultDispatcher-worker-4] io.ktor.server.Application - 200 OK: GET - /v1/admin/krithis/fb866901-fe1b-445e-bfdb-f5f0158102a4/sections in 112ms
  2026-01-29 08:17:23,931 INFO  [DefaultDispatcher-worker-1] io.ktor.server.Application - 200 OK: GET - /v1/admin/krithis/fb866901-fe1b-445e-bfdb-f5f0158102a4/tags in 112ms
  ```
