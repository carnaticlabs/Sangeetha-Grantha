| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangita Grantha Architect |

# TRACK-019: Datetime Standardization (kotlin.time.Instant)

## Goal
Standardize all timestamp usage across the codebase to `kotlin.time.Instant` (Stdlib) and remove dependencies on legacy `kotlinx.datetime.Instant`.

## Context
As per [Decision: Datetime Standardization](../../application_documentation/02-architecture/decisions/datetime-standardization.md), Kotlin 2.3.0 and `kotlinx-datetime` 0.7.0+ have moved `Instant` to the standard library. To ensure compatibility, future-proofing, and to upgrade `kotlinx-datetime` to 0.7.1, we must migrate.

## Scope
- **Shared Domain**: Update DTOs in `ImportDtos.kt`, `Models.kt`.
- **Backend DAL**: Update `ResultRowExtensions.kt` and `DtoMappers.kt`.
- **Backend API**: Verify Request/Response serialization.
- **Mobile**: Update Android/iOS usages if any.
- **Dependencies**: Upgrade `kotlinx-datetime` to 0.7.1 after code changes.

## Implementation Plan
- [x] **Step 1: Domain Layer**: Replace imports in `modules/shared/domain`.
- [x] **Step 2: DAL Layer**: Update extension functions and mappers.
- [x] **Step 3: Verification**: Run backend tests.
- [x] **Step 4: Upgrade Dependency**: Update `libs.versions.toml` to `kotlinxDatetime = "0.7.1"`.
- [x] **Step 5: Full Verification**: Run all tests and builds.

## Progress Log

- **2026-01-26**: Created track and identified scope.
- **2026-01-26**: Executed standardization.
  - Refactored `shared:domain` DTOs to use `kotlin.time.Instant`.
  - Refactored `backend:dal` extension functions to return `kotlin.time.Instant`.
  - Fixed `AutoApprovalServiceTest` to use `kotlin.time.Instant` and strict JSON construction.
  - Upgraded `kotlinx-datetime` to `0.7.1`.
- **2026-01-26**: Verification.
  - `backend:api` compiles and runs.
  - `backend:api` tests passed (except one unrelated logic failure).
  - `backend:dal` compiled but tests failed (needs investigation in separate track).
  - `clean build` compilation passed for all modules.

