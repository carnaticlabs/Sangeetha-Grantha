| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-02 |
| **Author** | Sangeetha Grantha Team |

# Frontend Pages Refactoring Checklist

**Related Review:** [frontend-pages-code-review.md](./frontend-pages-code-review.md)
**Scope:** All pages in `modules/frontend/sangita-admin-web/src/pages/`

---

## Priority 1: Critical (KrithiEditor + ReferenceData + Cross-Cutting)

### 1.1 KrithiEditor.tsx Refactoring
See detailed checklist: [krithi-editor-refactor-checklist.md](./krithi-editor-refactor-checklist.md)

**Summary:** 47 P1 items including tab extraction, TypeScript interfaces, useReducer implementation, and business logic extraction.

---

### 1.2 ReferenceData.tsx Decomposition

#### 1.2.1 Remove Duplicate Type Definitions
- [ ] Delete local `Composer` interface (Lines 42-48)
- [ ] Delete local `Raga` interface (Lines 50-57)
- [ ] Delete local `Tala` interface (Lines 59-64)
- [ ] Delete local `Temple` interface (Lines 66-73)
- [ ] Delete local `DeityItem` interface (Lines 75-78)
- [ ] Import all types from `../types` instead
- [ ] Update `ReferenceItem` union type to use imported types
- [ ] Fix all type mismatches from the change

#### 1.2.2 Extract Entity Form Component
- [ ] Create `components/reference-data/EntityForm.tsx`
- [ ] Move EntityForm component (Lines 201-650)
- [ ] Define `EntityFormProps` interface
- [ ] Export from barrel file

#### 1.2.3 Extract Form Input Components
- [ ] Create `components/form/FormInput.tsx` with proper types
- [ ] Create `components/form/FormTextarea.tsx` with proper types
- [ ] Create `components/form/FormSelect.tsx` with proper types
- [ ] Define interfaces:
  ```typescript
  interface FormInputProps {
    label: string;
    name: string;
    value: string;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    placeholder?: string;
    required?: boolean;
    type?: 'text' | 'number';
    help?: string;
    className?: string;
    readOnly?: boolean;
  }
  ```
- [ ] Remove `any` types from all form components

#### 1.2.4 Extract Data Mappers
- [ ] Create `utils/reference-data-mappers.ts`
- [ ] Move `mapComposerToItem()` function
- [ ] Move `mapRagaToItem()` function
- [ ] Move `mapTalaToItem()` function
- [ ] Move `mapTempleToItem()` function
- [ ] Move `mapDeityToItem()` function
- [ ] Add proper input/output types
- [ ] Add unit tests for mappers

#### 1.2.5 Consolidate Validation Logic
- [ ] Create `utils/reference-data-validation.ts`
- [ ] Extract common validation pattern:
  ```typescript
  const validateRequiredFields = (entityType: EntityType, formData: FormData): string | null
  ```
- [ ] Replace 5 duplicate validation blocks with single call

#### 1.2.6 Simplify Entity CRUD Logic
- [ ] Create `hooks/useEntityCrud.ts` custom hook
- [ ] Abstract create/update/delete patterns
- [ ] Reduce switch statement complexity
- [ ] Handle loading/error states consistently

#### 1.2.7 Fix TypeScript Issues
- [ ] Replace `Record<string, any>` with typed form state
- [ ] Remove `any` from FormInput props
- [ ] Remove `any` from FormTextarea props
- [ ] Remove `any` from FormSelect props
- [ ] Add proper types to all event handlers

---

### 1.3 Cross-Cutting: Remove All `any` Types

#### Frontend-wide Type Cleanup
- [ ] **AutoApproveQueue.tsx** - Replace `params: any` with typed interface (Line 37)
- [ ] **BulkImport.tsx** - Review and type all API responses
- [ ] **ImportReview.tsx** - Add proper type for JSON-parsed resolution data
- [ ] **TagsPage.tsx** - Already well-typed, verify no regressions
- [ ] Create shared `types/api-params.ts` for request parameter types
- [ ] Create shared `types/form-state.ts` for common form patterns

---

### 1.4 Cross-Cutting: Standardize Error Handling

#### Create Error Handling Utilities
- [ ] Create `utils/error-handler.ts`
- [ ] Define `handleApiError(error: unknown, toast: ToastContext): void`
- [ ] Define `formatErrorMessage(error: unknown): string`
- [ ] Replace all `alert()` calls with toast notifications
- [ ] Replace all `console.error` + silent failure with toast

#### Apply to Each Page
- [ ] AutoApproveQueue.tsx - Use toast for all errors
- [ ] BulkImport.tsx - Use toast for all errors
- [ ] Dashboard.tsx - Use toast instead of error state for non-blocking errors
- [ ] ImportReview.tsx - Use toast consistently
- [ ] ImportsPage.tsx - Already using toast
- [ ] KrithiEditor.tsx - Replace `alert()` with toast
- [ ] KrithiList.tsx - Add toast for search errors
- [ ] ReferenceData.tsx - Use toast consistently
- [ ] TagsPage.tsx - Already using toast

---

## Priority 2: High (BulkImport + ImportReview + Modals)

### 2.1 BulkImport.tsx Improvements

#### 2.1.1 Extract Action Handler
- [ ] Create `hooks/useBatchActions.ts`
- [ ] Move `triggerAction` logic to hook
- [ ] Break into smaller, testable functions:
  - [ ] `handlePause(batchId: string)`
  - [ ] `handleResume(batchId: string)`
  - [ ] `handleCancel(batchId: string)`
  - [ ] `handleRetry(batchId: string)`
  - [ ] `handleDelete(batchId: string)`
  - [ ] `handleApproveAll(batchId: string)`
  - [ ] `handleRejectAll(batchId: string)`
  - [ ] `handleFinalize(batchId: string)`
  - [ ] `handleExport(batchId: string, format: string)`

#### 2.1.2 Extract Modal Components
- [ ] Create `components/bulk-import/TaskLogDrawer.tsx` (Lines 261-364)
- [ ] Create `components/bulk-import/DeleteConfirmModal.tsx` (Lines 366-387)
- [ ] Define proper props interfaces
- [ ] Add accessibility attributes (aria-modal, aria-labelledby)

#### 2.1.3 Improve Polling
- [ ] Add AbortController to API calls
- [ ] Add polling cleanup on unmount
- [ ] Consider using a polling hook or library
- [ ] Add exponential backoff on errors

#### 2.1.4 Remove Console Logs
- [ ] Remove `console.log` at Line 182
- [ ] Remove any other debug logging

---

### 2.2 ImportReview.tsx Improvements

#### 2.2.1 Add JSON Validation
- [ ] Install `zod` or use runtime validation
- [ ] Create schema for `ResolutionResult`
- [ ] Validate JSON before using
- [ ] Handle validation errors gracefully

#### 2.2.2 Improve Bulk Operations
- [ ] Use `Promise.allSettled` instead of `Promise.all`
- [ ] Report partial success/failure counts
- [ ] Allow retry of failed items

#### 2.2.3 Consolidate Form State
- [ ] Replace 6 individual useState calls:
  ```typescript
  const [formState, setFormState] = useState({
    title: '', composer: '', raga: '', tala: '', language: '', lyrics: ''
  });
  ```
- [ ] Create `updateField(field: string, value: string)` helper

---

### 2.3 Create Accessible Modal Component

#### 2.3.1 Build Modal Infrastructure
- [ ] Create `components/Modal.tsx`
- [ ] Implement focus trap
- [ ] Add keyboard escape handler
- [ ] Add ARIA attributes
- [ ] Add backdrop click handling
- [ ] Support multiple sizes

#### 2.3.2 Create Confirmation Modal
- [ ] Create `components/ConfirmationModal.tsx`
- [ ] Props: `title`, `message`, `onConfirm`, `onCancel`, `confirmText`, `cancelText`, `variant`
- [ ] Replace all `window.confirm()` calls:
  - [ ] AutoApproveQueue.tsx Line 56
  - [ ] BulkImport.tsx Lines 190, 194, 199, 208
  - [ ] ImportReview.tsx Lines 94, 114, 137
  - [ ] TagsPage.tsx Line 100

---

### 2.4 Implement Request Cancellation

#### 2.4.1 Add AbortController Support
- [ ] Create `hooks/useAbortController.ts`
- [ ] Integrate with all data fetching hooks
- [ ] Cancel on unmount
- [ ] Cancel on parameter change

#### 2.4.2 Apply to Pages
- [ ] AutoApproveQueue.tsx - Cancel queue load on filter change
- [ ] BulkImport.tsx - Cancel batch detail load on selection change
- [ ] Dashboard.tsx - Cancel stats load on unmount
- [ ] ImportReview.tsx - Cancel imports load on unmount
- [ ] KrithiList.tsx - Cancel search on new search term
- [ ] ReferenceData.tsx - Cancel entity load on type change

---

## Priority 3: Medium (Minor Pages + Accessibility)

### 3.1 TagsPage.tsx Cleanup

- [ ] Remove console.log statements (Lines 44, 52-57, 64)
- [ ] Extract validation to shared utility
- [ ] Add debounce to search input
- [ ] Consolidate form reset logic into single function

---

### 3.2 ImportsPage.tsx Cleanup

- [ ] Remove unnecessary toast wrapper object
- [ ] Cache data between tab switches
- [ ] Remove error re-throw after toast (Line 70)
- [ ] Add loading state for scrape operation

---

### 3.3 Dashboard.tsx Improvements

- [ ] Extract StatCard to `components/dashboard/StatCard.tsx`
- [ ] Extract RecentItem to `components/dashboard/RecentItem.tsx`
- [ ] Wrap with `React.memo`
- [ ] Make Curator Tasks dynamic (fetch from API)

---

### 3.4 AutoApproveQueue.tsx Cleanup

- [ ] Create typed interface for queue params
- [ ] Remove duplicate useEffect for initial load
- [ ] Replace `confirm()` with ConfirmationModal
- [ ] Add proper TypeScript types

---

### 3.5 KrithiList.tsx Cleanup

- [ ] Remove unused `STATUS_Styles` constant
- [ ] Implement functional pagination:
  - [ ] Add page state
  - [ ] Update API call with pagination params
  - [ ] Enable Previous/Next buttons

---

### 3.6 Accessibility Improvements

#### Add ARIA Attributes
- [ ] **Tabs**: Add `role="tablist"`, `role="tab"`, `role="tabpanel"` where applicable
  - [ ] ImportsPage.tsx
  - [ ] KrithiEditor.tsx
- [ ] **Tables**: Add `role="grid"` or use semantic `<table>` with proper headers
- [ ] **Forms**: Add `aria-describedby` for field errors
- [ ] **Loading states**: Add `aria-busy="true"` and `aria-live="polite"`

#### Keyboard Navigation
- [ ] Tab navigation in all forms
- [ ] Escape key closes modals
- [ ] Arrow keys for tab switching (where applicable)

---

## Priority 4: Nice-to-Have (Polish)

### 4.1 Performance Optimizations

#### Add React.memo
- [ ] Wrap all extracted sub-components with `React.memo`
- [ ] Add comparison functions where needed

#### Add useCallback
- [ ] Wrap event handlers passed to child components
- [ ] Especially important for list item handlers

#### Add useMemo
- [ ] Memoize filtered/sorted lists
- [ ] Memoize computed values

#### Add Virtualization
- [ ] Install `react-virtual` or `react-window`
- [ ] Apply to BulkImport task list
- [ ] Apply to KrithiList (if large datasets expected)
- [ ] Apply to ReferenceData entity lists

---

### 4.2 UX Improvements

#### Skeleton Loaders
- [ ] Create `components/Skeleton.tsx`
- [ ] Add loading skeletons to:
  - [ ] Dashboard stats
  - [ ] KrithiList table
  - [ ] ReferenceData lists
  - [ ] TagsPage table

#### Optimistic Updates
- [ ] Implement for TagsPage CRUD
- [ ] Implement for ImportReview approve/reject
- [ ] Add rollback on error

---

### 4.3 Code Style Consistency

#### Naming Conventions
- [ ] Standardize handler naming (`handle*` prefix)
- [ ] Standardize state naming
- [ ] Standardize file naming (kebab-case for new files)

#### Clean Up
- [ ] Remove all `console.log` statements
- [ ] Remove commented-out code
- [ ] Add JSDoc comments to public functions
- [ ] Sort imports consistently (external, internal, relative)

---

## File Structure After Refactoring

```
src/
├── pages/
│   ├── AutoApproveQueue.tsx      # ~150 lines
│   ├── BulkImport.tsx            # ~400 lines
│   ├── Dashboard.tsx             # ~150 lines
│   ├── ImportReview.tsx          # ~300 lines
│   ├── ImportsPage.tsx           # ~200 lines
│   ├── KrithiEditor.tsx          # ~200 lines (orchestration)
│   ├── KrithiList.tsx            # ~150 lines
│   ├── ReferenceData.tsx         # ~300 lines (orchestration)
│   ├── RolesPage.tsx             # Placeholder
│   ├── TagsPage.tsx              # ~250 lines
│   └── UsersPage.tsx             # Placeholder
├── components/
│   ├── form/
│   │   ├── FormInput.tsx
│   │   ├── FormTextarea.tsx
│   │   ├── FormSelect.tsx
│   │   ├── FormCheckbox.tsx
│   │   └── index.ts
│   ├── Modal.tsx
│   ├── ConfirmationModal.tsx
│   ├── Skeleton.tsx
│   ├── krithi-editor/
│   │   ├── MetadataTab.tsx
│   │   ├── StructureTab.tsx
│   │   ├── LyricsTab.tsx
│   │   ├── TagsTab.tsx
│   │   ├── AuditTab.tsx
│   │   └── index.ts
│   ├── reference-data/
│   │   ├── EntityForm.tsx
│   │   ├── EntityList.tsx
│   │   └── index.ts
│   ├── bulk-import/
│   │   ├── TaskLogDrawer.tsx
│   │   ├── DeleteConfirmModal.tsx
│   │   ├── BatchTable.tsx
│   │   └── index.ts
│   └── dashboard/
│       ├── StatCard.tsx
│       ├── RecentItem.tsx
│       └── index.ts
├── hooks/
│   ├── useKrithiEditorReducer.ts
│   ├── useKrithiData.ts
│   ├── useReferenceData.ts
│   ├── useEntityCrud.ts
│   ├── useBatchActions.ts
│   ├── useAbortController.ts
│   └── index.ts
├── utils/
│   ├── error-handler.ts
│   ├── reference-data-mappers.ts
│   ├── reference-data-validation.ts
│   ├── krithi-mapper.ts
│   └── index.ts
└── types/
    ├── api-params.ts
    ├── form-state.ts
    ├── krithi-editor.types.ts
    └── index.ts
```

---

## Progress Tracking

| Priority | Section | Total Items | Completed | % |
|----------|---------|-------------|-----------|---|
| P1 | KrithiEditor | 47 | 0 | 0% |
| P1 | ReferenceData | 32 | 0 | 0% |
| P1 | Remove `any` Types | 8 | 0 | 0% |
| P1 | Standardize Errors | 11 | 0 | 0% |
| P2 | BulkImport | 16 | 0 | 0% |
| P2 | ImportReview | 7 | 0 | 0% |
| P2 | Modal Component | 12 | 0 | 0% |
| P2 | Request Cancellation | 8 | 0 | 0% |
| P3 | TagsPage | 4 | 0 | 0% |
| P3 | ImportsPage | 4 | 0 | 0% |
| P3 | Dashboard | 4 | 0 | 0% |
| P3 | AutoApproveQueue | 4 | 0 | 0% |
| P3 | KrithiList | 2 | 0 | 0% |
| P3 | Accessibility | 8 | 0 | 0% |
| P4 | Performance | 8 | 0 | 0% |
| P4 | UX (Skeletons) | 6 | 0 | 0% |
| P4 | Code Style | 4 | 0 | 0% |
| **TOTAL** | | **185** | **0** | **0%** |

---

## Definition of Done

For each checklist item:
1. Code compiles without TypeScript errors
2. No `any` types (where applicable)
3. Unit tests pass (where applicable)
4. Component renders correctly
5. No console errors/warnings
6. Accessibility checked (keyboard, screen reader)
7. Code reviewed

---

## Suggested Execution Order

### Phase 1: Foundation (Weeks 1-2)
1. Create shared form components with types
2. Create error handling utility
3. Create Modal/ConfirmationModal
4. Create useAbortController hook

### Phase 2: Critical Pages (Weeks 3-5)
1. Refactor KrithiEditor (largest effort)
2. Refactor ReferenceData
3. Apply new components to both

### Phase 3: Secondary Pages (Weeks 6-7)
1. Improve BulkImport
2. Improve ImportReview
3. Apply ConfirmationModal everywhere

### Phase 4: Polish (Week 8)
1. Clean up minor pages
2. Add accessibility
3. Performance optimizations
4. Documentation
