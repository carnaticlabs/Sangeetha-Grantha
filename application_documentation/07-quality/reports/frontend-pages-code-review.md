# Frontend Pages Code Quality Analysis Report

**Review Date:** 2026-01-27
**Reviewer:** Claude Code Analysis
**Scope:** `modules/frontend/sangita-admin-web/src/pages/*.tsx`

---

## Executive Summary

| Page | Lines | Complexity | Type Safety | Issues | Priority |
|------|-------|------------|-------------|--------|----------|
| KrithiEditor.tsx | 1,860 | Critical | Poor | 15+ | P1 |
| ReferenceData.tsx | 966 | High | Poor | 12+ | P1 |
| BulkImport.tsx | 772 | High | Moderate | 10+ | P2 |
| ImportReview.tsx | 414 | Medium | Moderate | 6+ | P2 |
| TagsPage.tsx | 317 | Low | Good | 4+ | P3 |
| ImportsPage.tsx | 244 | Low | Good | 3+ | P3 |
| Dashboard.tsx | 225 | Low | Good | 3+ | P3 |
| AutoApproveQueue.tsx | 215 | Low | Moderate | 4+ | P3 |
| KrithiList.tsx | 135 | Low | Good | 2+ | P4 |
| RolesPage.tsx | 39 | Placeholder | N/A | 0 | N/A |
| UsersPage.tsx | 38 | Placeholder | N/A | 0 | N/A |

**Total Issues Identified:** 59+
**Critical Files Requiring Immediate Attention:** 2 (KrithiEditor, ReferenceData)

---

## 1. KrithiEditor.tsx (1,860 lines)

**Already analyzed separately.** See [krithi-editor-code-review.md](./krithi-editor-code-review.md)

**Summary:** Monolithic component with 25+ useState calls, pervasive `any` types, complex effect chains, 280-line save handler. Requires significant decomposition.

---

## 2. ReferenceData.tsx (966 lines)

### Overview
Multi-view page managing Ragas, Talas, Composers, Temples, and Deities with HOME/LIST/FORM view modes.

### Critical Issues

#### 2.1 Type Redefinition Anti-pattern (Lines 34-81)
```tsx
// These types DUPLICATE existing types from '../types'
interface Composer extends BaseEntity { type: 'Composers'; ... }
interface Raga extends BaseEntity { type: 'Ragas'; ... }
interface Tala extends BaseEntity { type: 'Talas'; ... }
```
**Impact:** Type mismatches, maintenance burden, potential runtime bugs.

#### 2.2 Pervasive `any` Types (Lines 142, 169, 186, 209)
```tsx
const FormInput = ({ label, ... }: any) => (...)
const FormTextarea = ({ ... }: any) => (...)
const FormSelect = ({ ... }: any) => (...)
const [formData, setFormData] = useState<Record<string, any>>({});
```
**Impact:** No type safety on form data.

#### 2.3 Repetitive Validation Logic (Lines 335-359)
```tsx
if (entityType === 'Composers' && !formData.name) { toast.error(...); return; }
if (entityType === 'Ragas' && !formData.name) { toast.error(...); return; }
if (entityType === 'Talas' && !formData.name) { toast.error(...); return; }
// Same pattern repeated 5 times
```
**Impact:** Code duplication, maintenance burden.

#### 2.4 Massive Switch Statements (Lines 361-457, 700-731, 756-777)
Entity-specific logic spread across multiple switch statements. Should use polymorphism or strategy pattern.

#### 2.5 Inline Mapper Functions (Lines 85-137)
Data transformation logic mixed with component code. Should be extracted to utilities.

### Moderate Issues

- No loading states for deity dropdown in Temple form
- Form state not reset properly on entity type change
- No optimistic updates
- Missing error boundaries

---

## 3. BulkImport.tsx (772 lines)

### Overview
Complex orchestration page for bulk CSV imports with real-time polling, task management, and detailed views.

### Critical Issues

#### 3.1 Large triggerAction Function (Lines 181-233)
```tsx
const triggerAction = async (action: 'pause' | 'resume' | 'cancel' | 'retry' | 'delete' | ..., batchId: string) => {
    // 50+ lines handling 9 different action types
}
```
**Impact:** High cyclomatic complexity, hard to test.

#### 3.2 Polling Without Cleanup Guard (Lines 93-104)
```tsx
useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isRunning && selectedBatchId) {
        interval = setInterval(() => {
            void loadBatchDetail(selectedBatchId);  // No abort controller
        }, 2000);
    }
    return () => clearInterval(interval);
}, [selectedBatch?.status, selectedBatchId]);
```
**Impact:** Potential memory leaks, stale closures.

#### 3.3 Mixed Console Logging (Line 182)
```tsx
console.log(`triggerAction called: ${action} for batch ${batchId}`);
```
**Impact:** Development code in production.

### Moderate Issues

- Inline modal components (Lines 261-387) should be extracted
- No virtualization for potentially large task lists
- Missing loading states during action execution
- File download creates DOM elements directly (Lines 211-217)

---

## 4. ImportReview.tsx (414 lines)

### Overview
Queue-based import review page with bulk selection and entity resolution preview.

### Issues

#### 4.1 Unused Interface (Lines 6-17)
```tsx
interface ResolutionCandidate<T> { ... }
interface ResolutionResult { ... }
```
Defined but `ResolutionResult` is parsed from JSON without validation.

#### 4.2 JSON Parsing Without Validation (Lines 177-182)
```tsx
let resolution: ResolutionResult;
try {
    resolution = JSON.parse(selectedItem.resolutionData);
} catch (e) {
    return <div className="text-xs text-rose-500">Invalid resolution data</div>;
}
```
**Impact:** Runtime errors if data structure changes.

#### 4.3 Parallel Promise.all Without Error Handling (Lines 118-121, 141-144)
```tsx
const promises = Array.from(selectedImportIds).map(id =>
    reviewImport(id, { status: 'APPROVED' })
);
await Promise.all(promises);  // If one fails, all fail
```
**Impact:** One failed review blocks all reviews.

### Moderate Issues

- Six individual useState calls for form fields could be consolidated
- No confirmation before bulk actions beyond browser `confirm()`
- Selected items state uses Set but renders from array

---

## 5. TagsPage.tsx (317 lines)

### Overview
CRUD page for tag management with search and category filtering.

### Issues

#### 5.1 Development Console Logs (Lines 44, 52-57, 64)
```tsx
console.log('handleCreate called', { formData, isCreating });
console.log('Creating tag with payload:', {...});
console.log('Tag created successfully:', created);
```
**Impact:** Debug code in production.

#### 5.2 Inline Form Validation (Lines 46-49, 77-80)
```tsx
if (!formData.slug || !formData.displayNameEn) {
    toast.error('Slug and Display Name are required');
    return;
}
```
**Impact:** Duplicated in create and update handlers.

### Minor Issues

- Form reset logic duplicated in three places
- No debounce on search input
- Missing type for TAG_CATEGORIES array

---

## 6. ImportsPage.tsx (244 lines)

### Overview
Two-tab page for scraping new content and viewing import history.

### Issues

#### 6.1 Toast Wrapper Object (Lines 8-9)
```tsx
const toast = { success, error }; // Helper wrapper
```
Unnecessary wrapper, should use destructured values directly.

#### 6.2 Tab-based Data Loading (Lines 21-25)
```tsx
useEffect(() => {
    if (activeTab === 'LIST') {
        loadImports();
    }
}, [activeTab]);
```
Data not cached, reloads on every tab switch.

### Minor Issues

- Error thrown after toast display (Line 70)
- No loading state for scrape result

---

## 7. Dashboard.tsx (225 lines)

### Overview
Landing page with stats cards and recent activity feed.

### Issues

#### 7.1 Inline Sub-components (Lines 6-50)
```tsx
const StatCard: React.FC<{...}> = ({...}) => (...)
const RecentItem: React.FC<{...}> = ({...}) => (...)
```
Defined inside module but not memoized.

#### 7.2 Hardcoded Curator Tasks (Lines 189-204)
```tsx
<div className="p-3 bg-amber-50 ...">
    <h5>Missing Metadata</h5>
    <p>15 records are missing 'Tala' information.</p>
</div>
```
**Impact:** Static content, not reflecting actual data.

### Minor Issues

- Error state could be more informative
- Activity status mapping is simplistic (action → status pill)

---

## 8. AutoApproveQueue.tsx (215 lines)

### Overview
Filtered queue for high-confidence batch auto-approval.

### Issues

#### 8.1 Any Type in API Params (Lines 37-41)
```tsx
const params: any = {};
if (selectedBatchId) params.batchId = selectedBatchId;
```
Should use typed interface.

#### 8.2 Browser confirm() Usage (Line 56)
```tsx
if (!confirm(`Auto-approve all ${imports.length} imports in this queue?`)) return;
```
**Impact:** Inconsistent UX, not accessible.

#### 8.3 Double useEffect for Queue Loading (Lines 16-23)
```tsx
useEffect(() => {
    loadBatches();
    loadQueue();  // Called here
}, []);

useEffect(() => {
    loadQueue();  // And here
}, [selectedBatchId, selectedQualityTier, confidenceMin]);
```
Initial load duplicated.

---

## 9. KrithiList.tsx (135 lines)

### Overview
Simple list page with search and navigation to editor.

### Issues

#### 9.1 Unused STATUS_Styles Constant (Lines 6-11)
```tsx
const STATUS_Styles: Record<string, string> = {
  'PUBLISHED': '...',
  // Not used anywhere in the component
};
```

#### 9.2 Non-functional Pagination (Lines 123-128)
```tsx
<button className="..." disabled>Previous</button>
<button className="...">Next</button>  // No onClick handler
```

### Positive Aspects

- Clean, focused component
- Good debounce pattern for search
- Proper TypeScript usage

---

## 10-11. RolesPage.tsx & UsersPage.tsx (Placeholders)

Both are placeholder pages with "Coming Soon" messages. No issues to address until implementation.

---

## Cross-Cutting Issues

### 1. Inconsistent Error Handling

| Pattern | Files |
|---------|-------|
| `console.error` only | Dashboard, KrithiList, ReferenceData |
| `toast.error` | TagsPage, ImportsPage, ImportReview |
| `alert()` | KrithiEditor |
| Browser `confirm()` | AutoApproveQueue, ImportReview, BulkImport |

### 2. Form State Management

No consistent pattern across pages:
- Individual useState per field (ImportReview)
- Single formData object (TagsPage, ReferenceData)
- Mixed approach (KrithiEditor)

### 3. Loading States

- Some pages use simple boolean flags
- No skeleton loaders
- Inconsistent spinner components

### 4. Type Safety Gaps

| Issue | Occurrences |
|-------|-------------|
| `any` type props | 15+ |
| `as any` assertions | 10+ |
| Untyped API responses | 8+ |
| `Record<string, any>` | 5+ |

### 5. Missing Accessibility

- No ARIA attributes on interactive elements
- No focus management
- No keyboard navigation support
- Browser `confirm()` instead of accessible modals

### 6. Performance Concerns

- No `React.memo` on any components
- No `useCallback` for handlers
- No `useMemo` for expensive computations (except BulkImport)
- No virtualization for lists

---

## Recommendations by Priority

### Priority 1 (Critical)
1. Refactor KrithiEditor.tsx (see separate checklist)
2. Refactor ReferenceData.tsx - extract EntityForm, remove type duplication
3. Remove all `any` types across codebase
4. Standardize error handling pattern

### Priority 2 (High)
5. Extract reusable form components with proper types
6. Implement proper modal component (replace browser confirm)
7. Add request cancellation to all data fetching
8. Consolidate form state management approach

### Priority 3 (Medium)
9. Extract inline sub-components to separate files
10. Add accessibility attributes
11. Remove console.log statements
12. Implement proper pagination in KrithiList

### Priority 4 (Low)
13. Add skeleton loaders for better UX
14. Implement list virtualization for large datasets
15. Add Storybook documentation
16. Implement optimistic updates

---

## Metrics Summary

| Metric | Current State |
|--------|---------------|
| Total lines across pages | ~5,200 |
| Files needing refactor | 4 of 11 |
| `any` type usages | 30+ |
| Console.log in code | 10+ |
| Missing TypeScript interfaces | 20+ |
| Components without memo | All |
| Accessible modals | 0 |

---

## Conclusion

The frontend codebase is functional but exhibits inconsistent patterns, type safety gaps, and architectural issues primarily in the two largest files (KrithiEditor and ReferenceData). Addressing these would significantly improve maintainability, type safety, and developer experience. The smaller pages are reasonably well-structured and require only minor improvements.
