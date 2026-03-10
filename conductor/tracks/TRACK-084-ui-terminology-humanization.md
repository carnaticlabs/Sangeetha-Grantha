| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-084 |
| **Title** | UI Terminology Humanization for Musicologist Persona |
| **Status** | Completed |
| **Created** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | — |

# TRACK-084: UI Terminology Humanization for Musicologist Persona

## Objective

Replace engineering-oriented terminology across the Bulk Import and Sourcing & Quality UI with language that resonates with a Carnatic music musicologist. Pure string/label changes — no structural or routing modifications.

## Scope

### Sidebar Renames
- "Bulk Import" → "Add Compositions"
- "Sourcing & Quality" → "Collection Review"

### Bulk Import Page
- Page title, subtitle, form labels, pipeline stage names, status labels, job type labels, task log drawer labels, event type labels
- Add `statusLabel`, `taskStatusLabel`, `jobTypeLabel`, `eventTypeLabel` mapping objects
- Add `formatDuration` helper for human-readable times

### Sourcing Pages (All 6)
- Dashboard: title, subtitle, metric labels, quick stats, button labels
- Source Registry: title, subtitle, button labels, column headers
- Extraction Monitor: title, subtitle, button labels, column headers
- Source Evidence: title, subtitle, filter labels, column headers
- Structural Voting: title, subtitle
- Quality Dashboard: title, subtitle, KPI labels, button labels, phase label format, empty state messages
- SourcingLayout: breadcrumb label

## Files Modified

- `src/components/Sidebar.tsx`
- `src/components/sourcing/SourcingLayout.tsx`
- `src/pages/BulkImport.tsx`
- `src/pages/sourcing/SourcingDashboardPage.tsx`
- `src/pages/sourcing/SourceRegistryPage.tsx`
- `src/pages/sourcing/ExtractionMonitorPage.tsx`
- `src/pages/sourcing/SourceEvidencePage.tsx`
- `src/pages/sourcing/StructuralVotingPage.tsx`
- `src/pages/sourcing/QualityDashboardPage.tsx`

## Verification

- `bun run build` passes
- Visual check of every modified page
- Grep for old terms confirms none remain in UI-facing strings
