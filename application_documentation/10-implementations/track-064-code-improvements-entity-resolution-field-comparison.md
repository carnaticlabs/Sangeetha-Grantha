| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-064: Entity Resolution Cache & Field Comparison Improvements

## Purpose

Two targeted improvements to the unified extraction engine (TRACK-064):

1. **EntityResolutionCacheRepository**: Replaced racy select-then-update pattern with insert-first-catch-unique-violation (PostgreSQL `23505`), using `updateReturning` for the conflict path. Eliminates race conditions under concurrent extraction workers.

2. **SourceEvidenceTab**: Replaced client-side field comparison computation with server-driven `useFieldComparison` hook. Adds proper loading/error states, simplifying the component and delegating comparison logic to the API.

## Code Changes Summary

| File | Change |
|:---|:---|
| `EntityResolutionCacheRepository.kt` | Insert-first with `ExposedSQLException` catch for unique constraint; `updateReturning` on conflict; new `isTypeAndNameUniqueViolation()` helper |
| `SourceEvidenceTab.tsx` | Replaced inline field comparison arrays with `useFieldComparison` hook; added loading/error/empty states |

## Commit Reference

```
Ref: application_documentation/10-implementations/track-064-code-improvements-entity-resolution-field-comparison.md
```
