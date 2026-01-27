# KrithiEditor.tsx Refactoring Checklist

**Related Review:** [krithi-editor-code-review.md](./krithi-editor-code-review.md)
**Target File:** `modules/frontend/sangita-admin-web/src/pages/KrithiEditor.tsx`

---

## Priority 1: Critical (Foundation)

These items block other improvements and should be done first.

### 1.1 Extract Tab Components
- [ ] Create `components/krithi-editor/` directory structure
- [ ] Extract `MetadataTab.tsx` (~200 lines)
  - [ ] Move Identity section
  - [ ] Move Canonical Links section
  - [ ] Move Language & Form section
  - [ ] Move Additional Information section
  - [ ] Move Status sidebar
- [ ] Extract `StructureTab.tsx` (~130 lines)
  - [ ] Move section list rendering
  - [ ] Move add/remove section handlers
- [ ] Extract `LyricsTab.tsx` (~220 lines)
  - [ ] Move variant list rendering
  - [ ] Move variant editor
  - [ ] Move AI generation button logic
- [ ] Extract `TagsTab.tsx` (~120 lines)
  - [ ] Move tag search/filter
  - [ ] Move tag assignment UI
- [ ] Extract `NotationTab.tsx` (already exists, verify integration)
- [ ] Extract `AuditTab.tsx` (~80 lines)
  - [ ] Move audit log rendering
- [ ] Create `index.ts` barrel export

### 1.2 Define TypeScript Interfaces
- [ ] Create `types/krithi-editor.types.ts`
- [ ] Define `InputFieldProps` interface
- [ ] Define `SelectFieldProps` interface
- [ ] Define `TextareaFieldProps` interface
- [ ] Define `CheckboxFieldProps` interface
- [ ] Define `SectionHeaderProps` interface
- [ ] Define `TabProps` interface for each tab component
- [ ] Define `KrithiEditorState` interface
- [ ] Define `KrithiEditorActions` type (for useReducer)
- [ ] Remove all `any` type usages (15+ occurrences)
- [ ] Replace unsafe type assertions (`as any`) with proper types

### 1.3 Implement useReducer for Form State
- [ ] Create `hooks/useKrithiEditorReducer.ts`
- [ ] Define action types:
  - [ ] `SET_KRITHI`
  - [ ] `UPDATE_FIELD`
  - [ ] `SET_COMPOSER`
  - [ ] `SET_TALA`
  - [ ] `ADD_RAGA` / `REMOVE_RAGA`
  - [ ] `SET_DEITY`
  - [ ] `SET_TEMPLE`
  - [ ] `ADD_SECTION` / `UPDATE_SECTION` / `REMOVE_SECTION`
  - [ ] `ADD_VARIANT` / `UPDATE_VARIANT` / `REMOVE_VARIANT`
  - [ ] `ADD_TAG` / `REMOVE_TAG`
  - [ ] `SET_LOADING_STATE`
- [ ] Create reducer function with proper typing
- [ ] Create initial state factory
- [ ] Replace 25+ useState calls with single useReducer
- [ ] Create dispatch wrapper functions for common actions

### 1.4 Extract Business Logic
- [ ] Create `utils/krithi-mapper.ts`
  - [ ] Move `mapKrithiDtoToDetail()` function
  - [ ] Add proper input/output types
  - [ ] Add unit tests
- [ ] Create `utils/krithi-payload-builder.ts`
  - [ ] Extract payload construction from `handleSave`
  - [ ] Create `buildCreatePayload()` function
  - [ ] Create `buildUpdatePayload()` function
  - [ ] Add validation logic
  - [ ] Add unit tests
- [ ] Create `utils/section-id-mapper.ts`
  - [ ] Extract section ID mapping logic
  - [ ] Add unit tests

---

## Priority 2: High (Stability & Robustness)

### 2.1 Create Custom Data Fetching Hooks
- [ ] Create `hooks/useKrithiData.ts`
  - [ ] Consolidate krithi loading logic
  - [ ] Handle loading/error states
  - [ ] Implement request cancellation with AbortController
- [ ] Create `hooks/useReferenceData.ts`
  - [ ] Load composers, ragas, talas, deities, temples, sampradayas
  - [ ] Cache reference data appropriately
- [ ] Create `hooks/useKrithiSections.ts`
  - [ ] Handle section loading
  - [ ] Remove magic timeout workaround
- [ ] Create `hooks/useKrithiLyricVariants.ts`
  - [ ] Handle variant loading
- [ ] Create `hooks/useKrithiTags.ts`
  - [ ] Handle tag loading
- [ ] Create `hooks/useKrithiAuditLogs.ts`
  - [ ] Handle audit log loading

### 2.2 Implement Error Boundary
- [ ] Create `components/ErrorBoundary.tsx`
- [ ] Wrap KrithiEditor with error boundary
- [ ] Create fallback UI for errors
- [ ] Add error reporting/logging

### 2.3 Standardize Error Handling
- [ ] Create `utils/error-handler.ts`
- [ ] Define error display strategy (toast for user errors, console for dev)
- [ ] Replace `alert()` calls with toast
- [ ] Replace silent `console.error` with appropriate user feedback
- [ ] Add error types/codes for different failure modes

### 2.4 Performance Optimizations
- [ ] Wrap `InputField` with `React.memo`
- [ ] Wrap `SelectField` with `React.memo`
- [ ] Wrap `TextareaField` with `React.memo`
- [ ] Wrap `CheckboxField` with `React.memo`
- [ ] Wrap `SectionHeader` with `React.memo`
- [ ] Add `useCallback` to `handleSave`
- [ ] Add `useCallback` to `handleComposerChange`
- [ ] Add `useCallback` to `handleTalaChange`
- [ ] Add `useCallback` to `handleRagaChange`
- [ ] Add `useCallback` to `handleDeityChange`
- [ ] Add `useCallback` to `handleTempleChange`
- [ ] Add `useCallback` to `handleDeitySave`
- [ ] Add `useCallback` to `handleTempleSave`
- [ ] Add `useMemo` for sorted sections list
- [ ] Add `useMemo` for filtered tags list

---

## Priority 3: Medium (Quality & Maintainability)

### 3.1 Accessibility Improvements
- [ ] Add `role="tablist"` to tab container
- [ ] Add `role="tab"` to each tab button
- [ ] Add `role="tabpanel"` to each tab content
- [ ] Add `aria-selected` to active tab
- [ ] Add `aria-controls` linking tabs to panels
- [ ] Add `id` attributes for ARIA relationships
- [ ] Add `aria-modal="true"` to modals
- [ ] Add `aria-labelledby` to modals
- [ ] Implement focus trap in modals
- [ ] Add keyboard navigation for tabs (arrow keys)
- [ ] Add `aria-describedby` for form field errors
- [ ] Test with screen reader

### 3.2 Create Reusable Form Components
- [ ] Create `components/form/FormInput.tsx` with proper types
- [ ] Create `components/form/FormSelect.tsx` with proper types
- [ ] Create `components/form/FormTextarea.tsx` with proper types
- [ ] Create `components/form/FormCheckbox.tsx` with proper types
- [ ] Create `components/form/FormField.tsx` wrapper with label/error
- [ ] Add validation state support (error, success, warning)
- [ ] Add disabled state styling
- [ ] Add required field indicator
- [ ] Export from `components/form/index.ts`

### 3.3 Add Unit Tests
- [ ] Test `mapKrithiDtoToDetail()` function
  - [ ] Test with full DTO
  - [ ] Test with minimal DTO
  - [ ] Test workflow state mapping
  - [ ] Test raga array mapping
- [ ] Test `buildCreatePayload()` function
- [ ] Test `buildUpdatePayload()` function
- [ ] Test section ID mapper
- [ ] Test reducer actions
- [ ] Test custom hooks with React Testing Library

### 3.4 Implement Request Cancellation
- [ ] Add AbortController to `useKrithiData` hook
- [ ] Add AbortController to `useReferenceData` hook
- [ ] Add AbortController to `useKrithiSections` hook
- [ ] Add AbortController to `useKrithiLyricVariants` hook
- [ ] Cancel pending requests on component unmount
- [ ] Cancel pending requests on krithiId change

---

## Priority 4: Nice-to-Have (Polish)

### 4.1 Consider Form Library Integration
- [ ] Evaluate react-hook-form for this use case
- [ ] If adopting:
  - [ ] Install react-hook-form
  - [ ] Create form schema with zod/yup
  - [ ] Migrate form fields to use register/control
  - [ ] Implement form-level validation
  - [ ] Add field-level error display

### 4.2 Implement Optimistic Updates
- [ ] Add optimistic update for section changes
- [ ] Add optimistic update for tag changes
- [ ] Add rollback on save failure
- [ ] Add visual indicator for pending changes

### 4.3 Add Storybook Documentation
- [ ] Install Storybook (if not present)
- [ ] Create stories for form components
- [ ] Create stories for tab components
- [ ] Document component props
- [ ] Add interaction tests

### 4.4 Code Style Cleanup
- [ ] Standardize handler naming (`handle*` prefix)
- [ ] Extract long className strings to variables or CSS modules
- [ ] Add JSDoc comments to public functions
- [ ] Remove unused imports
- [ ] Sort imports consistently

---

## File Structure After Refactoring

```
src/
├── pages/
│   └── KrithiEditor.tsx              # ~200 lines (orchestration only)
├── components/
│   ├── form/
│   │   ├── FormInput.tsx
│   │   ├── FormSelect.tsx
│   │   ├── FormTextarea.tsx
│   │   ├── FormCheckbox.tsx
│   │   ├── FormField.tsx
│   │   └── index.ts
│   ├── krithi-editor/
│   │   ├── MetadataTab.tsx
│   │   ├── StructureTab.tsx
│   │   ├── LyricsTab.tsx
│   │   ├── TagsTab.tsx
│   │   ├── AuditTab.tsx
│   │   ├── CanonicalFieldModal.tsx
│   │   └── index.ts
│   └── ErrorBoundary.tsx
├── hooks/
│   ├── useKrithiEditorReducer.ts
│   ├── useKrithiData.ts
│   ├── useReferenceData.ts
│   ├── useKrithiSections.ts
│   ├── useKrithiLyricVariants.ts
│   ├── useKrithiTags.ts
│   └── useKrithiAuditLogs.ts
├── utils/
│   ├── krithi-mapper.ts
│   ├── krithi-payload-builder.ts
│   ├── section-id-mapper.ts
│   └── error-handler.ts
├── types/
│   └── krithi-editor.types.ts
└── __tests__/
    ├── krithi-mapper.test.ts
    ├── krithi-payload-builder.test.ts
    └── useKrithiEditorReducer.test.ts
```

---

## Progress Tracking

| Priority | Total Items | Completed | Percentage |
|----------|-------------|-----------|------------|
| P1 Critical | 47 | 0 | 0% |
| P2 High | 30 | 0 | 0% |
| P3 Medium | 28 | 0 | 0% |
| P4 Nice-to-have | 16 | 0 | 0% |
| **Total** | **121** | **0** | **0%** |

---

## Definition of Done

For each checklist item:
1. Code is written and compiles without errors
2. No TypeScript `any` types (where applicable)
3. Unit tests pass (where applicable)
4. Component renders correctly in browser
5. No console errors or warnings
6. Code reviewed (self or peer)

---

## Notes

- Start with Priority 1 items as they establish the foundation
- P1.1 (Extract Tab Components) and P1.3 (useReducer) can be done in parallel
- P1.2 (TypeScript) should be done alongside P1.1
- Consider creating a feature branch: `refactor/krithi-editor-cleanup`
- Break into multiple PRs if the change becomes too large
