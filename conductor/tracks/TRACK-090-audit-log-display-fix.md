| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# Goal
Fix the audit log display across the Dashboard "Recent Edits" and Krithi Editor "Change History" panels, which showed "Unknown Entity", "Unknown User", "Unknown Date", and "Invalid Date" due to a field name mismatch between the backend DTO and frontend TypeScript interface.

# Root Cause
The backend `AuditLogDto` serialized fields as `entityTable`, `actorUserId` (UUID), and `changedAt`, but the frontend `AuditLog` interface expected `entityType`, `actor` (string name), and `timestamp`. Additionally, actor names were never resolved from the `users` table, and the audit route ignored `entityTable`/`entityId` query parameters.

# Implementation Summary

## Backend Changes
1. **`AuditDtos.kt`** — Added `actorName: String?` field to the shared DTO.
2. **`AuditLogRepository.kt`** — Left-joined `users` table to resolve actor display names (displayName → fullName → email fallback). Added `listByEntity()` method for filtered queries.
3. **`AuditLogService.kt`** — Added `listByEntity()` delegation method.
4. **`AuditRoutes.kt`** — Now reads `entityTable` and `entityId` query parameters and routes to filtered or unfiltered query accordingly.
5. **`ImportDtoMappers.kt`** — Removed orphaned `toAuditLogDto()` mapper (replaced by enriched version in repository).

## Frontend Changes
6. **`types.ts`** — Aligned `AuditLog` interface to match backend serialization: `entityTable`, `actorName`, `changedAt` (previously `entityType`, `actor`, `timestamp`).
7. **`Dashboard.tsx`** — Added `formatEntityTable()` for human-readable table labels ("Lyric Variant" instead of "krithi_lyric_variants"), `formatAction()` for title-cased actions, `actorName` with "System" fallback.
8. **`AuditTab.tsx`** — Same field name corrections for the Change History timeline, plus `actionIcon()` helper for contextual icons.

# Files Changed
- `modules/shared/domain/src/commonMain/kotlin/.../model/AuditDtos.kt`
- `modules/backend/dal/src/main/kotlin/.../repositories/AuditLogRepository.kt`
- `modules/backend/dal/src/main/kotlin/.../models/ImportDtoMappers.kt`
- `modules/backend/api/src/main/kotlin/.../services/AuditLogService.kt`
- `modules/backend/api/src/main/kotlin/.../routes/AuditRoutes.kt`
- `modules/frontend/sangita-admin-web/src/types.ts`
- `modules/frontend/sangita-admin-web/src/pages/Dashboard.tsx`
- `modules/frontend/sangita-admin-web/src/components/krithi-editor/AuditTab.tsx`
