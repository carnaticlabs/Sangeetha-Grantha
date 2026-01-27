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
