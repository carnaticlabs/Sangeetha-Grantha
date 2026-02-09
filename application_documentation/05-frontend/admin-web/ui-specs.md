| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# React Admin Web — UI Specifications

**Source of truth:** `modules/frontend/sangita-admin-web/`

---

## 1. Tech Stack

| Technology | Version | Purpose |
|:---|:---|:---|
| React | 19.2.4 | UI library |
| TypeScript | ~5.9.0 | Type safety |
| Vite | 7.3.1 | Build tool & dev server |
| Tailwind CSS | 4.1.18 | Utility-first styling |
| React Router | 7.13.0 | Client-side routing |
| TanStack React Query | 5.90.20 | Server state management & caching |
| Google Generative AI (`@google/genai`) | 1.34.0 | Gemini AI integration for scraping & transliteration |
| Playwright | 1.40.0 | End-to-end testing |
| Vitest | 4.0.18 | Unit testing |
| ESLint | 9.39.2 | Linting |

### Build Configuration

- **Dev server port**: 5001
- **API proxy**: `/v1` → `http://localhost:8080`
- **Environment**: Loaded from `config/` directory (monorepo root)
- **Path alias**: `@` → project root
- **Package manager**: Bun

---

## 2. Project Structure

```text
src/
├── index.tsx                           # React entry point (ErrorBoundary + render)
├── index.css                           # Global styles, Tailwind theme, animations
├── App.tsx                             # Router + QueryClientProvider + Layout
├── metadata.json                       # App metadata
├── types.ts                            # Main type definitions
│
├── api/
│   └── client.ts                       # Centralised API client (60+ endpoint functions)
│
├── components/
│   ├── Sidebar.tsx                     # Fixed sidebar navigation
│   ├── TopBar.tsx                      # Top bar with search & environment badge
│   ├── Toast.tsx                       # Toast notification system (useToast hook)
│   ├── ErrorBoundary.tsx               # React error boundary
│   ├── SearchableSelectField.tsx       # Typeahead select for entity references
│   ├── DeityModal.tsx                  # Deity selection modal
│   ├── TempleModal.tsx                 # Temple selection modal
│   ├── TransliterationModal.tsx        # AI transliteration dialog
│   ├── ReviewImportModal.tsx           # Import review modal (from ImportsPage)
│   │
│   ├── common/
│   │   ├── Modal.tsx                   # Base modal component (overlay + panel)
│   │   ├── ConfirmationModal.tsx       # Yes/No confirmation dialog
│   │   ├── ReferenceSelectionModal.tsx # Reference entity picker modal
│   │   ├── SectionHeader.tsx           # Reusable section header with actions
│   │   └── Skeleton.tsx               # Loading skeleton placeholder
│   │
│   ├── dashboard/
│   │   ├── StatCard.tsx               # Metric card (icon, label, value, click)
│   │   └── RecentItem.tsx             # Activity feed item (title, subtitle, time, status)
│   │
│   ├── form/
│   │   ├── FormInput.tsx              # Labelled text input
│   │   ├── FormSelect.tsx             # Labelled select dropdown
│   │   ├── FormTextarea.tsx           # Labelled textarea
│   │   ├── FormCheckbox.tsx           # Labelled checkbox
│   │   └── ReferenceField.tsx         # Reference entity field with search & modal
│   │
│   ├── krithi-editor/
│   │   ├── MetadataTab.tsx            # Core metadata fields (title, composer, raga, etc.)
│   │   ├── StructureTab.tsx           # Section structure editor (P/A/C)
│   │   ├── LyricsTab.tsx             # Multi-language lyric variant editor
│   │   ├── TagsTab.tsx               # Tag assignment interface
│   │   └── AuditTab.tsx              # Audit log history display
│   │
│   ├── notation/
│   │   ├── NotationTab.tsx            # Notation main tab (variant list + row editor)
│   │   ├── NotationVariantList.tsx    # List of notation variants
│   │   ├── NotationVariantModal.tsx   # Create/edit notation variant modal
│   │   └── NotationRowsEditor.tsx    # Inline notation row editing grid
│   │
│   └── reference-data/
│       ├── types.ts                   # Reference data component types
│       ├── EntityList.tsx             # Generic entity list with search, edit, delete
│       └── forms/
│           ├── ComposerForm.tsx       # Composer create/edit form
│           ├── RagaForm.tsx           # Raga create/edit form (arohanam/avarohanam)
│           ├── TalaForm.tsx           # Tala create/edit form
│           ├── DeityForm.tsx          # Deity create/edit form
│           └── TempleForm.tsx         # Temple create/edit form (location, geo, deity)
│
├── pages/
│   ├── Login.tsx                      # Token-based login form
│   ├── Dashboard.tsx                  # Home dashboard (stats, feed, tasks)
│   ├── KrithiList.tsx                 # Krithi search & listing
│   ├── KrithiEditor.tsx               # Tabbed krithi editor (6 tabs)
│   ├── ReferenceData.tsx              # Reference data management (card home → list → form)
│   ├── ImportsPage.tsx                # Web scraping + import history
│   ├── BulkImport.tsx                 # Bulk import orchestration (batch list + detail)
│   ├── ImportReview.tsx               # Split-pane import review queue
│   ├── AutoApproveQueue.tsx           # Auto-approve queue (backend integration)
│   ├── TagsPage.tsx                   # Tag CRUD with category filtering
│   ├── UsersPage.tsx                  # User management (placeholder)
│   └── RolesPage.tsx                  # Role management (placeholder)
│
├── hooks/
│   ├── useKrithiData.ts               # Krithi CRUD, section/variant/tag loading
│   ├── useKrithiEditorReducer.ts      # Editor state machine (useReducer)
│   ├── useReferenceData.ts            # Load all reference data (composers, ragas, etc.)
│   ├── useEntityCrud.ts               # Generic entity CRUD (stats, load, save, delete)
│   ├── useBatchActions.ts             # Bulk import batch actions (pause/resume/retry/etc.)
│   └── useAbortController.ts          # Request cancellation
│
├── types/
│   ├── api-params.ts                  # API request parameter types
│   ├── form-state.ts                  # Form state types
│   └── krithi-editor.types.ts         # Editor-specific type definitions
│
└── utils/
    ├── enums.ts                       # Enum display labels & colour mappings
    ├── error-handler.ts               # Centralised API error handler
    └── krithi-mapper.ts               # DTO ↔ form state mappers

e2e/                                   # Playwright end-to-end tests
├── playwright.config.ts               # Test runner configuration
├── global-setup.ts                    # Global test setup (auth, DB)
├── fixtures/
│   ├── auth.setup.ts                  # Authentication fixtures
│   ├── db-helpers.ts                  # Database helpers (pg)
│   ├── shared-batch.ts               # Shared batch test fixtures
│   └── test-data.ts                   # Test data constants
├── pages/                             # Page Object Model
│   ├── base.page.ts
│   ├── login.page.ts
│   ├── krithi-list.page.ts
│   ├── bulk-import.page.ts
│   └── import-review.page.ts
├── tests/
│   ├── bulk-import-happy-path.spec.ts
│   ├── bulk-import-error-cases.spec.ts
│   ├── bulk-import-database.spec.ts
│   └── bulk-import-review.spec.ts
└── utils/
    ├── api-client.ts
    ├── log-verifier.ts
    └── polling.ts
```

---

## 3. Design System

### 3.1 Typography

| Token | Font | Usage |
|:---|:---|:---|
| `font-sans` | Work Sans (300–700) | Body text, UI elements, labels |
| `font-serif` / `font-display` | Fraunces (300–700) | Headings, page titles, brand elements |

### 3.2 Colour Palette

| Token | Value | Usage |
|:---|:---|:---|
| `ink-900` to `ink-50` | `#3e4049` → `#f8f8fa` | Text hierarchy |
| `primary` | `#3b82f6` (Blue 500) | Interactive elements, links, active states |
| `primary-dark` | `#2563eb` (Blue 600) | Hover states |
| `primary-light` | `#dbeafe` (Blue 100) | Active backgrounds, badges |
| `accent` | `#d94e1f` (Orange-red) | Brand accent, warnings |
| `surface-light` | `#ffffff` | Card backgrounds |
| `border-light` | `#e2e8f0` (Slate 200) | Card borders, dividers |

### 3.3 Icons

- **Material Symbols Outlined** (Google Fonts, variable icon font)
- Class: `material-symbols-outlined`
- Sizes: `text-sm` (14px), `text-base` (16px), `text-[20px]`, `text-2xl`, `text-3xl`

### 3.4 Animations

| Class | Effect |
|:---|:---|
| `animate-fadeIn` | Opacity 0→1 with slight scale, 0.4s ease-out |
| `animate-slide-in-right` | Translate from right, 0.3s ease-out |
| `animate-spin` | 360° rotation for loading spinners |
| `animate-pulse` | Pulse animation for active/processing states |

---

## 4. Routing

Defined in `App.tsx` using `react-router-dom` v7:

| Route | Component | Description |
|:---|:---|:---|
| `/login` | `Login` | Token-based authentication |
| `/` | `Dashboard` | Home dashboard with stats and activity |
| `/krithis` | `KrithiList` | Composition search and listing |
| `/krithis/new` | `KrithiEditor` | Create new composition |
| `/krithis/:id` | `KrithiEditor` | Edit existing composition |
| `/reference` | `ReferenceData` | Reference data management |
| `/imports` | `ImportsPage` | Web scraping and import history |
| `/bulk-import` | `BulkImport` | Bulk import orchestration |
| `/bulk-import/review` | `ImportReview` | Import review queue |
| `/tags` | `TagsPage` | Tag management |
| `/users` | `UsersPage` | User management (placeholder) |
| `/roles` | `RolesPage` | Role management (placeholder) |
| `*` | Fallback | "Module Under Development" message |

---

## 5. Layout Architecture

### 5.1 App Shell

```text
┌──────────────────────────────────────────────────┐
│ ┌──────────┐ ┌─────────────────────────────────┐ │
│ │          │ │ TopBar (search, env badge)       │ │
│ │          │ ├─────────────────────────────────┤ │
│ │ Sidebar  │ │                                 │ │
│ │ (fixed   │ │    <main> content area          │ │
│ │  264px)  │ │    (scrollable, padded)          │ │
│ │          │ │                                 │ │
│ │          │ │                                 │ │
│ └──────────┘ └─────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

- **Sidebar**: Hidden on mobile (`hidden md:flex`), 264px fixed width. Brand header → nav links → system section → sign-out.
- **TopBar**: Full width, contains desktop search bar and "STAGING" environment badge.
- **Main**: `flex-1 overflow-y-auto`, max-width 7xl content, responsive padding.

### 5.2 Sidebar Navigation

| Group | Items | Icons |
|:---|:---|:---|
| Main | Dashboard, Kritis, Reference Data, Imports, Bulk Import, Review Queue, Tags | `dashboard`, `music_note`, `library_books`, `upload_file`, `inventory_2`, `rate_review`, `label` |
| System | Settings (button), Users, Roles | `settings`, `group`, `admin_panel_settings` |
| Footer | Sign Out | `logout` |

Active state: `bg-primary-light text-primary` with filled icon variant.

---

## 6. State Management

### 6.1 Server State (TanStack React Query)

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 60 * 1000,   // 1 minute
      retry: 1,
    },
  },
});
```

Query key patterns:
- `['krithi', id]` – Single Krithi detail
- `['krithis']` – Krithi search results
- `['dashboardStats']` – Dashboard statistics
- `['auditLogs']` – Recent audit entries
- `['composers']`, `['ragas']`, `['talas']`, etc. – Reference data lists

### 6.2 Local State

- **useKrithiEditorReducer**: `useReducer`-based state machine for the Krithi Editor.
  Actions: `SET_KRITHI`, `UPDATE_FIELD`, `SET_ACTIVE_TAB`, `SET_SECTIONS_LOADED`, `SET_VARIANTS_LOADED`, `SET_SAVING`.
- **useState**: Page-level state for forms, filters, selections, loading/saving flags.
- **No global store**: No Redux/Zustand; React Query + local hooks are sufficient.

### 6.3 Cache Invalidation Strategy

| Mutation | Invalidated Keys |
|:---|:---|
| Create/update Krithi | `['krithi', id]`, `['krithis']`, `['dashboardStats']` |
| Save sections | `['krithi', id]` (sections sub-query) |
| Create/update lyric variant | `['krithi', id]` (variants sub-query) |
| Save notation | `['krithi', id, 'notation']` |
| Create/update reference entity | Entity-specific key + `['referenceStats']` |
| Import review | `['imports']`, `['dashboardStats']` |
| Bulk import actions | Batch-specific queries |

---

## 7. API Integration

### 7.1 Client Architecture

Single `api/client.ts` module with:
- **Base URL**: `import.meta.env.VITE_API_BASE_URL || '/v1'`
- **Auth**: JWT from `localStorage.getItem('authToken')`
- **Generic request function**: `request<T>(endpoint, options)` handling auth headers, error responses, and 204 empty bodies.
- **60+ exported functions** organised by domain (see Section 7.2).

### 7.2 API Domains

| Domain | Functions | Count |
|:---|:---|:---|
| Authentication | `login` | 1 |
| Dashboard | `getDashboardStats`, `getReferenceStats`, `getAuditLogs` | 3 |
| Krithi CRUD | `searchKrithis`, `getKrithi`, `createKrithi`, `updateKrithi` | 4 |
| Sections & Lyrics | `getKrithiSections`, `saveKrithiSections`, `getKrithiLyricVariants`, `createLyricVariant`, `updateLyricVariant`, `saveVariantSections` | 6 |
| Tags & Audit | `getKrithiTags`, `getKrithiAuditLogs`, `transliterateContent` | 3 |
| Reference Data | CRUD per entity (Composers, Ragas, Talas, Deities, Temples) × 5 ops | 25 |
| Tags Catalog | `getTags`, `getAllTags`, `getTag`, `createTag`, `updateTag`, `deleteTag` | 6 |
| Sampradayas | `getSampradayas` | 1 |
| Notation | `getAdminKrithiNotation`, `createNotationVariant`, `updateNotationVariant`, `deleteNotationVariant`, `createNotationRow`, `updateNotationRow`, `deleteNotationRow` | 7 |
| Imports | `getImports`, `scrapeContent`, `reviewImport`, `bulkReviewImports`, `getAutoApproveQueue` | 5 |
| Bulk Import | `uploadBulkImportFile`, `listBulkImportBatches`, `getBulkImportBatch`, `deleteBulkImportBatch`, `getBulkImportJobs`, `getBulkImportTasks`, `getBulkImportEvents`, `pauseBulkImportBatch`, `resumeBulkImportBatch`, `cancelBulkImportBatch`, `retryBulkImportBatch`, `approveAllInBulkImportBatch`, `rejectAllInBulkImportBatch`, `finalizeBulkImportBatch`, `exportBulkImportReport` | 15 |
| **Total** | | **~76** |

---

## 8. Key Feature Implementations

### 8.1 Krithi Editor

**Component**: `KrithiEditor.tsx` (wrapped in `ErrorBoundary`)

**Architecture**:
- `useKrithiEditorReducer` manages local editor state.
- `useKrithiData` provides CRUD operations (query, save, load sections/variants/tags).
- `useReferenceData` loads all reference entities for selection fields.
- Tabs lazy-load data on activation (sections, lyric variants, tags).
- URL-driven: `/krithis/new` for create, `/krithis/:id` for edit.

**Tab Components**:

| Tab | Component | Data Source | Features |
|:---|:---|:---|:---|
| Metadata | `MetadataTab` | Core Krithi fields | Searchable reference selectors, musical form dropdown, ragamalika toggle |
| Structure | `StructureTab` | `getKrithiSections` | Section type/label editor, add/remove/reorder |
| Lyrics | `LyricsTab` | `getKrithiLyricVariants` | Multi-variant management, language/script/sampradaya, primary flag |
| Notation | `NotationTab` | `getAdminKrithiNotation` | Variant list, modal for create/edit, row editor grid |
| Tags | `TagsTab` | `getKrithiTags` + `getAllTags` | Assign/unassign from catalog |
| Audit | `AuditTab` | `getKrithiAuditLogs` | Timeline of field-level changes |

### 8.2 Reference Data

**Component**: `ReferenceData.tsx`

**Three-level navigation** (managed via `ViewMode` state):
1. **HOME**: Card grid (5 entities) with counts and descriptions.
2. **LIST**: `EntityList` component — searchable, sortable table with edit/delete actions.
3. **FORM**: Entity-specific form (ComposerForm, RagaForm, etc.) with breadcrumb nav.

**Generic pattern**: `useEntityCrud` hook provides `loadStats`, `loadData`, `saveEntity`, `deleteEntity` for all entity types.

### 8.3 Bulk Import

**Component**: `BulkImport.tsx`

**Architecture**:
- Master-detail layout: batch list (left 2/3) + batch detail (right 1/3).
- `useBatchActions` hook wraps pause/resume/cancel/retry/delete/approve-all/reject-all/finalize/export operations.
- Auto-polling: 5-second interval for RUNNING/PENDING batches.
- Batch detail includes pipeline stepper (MANIFEST_INGEST → SCRAPE → ENTITY_RESOLUTION), job list, task list (filterable by status), and event log.
- Task log viewer: Full-screen slide-in drawer with task overview, error details, and copy-to-clipboard.

### 8.4 Import Review Queue

**Component**: `ImportReview.tsx`

**Architecture**:
- Full-height split-pane: sidebar list (1/3) + detail panel (2/3).
- Sidebar: checkbox selection, select-all, per-item preview (title, composer, raga, source key).
- Detail panel: editable metadata form, AI resolution candidates panel (Composer, Raga, Deity, Temple with confidence scores), lyrics preview textarea.
- Bulk operations: Approve Selected, Reject Selected with confirmation.

### 8.5 Web Scraping

**Component**: `ImportsPage.tsx`

**Architecture**:
- Two tabs: "Scrape New" + "Import History".
- Scrape: URL input → calls `scrapeContent` API → shows success card with extracted title/composer.
- Import History: Table of all imported Krithis with status badges, review button triggers `ReviewImportModal`.

---

## 9. Type System

### 9.1 Core Enums

```typescript
enum ViewState { DASHBOARD, KRITHIS, KRITHI_DETAIL, REFERENCE, IMPORTS, BULK_IMPORT, IMPORT_REVIEW, TAGS }
enum MusicalForm { KRITHI, VARNAM, SWARAJATHI }
enum NotationType { SWARA, JATHI }
```

### 9.2 Domain Models

| Type | Description | Key Fields |
|:---|:---|:---|
| `KrithiDetail` | Full Krithi with all metadata | title, composer, ragas, tala, deity, temple, musicalForm, status, sections, lyricVariants, tags |
| `KrithiSummary` | List view summary | id, name, composerName, ragas, primaryLanguage |
| `KrithiSearchResult` | Paginated search response | items, total, page, pageSize |
| `Composer` | Composer entity | id, name, normalizedName, lifeDates, place, notes |
| `Raga` | Raga entity | id, name, normalizedName, melakartaNumber, parentRaga, arohanam, avarohanam |
| `Tala` | Tala entity | id, name, normalizedName, angaStructure, beatCount |
| `Deity` | Deity entity | id, name, normalizedName, description |
| `Temple` | Temple entity | id, name, normalizedName, location, primaryDeity, latitude, longitude |
| `Tag` | Tag entity | id, category, slug, displayName, displayNameEn, descriptionEn |
| `NotationVariant` | Notation variant | id, notationType, tala, kalai, eduppu, label, isPrimary |
| `NotationRow` | Single notation row | id, sectionType, orderIndex, swaraText, sahityaText, talaMarkers |
| `ImportedKrithi` | Imported record | id, rawTitle, rawComposer, rawRaga, rawTala, rawLyrics, importStatus, resolutionData, sourceKey |
| `ImportBatch` | Bulk import batch | id, sourceManifest, status, totalTasks, processedTasks, succeededTasks, failedTasks, startedAt, completedAt |
| `ImportJob` | Pipeline job | id, jobType, status |
| `ImportTaskRun` | Individual task | id, status, sourceUrl, krithiKey, attempt, durationMs, error |
| `ImportEvent` | Batch event | id, eventType, data, createdAt |
| `DashboardStats` | Dashboard metrics | totalKrithis, totalComposers, totalRagas, pendingReview |
| `ReferenceDataStats` | Reference counts | composerCount, ragaCount, talaCount, deityCount, templeCount, tagCount |
| `ResolutionResult` | AI entity matches | composerCandidates, ragaCandidates, deityCandidates, templeCandidates (each with entity, confidence, score) |

### 9.3 Supplementary Type Files

| File | Purpose |
|:---|:---|
| `types/api-params.ts` | API request parameter interfaces |
| `types/form-state.ts` | Form state type definitions |
| `types/krithi-editor.types.ts` | Editor-specific types (tab state, reducer actions) |

---

## 10. UI/UX Patterns

### 10.1 Workflow State Indicators

| State | Badge Style | Behaviour |
|:---|:---|:---|
| `draft` | `bg-slate-100 text-slate-600 border-slate-200` | Fully editable |
| `in_review` | `bg-amber-50 text-amber-700 border-amber-200` | Limited editing |
| `published` | `bg-green-50 text-green-700 border-green-200` | Read-only (archive only) |
| `archived` | `bg-gray-100 text-gray-800 border-gray-200` | Read-only |

### 10.2 Musical Form Indicators

| Form | Notation Tab | Notes |
|:---|:---|:---|
| `KRITHI` | Shown | Standard lyric-focused interface |
| `VARNAM` | Shown | Notation editor prominent |
| `SWARAJATHI` | Shown | Both lyrics and notation |
| Other | Hidden | Notation tab not rendered |

### 10.3 Status Chips (Bulk Import)

Batch statuses: `PENDING` (grey), `RUNNING` (blue), `PAUSED` (amber), `SUCCEEDED` (green), `FAILED` (rose), `CANCELLED` (grey).

Task statuses: `PENDING` (grey), `RUNNING` (blue), `SUCCEEDED` (green), `FAILED` (rose), `RETRYABLE` (amber), `BLOCKED` (purple), `CANCELLED` (grey).

### 10.4 Import Status Badges

`PENDING` (blue), `IN_REVIEW` (yellow), `APPROVED` (emerald), `MAPPED` (green), `REJECTED` (red), `DISCARDED` (grey).

### 10.5 Form Patterns

- Client-side validation before API calls.
- Required field indicators via label convention.
- Enum validation (musicalForm, workflowState, etc.).
- `SearchableSelectField` for reference entity selection with typeahead.
- Modal-based entity creation for inline reference data entry.
- Toast notifications for success/error feedback.
- Error boundaries for component resilience.

### 10.6 Loading & Empty States

- **Loading**: Spinner animation (`border-b-2 border-primary animate-spin`) with descriptive text.
- **Empty**: Icon + message + action button (e.g., "No imports found — Scrape a new Krithi").
- **Skeleton**: `Skeleton.tsx` component for placeholder content during data fetching.
- **Error**: Red-bordered alert card with error message and "Reload Page" action.

---

## 11. Custom Hooks

| Hook | Purpose | Key Returns |
|:---|:---|:---|
| `useKrithiData` | Krithi CRUD operations | `useKrithiQuery`, `loadKrithi`, `saveKrithi`, `loadSections`, `loadVariants`, `loadKrithiTags`, `loading`, `saving` |
| `useKrithiEditorReducer` | Editor state machine | `state` (krithi, activeTab, sectionsLoaded, lyricVariantsLoaded, saving), `dispatch` |
| `useReferenceData` | Load all reference entities | `composers`, `ragas`, `talas`, `deities`, `temples`, `sampradayas`, `loading`, `loadTags` |
| `useEntityCrud` | Generic entity CRUD | `loading`, `stats`, `loadStats`, `loadData`, `saveEntity`, `deleteEntity` |
| `useBatchActions` | Bulk import batch operations | `triggerAction(action, batchId)` — handles pause/resume/cancel/retry/delete/approve/reject/finalize/export |
| `useAbortController` | Request cancellation | Abort controller for in-flight request management |
| `useToast` | Toast notification system | `toasts`, `success(msg)`, `error(msg)`, `removeToast(id)` |

---

## 12. Utilities

| File | Purpose | Exports |
|:---|:---|:---|
| `utils/enums.ts` | Display labels & colours | `WORKFLOW_STATE_LABELS`, `WORKFLOW_STATE_COLORS`, `LANGUAGE_CODE_LABELS`, `LANGUAGE_CODE_OPTIONS`, `TAG_CATEGORY_LABELS`, `formatWorkflowState`, `getWorkflowStateColor`, `formatLanguageCode`, `formatTagCategory`, `formatMusicalForm` |
| `utils/error-handler.ts` | Centralised error handling | `handleApiError` — extracts user-friendly messages from API errors |
| `utils/krithi-mapper.ts` | DTO ↔ form mapping | `mapKrithiDtoToDetail` — maps API response to editor state; `buildKrithiPayload` — maps form state to API request |

---

## 13. Testing

### 13.1 Unit Tests (Vitest)

- Framework: Vitest 4.0.18 with jsdom environment
- Config: `vitest.config.ts`
- Run: `bun run test`

### 13.2 End-to-End Tests (Playwright)

- Framework: Playwright 1.40.0
- Config: `e2e/playwright.config.ts`
- Run: `bun run test:e2e`
- Reports: `bun run test:e2e:report`

**Page Object Model** (`e2e/pages/`):

| Page Object | Coverage |
|:---|:---|
| `base.page.ts` | Shared navigation and utility methods |
| `login.page.ts` | Authentication flow |
| `krithi-list.page.ts` | Krithi listing and search |
| `bulk-import.page.ts` | Bulk import batch operations |
| `import-review.page.ts` | Import review workflow |

**Test Suites** (`e2e/tests/`):

| Suite | Coverage |
|:---|:---|
| `bulk-import-happy-path.spec.ts` | End-to-end batch creation → completion flow |
| `bulk-import-error-cases.spec.ts` | Error handling, retry behaviour |
| `bulk-import-database.spec.ts` | Database state verification after import |
| `bulk-import-review.spec.ts` | Review queue and approval workflow |

**Test Fixtures** (`e2e/fixtures/`):
- Auth setup with test admin credentials
- Database helpers (direct pg queries)
- Shared batch creation fixtures
- Deterministic test data

---

## 14. Implementation Status

### Completed ✅

| Feature | Component(s) |
|:---|:---|
| App shell & navigation | `App.tsx`, `Sidebar.tsx`, `TopBar.tsx` |
| Login / Authentication | `Login.tsx`, `api/client.ts` (login) |
| Dashboard | `Dashboard.tsx`, `StatCard.tsx`, `RecentItem.tsx` |
| Krithi listing & search | `KrithiList.tsx` |
| Krithi Editor (6 tabs) | `KrithiEditor.tsx`, `MetadataTab`, `StructureTab`, `LyricsTab`, `NotationTab`, `TagsTab`, `AuditTab` |
| Notation management | `NotationTab.tsx`, `NotationVariantList`, `NotationVariantModal`, `NotationRowsEditor` |
| Reference Data CRUD | `ReferenceData.tsx`, `EntityList.tsx`, 5× entity forms |
| Tags management | `TagsPage.tsx` |
| Web scraping | `ImportsPage.tsx` (Scrape tab) |
| Import history & review | `ImportsPage.tsx` (List tab), `ReviewImportModal.tsx` |
| Bulk import orchestration | `BulkImport.tsx`, `useBatchActions.ts` |
| Import review queue | `ImportReview.tsx` (split-pane, AI resolution, bulk actions) |
| Auto-approve queue | `AutoApproveQueue.tsx` |
| Transliteration modal | `TransliterationModal.tsx` |
| Toast notification system | `Toast.tsx` + `useToast` |
| Error boundary | `ErrorBoundary.tsx` |
| Reusable form components | `FormInput`, `FormSelect`, `FormTextarea`, `FormCheckbox`, `ReferenceField` |
| Modal system | `Modal.tsx`, `ConfirmationModal.tsx`, `ReferenceSelectionModal.tsx` |
| Enum utilities | `enums.ts` |
| API client (60+ endpoints) | `api/client.ts` |
| E2E test suite | Playwright tests for bulk import flows |
| Design system tokens | Custom Tailwind theme, typography, animations |

### Placeholder 🔲

| Feature | Component | Notes |
|:---|:---|:---|
| User management | `UsersPage.tsx` | "Coming soon" placeholder |
| Role management | `RolesPage.tsx` | "Coming soon" placeholder |
| Settings | Sidebar button | Not routed |

### Planned 📋

| Feature | Priority | Notes |
|:---|:---|:---|
| Advanced search filters | Medium | Filter button exists in KrithiList; modal/dropdown UI not yet built |
| Pagination (server-side) | Medium | Client-side display only; API pagination params available but not wired |
| User & role CRUD | Low | Backend endpoints exist; UI needs implementation |
| Sourcing & extraction monitoring | High | See [Sourcing UI/UX Plan](../../01-requirements/krithi-data-sourcing/ui-ux-plan.md) |
| Quality dashboard | Medium | Planned for Phase 4 of sourcing strategy |
| Dark mode | Low | Tailwind supports it; not yet configured |

---

## 15. Key Constraints

- **No `any` type** – use proper TypeScript types throughout
- **Validate `musicalForm`** before showing notation editor tab
- **Use React Query** for all server state fetching
- **Follow existing component patterns** in `src/components`
- **Keep DTOs in sync** with `modules/shared/domain` Kotlin models
- **Tailwind utility classes only** – no CSS modules or styled-components
- **Bun** as package manager (not npm/yarn)
- **All mutations must trigger audit_log** on the backend
