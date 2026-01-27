# Frontend Pages Refactoring Plan

**Track:** TRACK-021-frontend-pages-refactoring
**Status:** Planned
**Priority:** P1

---

## Summary

Refactor 9 React pages (~5,200 lines) to address 59+ identified issues including type safety gaps, inconsistent error handling, monolithic components, and missing accessibility. Target: ~2,100 lines with proper component decomposition.

---

## Phases

### Phase 1: Foundation Infrastructure
**Files to Create:**
- `src/components/form/FormInput.tsx` - Typed text input
- `src/components/form/FormTextarea.tsx` - Typed textarea
- `src/components/form/FormSelect.tsx` - Typed dropdown
- `src/components/form/FormCheckbox.tsx` - Typed checkbox
- `src/components/form/index.ts` - Barrel exports
- `src/components/Modal.tsx` - Accessible modal with ARIA
- `src/components/ConfirmationModal.tsx` - Replace browser confirm()
- `src/utils/error-handler.ts` - Standardized error handling
- `src/hooks/useAbortController.ts` - Request cancellation
- `src/types/api-params.ts` - API parameter types

**Acceptance:** Zero `any` types in new code, ARIA attributes on modals

### Phase 2: KrithiEditor Refactoring (1,860 → ~200 lines)
**Files to Create:**
- `src/components/krithi-editor/MetadataTab.tsx`
- `src/components/krithi-editor/StructureTab.tsx`
- `src/components/krithi-editor/LyricsTab.tsx`
- `src/components/krithi-editor/TagsTab.tsx`
- `src/components/krithi-editor/AuditTab.tsx`
- `src/hooks/useKrithiEditorReducer.ts` - Replace 25+ useState
- `src/hooks/useKrithiData.ts` - Data fetching with AbortController
- `src/hooks/useReferenceData.ts` - Reference entities loading
- `src/utils/krithi-mapper.ts` - DTO transformation
- `src/utils/krithi-payload-builder.ts` - API payload construction
- `src/types/krithi-editor.types.ts` - Component interfaces

**File to Modify:**
- `src/pages/KrithiEditor.tsx` - Reduce to orchestration only

**Acceptance:** All 15+ `any` types removed, 25+ useState → useReducer

### Phase 3: ReferenceData Refactoring (966 → ~300 lines)
**Files to Create:**
- `src/components/reference-data/EntityForm.tsx`
- `src/components/reference-data/EntityList.tsx`
- `src/utils/reference-data-mappers.ts`
- `src/utils/reference-data-validation.ts`
- `src/hooks/useEntityCrud.ts`

**File to Modify:**
- `src/pages/ReferenceData.tsx` - Remove duplicate types (lines 42-78), use imports from `../types`

**Acceptance:** No duplicate type definitions, validation consolidated

### Phase 4: Secondary Pages Standardization
**Files to Create:**
- `src/components/bulk-import/TaskLogDrawer.tsx`
- `src/components/bulk-import/DeleteConfirmModal.tsx`
- `src/components/dashboard/StatCard.tsx`
- `src/components/dashboard/RecentItem.tsx`
- `src/hooks/useBatchActions.ts`

**Files to Modify:**
- `src/pages/BulkImport.tsx` - Extract drawer, add AbortController, remove console.log (line 182)
- `src/pages/ImportReview.tsx` - Promise.allSettled, consolidate form state
- `src/pages/TagsPage.tsx` - Remove console.logs (lines 44, 52-57, 64), add debounce
- `src/pages/AutoApproveQueue.tsx` - Type params (line 37), fix duplicate useEffect
- `src/pages/KrithiList.tsx` - Remove unused STATUS_Styles, implement pagination
- `src/pages/Dashboard.tsx` - Extract sub-components
- `src/pages/ImportsPage.tsx` - Remove toast wrapper, cache data

**Acceptance:** All confirm() replaced, all console.log removed

### Phase 5: Accessibility & Type Safety Hardening
**Files to Modify:**
- All pages - Add ARIA attributes for tabs, modals, loading states
- `src/api/client.ts` - Type all payload parameters (remove `any`)

**Acceptance:** Zero `any` types in codebase, keyboard navigation works

### Phase 6: Performance & Polish
**All extracted components:**
- Add React.memo wrappers
- Add useCallback for handlers passed to children
- Add useMemo for computed values

**Files to Create:**
- `src/components/Skeleton.tsx` - Loading skeleton component

**Acceptance:** No lint warnings, all components memoized

---

## Critical Files

| File | Current Lines | Target Lines | Key Issues |
|------|--------------|--------------|------------|
| `src/pages/KrithiEditor.tsx` | 1,860 | 200 | 25+ useState, 15+ `any`, 280-line save |
| `src/pages/ReferenceData.tsx` | 966 | 300 | Duplicate types, repetitive validation |
| `src/pages/BulkImport.tsx` | 772 | 400 | Large triggerAction, polling leaks |
| `src/pages/ImportReview.tsx` | 414 | 300 | Unvalidated JSON, Promise.all |

---

## Verification

1. **Build:** `cd modules/frontend/sangita-admin-web && bun run build` - zero errors
2. **Lint:** `bun run lint` - zero warnings
3. **Manual Testing:**
   - Create/edit krithi with all fields
   - Reference data CRUD (all 5 entity types)
   - Bulk import workflow
   - Tab keyboard navigation
4. **Type Check:** No `any` types (`grep -r ": any" src/`)

---

## Track Document

Create `conductor/tracks/TRACK-021-frontend-pages-refactoring.md` with phases above.

Update `conductor/tracks.md` to add:
```
| [TRACK-021](./tracks/TRACK-021-frontend-pages-refactoring.md) | Frontend Pages Refactoring | In Progress |
```
