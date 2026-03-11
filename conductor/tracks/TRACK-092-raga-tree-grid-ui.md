| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.1 |
| **Last Updated** | 2026-03-11 |
| **Author** | Sangeetha Grantha Team |

# Goal

Replace the flat EntityList for ragas with a purpose-built tree-grid that reflects the melakarta-janya hierarchy. With 962 ragas now in the database, the UI needs to show the 72 melakarta parent ragas as expandable rows with their janya children, displaying arohanam and avarohanam details inline.

# Implementation Plan

- [x] Create conductor track
- [x] Build `RagaTreeGrid.tsx` component with tree-grid layout
- [x] Wire into `ReferenceData.tsx` for Ragas type
- [x] Verify with preview screenshot

# Files

| File | Action |
|:---|:---|
| `modules/frontend/sangita-admin-web/src/components/reference-data/RagaTreeGrid.tsx` | New |
| `modules/frontend/sangita-admin-web/src/components/reference-data/index.ts` | Edit |
| `modules/frontend/sangita-admin-web/src/pages/ReferenceData.tsx` | Edit |
