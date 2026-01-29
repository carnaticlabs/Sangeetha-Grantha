| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.1 |
| **Last Updated** | 2026-01-29 |
| **Author** | Sangeetha Grantha Team |

# KrithiEditor.tsx Refactoring Checklist

**Related Review:** [krithi-editor-code-review.md](./krithi-editor-code-review.md)
**Target File:** `modules/frontend/sangita-admin-web/src/pages/KrithiEditor.tsx`

---

## Priority 1: Critical (Foundation)

These items block other improvements and should be done first.

### 1.1 Extract Tab Components
- [x] Create `components/krithi-editor/` directory structure
- [x] Extract `MetadataTab.tsx` (~200 lines)
  - [x] Move Identity section
  - [x] Move Canonical Links section
  - [x] Move Language & Form section
  - [x] Move Additional Information section
  - [x] Move Status sidebar
- [x] Extract `StructureTab.tsx` (~130 lines)
  - [x] Move section list rendering
  - [x] Move add/remove section handlers
- [x] Extract `LyricsTab.tsx` (~220 lines)
  - [x] Move variant list rendering
  - [x] Move variant editor
  - [x] Move AI generation button logic
- [x] Extract `TagsTab.tsx` (~120 lines)
  - [x] Move tag search/filter
  - [x] Move tag assignment UI
- [x] Extract `NotationTab.tsx` (already exists, verify integration)
- [x] Extract `AuditTab.tsx` (~80 lines)
  - [x] Move audit log rendering
- [x] Create `index.ts` barrel export

### 1.2 Define TypeScript Interfaces
- [x] Create `types/krithi-editor.types.ts`
- [x] Define `InputFieldProps` interface (in FormInput.tsx)
- [x] Define `SelectFieldProps` interface (in FormSelect.tsx)
- [x] Define `TextareaFieldProps` interface (in FormTextarea.tsx)
- [x] Define `CheckboxFieldProps` interface (in FormCheckbox.tsx)
- [ ] Define `SectionHeaderProps` interface
- [x] Define `TabProps` interface for each tab component
- [x] Define `KrithiEditorState` interface
- [x] Define `KrithiEditorActions` type (for useReducer)
- [x] Remove all `any` type usages (15+ occurrences)
- [x] Replace unsafe type assertions (`as any`) with proper types

### 1.3 Implement useReducer for Form State
- [x] Create `hooks/useKrithiEditorReducer.ts`
- [x] Define action types:
  - [x] `SET_KRITHI`
  - [x] `UPDATE_FIELD`
  - [ ] `SET_COMPOSER`
  - [ ] `SET_TALA`
  - [ ] `ADD_RAGA` / `REMOVE_RAGA`
  - [ ] `SET_DEITY`
  - [ ] `SET_TEMPLE`
  - [x] `ADD_SECTION` / `UPDATE_SECTION` / `REMOVE_SECTION`
  - [x] `ADD_VARIANT` / `UPDATE_VARIANT` / `REMOVE_VARIANT`
  - [ ] `ADD_TAG` / `REMOVE_TAG`
  - [x] `SET_LOADING_STATE`
- [x] Create reducer function with proper typing
- [x] Create initial state factory
- [x] Replace 25+ useState calls with single useReducer
- [x] Create dispatch wrapper functions for common actions

### 1.4 Extract Business Logic
- [x] Create `utils/krithi-mapper.ts`
  - [x] Move `mapKrithiDtoToDetail()` function
  - [x] Add proper input/output types
  - [ ] Add unit tests
- [x] Create `utils/krithi-payload-builder.ts` (Integrated into `krithi-mapper.ts`)
  - [x] Extract payload construction from `handleSave`
  - [x] Create `buildCreatePayload()` function
  - [x] Create `buildUpdatePayload()` function
  - [ ] Add validation logic
  - [ ] Add unit tests
- [ ] Create `utils/section-id-mapper.ts`
  - [ ] Extract section ID mapping logic
  - [ ] Add unit tests

---

## Priority 2: High (Stability & Robustness)

### 2.1 Create Custom Data Fetching Hooks
- [x] Create `hooks/useKrithiData.ts`
  - [x] Consolidate krithi loading logic
  - [x] Handle loading/error states
  - [ ] Implement request cancellation with AbortController
- [x] Create `hooks/useReferenceData.ts`
  - [x] Load composers, ragas, talas, deities, temples, sampradayas
  - [x] Cache reference data appropriately
- [x] Create `hooks/useKrithiSections.ts` (Integrated into `useKrithiData.ts`)
  - [x] Handle section loading
  - [x] Remove magic timeout workaround
- [x] Create `hooks/useKrithiLyricVariants.ts` (Integrated into `useKrithiData.ts`)
  - [x] Handle variant loading
- [x] Create `hooks/useKrithiTags.ts` (Integrated into `useKrithiData.ts`)
  - [x] Handle tag loading
- [x] Create `hooks/useKrithiAuditLogs.ts` (Integrated into `useKrithiData.ts`)
  - [x] Handle audit log loading

### 2.2 Implement Error Boundary
- [x] Create `components/ErrorBoundary.tsx`
- [x] Wrap KrithiEditor with error boundary
- [x] Create fallback UI for errors
- [ ] Add error reporting/logging

### 2.3 Standardize Error Handling
- [x] Create `utils/error-handler.ts` (Already exists)
- [x] Define error display strategy (toast for user errors, console for dev)
- [x] Replace `alert()` calls with toast
- [x] Replace silent `console.error` with appropriate user feedback
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
- [x] Create `components/form/FormInput.tsx` with proper types
- [x] Create `components/form/FormSelect.tsx` with proper types
- [x] Create `components/form/FormTextarea.tsx` with proper types
- [x] Create `components/form/FormCheckbox.tsx` with proper types
- [ ] Create `components/form/FormField.tsx` wrapper with label/error
- [ ] Add validation state support (error, success, warning)
- [x] Add disabled state styling
- [x] Add required field indicator
- [x] Export from `components/form/index.ts`

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
| P1 Critical | 47 | 45 | 96% |
| P2 High | 30 | 14 | 47% |
| P3 Medium | 28 | 6 | 21% |
| P4 Nice-to-have | 16 | 0 | 0% |
| **Total** | **121** | **65** | **54%** |

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
