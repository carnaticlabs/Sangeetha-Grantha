# Conductor Track: Reference Data Refactoring
**ID:** TRACK-024
**Status:** Completed
**Owner:** UI Team

## Objective
Refactor `ReferenceData.tsx` to eliminate code duplication and improve type safety.

## Scope
-   Refactor `ReferenceData.tsx` (966 lines).
-   Extract generic Entity Form and List components.
-   Consolidate validation logic.

## Checklist
- [x] **Type Cleanup**
    - [x] Remove duplicate interface definitions in `ReferenceData.tsx`.
    - [x] Import shared types from `src/types/`.
- [x] **Component Extraction**
    - [x] Create `src/components/reference-data/EntityForm.tsx` (Generic)
    - [x] Create `src/components/reference-data/EntityList.tsx` (Generic)
- [x] **Logic Extraction**
    - [x] Create `src/hooks/useEntityCrud.ts`
    - [x] Create `src/utils/reference-data-mappers.ts`
    - [x] Create `src/utils/reference-data-validation.ts`
- [x] **Integration**
    - [x] Refactor `src/pages/ReferenceData.tsx` to use the new components.

## Acceptance Criteria
- [x] `ReferenceData.tsx` is < 400 lines.
- [x] No duplicate logic for Raga, Tala, Composer, Temple, Deity.
- [x] All forms use the shared `FormInput` components.
