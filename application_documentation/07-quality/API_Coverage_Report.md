| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# API Coverage Report


---


**Date:** 2026-01-02
**Scope:** `modules/frontend/sangita-admin-web` vs `modules/backend/api`

## Executive Summary

A comprehensive scan of the Frontend (Sangita Admin Web) and Backend (API) reveals that while core entity management (Krithis, Composers, Ragas, etc.) is well-supported, there are **critical gaps in the Lyric Variant management workflow** and **API path mismatches in the Notation management workflow**.

- **Total Frontend Endpoints Analyzed:** ~35
- **Critical Implementation Gaps:** 1 Major Feature Area (Lyric Variants)
- **Path Mismatches:** 1 Feature Area (Notation)
- **Backend Endpoints Unused by Frontend:** ~5

---

## 1. Critical Implementation Gaps (Missing Backend Routes)

The following operations are defined in the Frontend client and used in the UI, but **NO corresponding routes were found in the Backend codebase**, leading to 404 errors during execution.

| Feature | Frontend Method | UI Path | Implementation Status |
| :--- | :--- | :--- | :--- |
| **Lyric Variants** | `createLyricVariant` | `/admin/krithis/{id}/variants` (POST) | **MISSING**. No POST endpoint for variants. |
| **Lyric Variants** | `updateLyricVariant` | `/admin/variants/{id}` (PUT) | **MISSING**. No PUT endpoint for variants. |
| **Lyric Variants** | `saveVariantSections` | `/admin/variants/{id}/sections` (POST) | **MISSING**. No endpoint to save variant sections. |

**Impact:** Users cannot add or edit details of lyric variants (language, script, lyrics) or manage their sectional breakdowns. This is a blocking issue for the Content Management workflow.

---

## 2. API Path Mismatches

The Frontend uses a different URL structure than what the Backend defines for the Notation Management feature. These will likely result in 404 errors unless a global rewrite rule exists (none observed).

| Feature | Frontend Request Path | Backend Defined Path | Status |
| :--- | :--- | :--- | :--- |
| **Create Notation** | `/admin/krithis/{id}/notation/variants` | `/v1/krithis/{id}/notation/variants` | **Mismatch** (Frontend adds `/admin`) |
| **Update Notation** | `/admin/notation/variants/{variantId}` | `/v1/notation/variants/{variantId}` | **Mismatch** (Frontend adds `/admin`) |
| **Delete Notation** | `/admin/notation/variants/{variantId}` | `/v1/notation/variants/{variantId}` | **Mismatch** (Frontend adds `/admin`) |
| **Create Row** | `/admin/notation/variants/{variantId}/rows` | `/v1/notation/variants/{variantId}/rows` | **Mismatch** (Frontend adds `/admin`) |
| **Update Row** | `/admin/notation/rows/{rowId}` | `/v1/notation/rows/{rowId}` | **Mismatch** (Frontend adds `/admin`) |
| **Delete Row** | `/admin/notation/rows/{rowId}` | `/v1/notation/rows/{rowId}` | **Mismatch** (Frontend adds `/admin`) |

**Impact:** Notation editing functionality will fail.

---

## 3. Coverage Matrix

### 3.1 Core Krithi Management
| Operation | Frontend Path | Backend Path | Status |
| :--- | :--- | :--- | :--- |
| Search Krithis | `/krithis/search` | `/v1/krithis/search` | ✅ Matched |
| Get Krithi | `/krithis/{id}` | `/v1/krithis/{id}` | ✅ Matched |
| Create Krithi | `/krithis` | `/v1/krithis` | ✅ Matched |
| Update Krithi | `/krithis/{id}` | `/v1/krithis/{id}` | ✅ Matched |
| Get Sections | `/admin/krithis/{id}/sections` | `/v1/admin/krithis/{id}/sections` | ✅ Matched |
| Save Sections | `/admin/krithis/{id}/sections` | `/v1/admin/krithis/{id}/sections` | ✅ Matched |
| Get Variants | `/admin/krithis/{id}/variants` | `/v1/admin/krithis/{id}/variants` | ✅ Matched |
| Get Tags | `/admin/krithis/{id}/tags` | `/v1/admin/krithis/{id}/tags` | ✅ Matched |
| Transliterate | `/admin/krithis/{id}/transliterate` | `/v1/admin/krithis/{id}/transliterate` | ✅ Matched |

### 3.2 Reference Data (Admin)
| Operation | Frontend Path | Backend Path | Status |
| :--- | :--- | :--- | :--- |
| **Composers** | `/admin/composers` (CRUD) | `/v1/admin/composers` | ✅ Matched |
| **Ragas** | `/admin/ragas` (CRUD) | `/v1/admin/ragas` | ✅ Matched |
| **Talas** | `/admin/talas` (CRUD) | `/v1/admin/talas` | ✅ Matched |
| **Temples** | `/admin/temples` (CRUD) | `/v1/admin/temples` | ✅ Matched |
| **Tags** | `/admin/tags` (CRUD) | `/v1/admin/tags` | ✅ Matched |

### 3.3 Imports & Dashboard
| Operation | Frontend Path | Backend Path | Status |
| :--- | :--- | :--- | :--- |
| List Imports | `/admin/imports` | `/v1/admin/imports` | ✅ Matched |
| Scrape | `/admin/imports/scrape` | `/v1/admin/imports/scrape` | ✅ Matched |
| Review Import | `/imports/{id}/review` | `/v1/imports/{id}/review` | ✅ Matched |
| Audit Logs | `/audit/logs` | `/v1/audit/logs` | ✅ Matched |
| Dashboard Stats | `/admin/dashboard/stats` | `/v1/admin/dashboard/stats` | ✅ Matched |

---

## 4. Unused Backend Endpoints

The following endpoints exist in the Backend but are not currently utilized by the Frontend:

1.  **Validate Krithi**: `POST /v1/admin/krithis/{id}/validate`
    - Frontend implementation is marked as TODO (Phase 4).
2.  **Health Check**: `/v1/health`
    - Likely used by infrastructure/monitoring, not UI.
3.  **Reference Lists (Public)**: `/v1/composers`, `/v1/ragas` etc.
    - Used by `client.ts` public methods, but Admin UI mostly uses the specific `/admin/` stats or CRUD endpoints. (Verified `client.ts` does define these, so they are technically covered, just maybe rarely used in Admin flow).

## 5. Recommendations

1.  **Implement Lyric Variant CRUD**: create `AdminLyricVariantRoutes.kt` (or update `AdminKrithiRoutes.kt`) to support:
    - `POST /v1/admin/krithis/{id}/variants`
    - `PUT /v1/admin/variants/{id}`
    - `POST /v1/admin/variants/{id}/sections`
2.  **Fix Notation API Paths**: Ensure consistency. Recommended: Update Frontend `client.ts` to remove the extra `/admin` prefix for notation routes to match Backend's `/v1/` structure, OR update Backend `AdminNotationRoutes.kt` to nest under `/admin`.
    - *Correction*: Backend defines notation routes under `/v1/`, but logically they are admin actions. The Frontend matches the "Admin" intent. Updating Backend to `route("/v1/admin")` in `AdminNotationRoutes.kt` is likely the cleaner semantic fix.
