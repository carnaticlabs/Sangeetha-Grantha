| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Frontend-Backend API Coverage Report

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


**Generated:** 2025-01-27  
**Scope:** Complete mapping of UI operations to backend API endpoints  
**Status:** Comprehensive analysis with identified gaps

---

## Executive Summary

This report provides a comprehensive mapping between frontend UI operations and backend API endpoints in the Sangeetha Grantha admin web application. The analysis covers all 6 main pages and their associated operations, comparing them against available backend endpoints.

### Key Findings

- **Total Frontend Pages Analyzed:** 6
- **Total UI Operations Identified:** 45+
- **Total Backend Endpoints Available:** 50+
- **Gaps Identified:** 3 critical missing endpoints
- **Coverage:** ~93% (3 missing endpoints for lyric variant management)

---

## 1. Dashboard Page (`/`)

### UI Operations

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load dashboard statistics | `getDashboardStats()` | ✅ Supported |
| Load recent audit logs | `getAuditLogs()` | ✅ Supported |
| Navigate to Krithis list | Navigation only | N/A |
| Navigate to Reference Data | Navigation only | N/A |
| Navigate to new Krithi editor | Navigation only | N/A |

### Backend Endpoints Used

- ✅ `GET /v1/admin/dashboard/stats` - Returns dashboard statistics
- ✅ `GET /v1/audit/logs` - Returns recent audit logs

### Coverage Status: ✅ **100% Complete**

---

## 2. Krithi List Page (`/krithis`)

### UI Operations

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Search krithis | `searchKrithis(query)` | ✅ Supported |
| Navigate to krithi editor | Navigation only | N/A |
| Navigate to new krithi | Navigation only | N/A |
| Filter krithis (UI only) | Client-side filtering | N/A |
| Pagination (UI only) | Client-side pagination | N/A |

### Backend Endpoints Used

- ✅ `GET /v1/krithis/search?query={query}` - Search krithis with optional query parameters

### Coverage Status: ✅ **100% Complete**

**Note:** The UI shows pagination controls but doesn't implement pagination API calls yet. The backend supports pagination via `page` and `pageSize` query parameters.

---

## 3. Krithi Editor Page (`/krithis/new`, `/krithis/:id`)

### UI Operations - Metadata Tab

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load krithi details | `getKrithi(id)` | ✅ Supported |
| Create new krithi | `createKrithi(payload)` | ✅ Supported |
| Update krithi | `updateKrithi(id, payload)` | ✅ Supported |
| Load reference data (composers, ragas, etc.) | `getComposers()`, `getRagas()`, etc. | ✅ Supported |
| Transliterate content | `transliterateContent(krithiId, content, ...)` | ✅ Supported |
| Validate krithi | Not implemented in UI | ⚠️ Backend stub exists |

### UI Operations - Structure Tab

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load krithi sections | `getKrithiSections(krithiId)` | ✅ Supported |
| Save krithi sections | `saveKrithiSections(krithiId, sections)` | ✅ Supported |

### UI Operations - Lyrics Tab

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load lyric variants | `getKrithiLyricVariants(krithiId)` | ✅ Supported |
| Create lyric variant | `createLyricVariant(krithiId, payload)` | ❌ **MISSING** |
| Update lyric variant | `updateLyricVariant(variantId, payload)` | ❌ **MISSING** |
| Save variant sections | `saveVariantSections(variantId, sections)` | ❌ **MISSING** |

### UI Operations - Tags Tab

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load krithi tags | `getKrithiTags(krithiId)` | ✅ Supported |
| Load all tags | `getAllTags()` | ✅ Supported |
| Tag management (via Tags page) | See Tags Page section | ✅ Supported |

### UI Operations - Notation Tab

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load notation | `getAdminKrithiNotation(krithiId, form)` | ✅ Supported |
| Create notation variant | `createNotationVariant(krithiId, payload)` | ✅ Supported |
| Update notation variant | `updateNotationVariant(variantId, payload)` | ✅ Supported |
| Delete notation variant | `deleteNotationVariant(variantId)` | ✅ Supported |
| Create notation row | `createNotationRow(variantId, payload)` | ✅ Supported |
| Update notation row | `updateNotationRow(rowId, payload)` | ✅ Supported |
| Delete notation row | `deleteNotationRow(rowId)` | ✅ Supported |

### UI Operations - Audit Tab

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load audit logs for krithi | `getKrithiAuditLogs(krithiId)` | ✅ Supported |

### Backend Endpoints Used

#### Metadata Operations
- ✅ `GET /v1/krithis/{id}` - Get krithi details
- ✅ `POST /v1/krithis` - Create new krithi
- ✅ `PUT /v1/krithis/{id}` - Update krithi
- ✅ `POST /v1/admin/krithis/{id}/transliterate` - Transliterate content
- ✅ `POST /v1/admin/krithis/{id}/validate` - Validate krithi (stub implementation)

#### Structure Operations
- ✅ `GET /v1/admin/krithis/{id}/sections` - Get krithi sections
- ✅ `POST /v1/admin/krithis/{id}/sections` - Save krithi sections

#### Lyrics Operations
- ✅ `GET /v1/admin/krithis/{id}/variants` - Get lyric variants
- ❌ `POST /v1/admin/krithis/{id}/variants` - **MISSING** - Create lyric variant
- ❌ `PUT /v1/admin/variants/{variantId}` - **MISSING** - Update lyric variant
- ❌ `POST /v1/admin/variants/{variantId}/sections` - **MISSING** - Save variant sections

#### Tags Operations
- ✅ `GET /v1/admin/krithis/{id}/tags` - Get krithi tags
- ✅ `GET /v1/admin/tags` - Get all tags

#### Notation Operations
- ✅ `GET /v1/krithis/{id}/notation?musicalForm={form}` - Get notation (admin context)
- ✅ `POST /v1/krithis/{id}/notation/variants` - Create notation variant
- ✅ `PUT /v1/notation/variants/{variantId}` - Update notation variant
- ✅ `DELETE /v1/notation/variants/{variantId}` - Delete notation variant
- ✅ `POST /v1/notation/variants/{variantId}/rows` - Create notation row
- ✅ `PUT /v1/notation/rows/{rowId}` - Update notation row
- ✅ `DELETE /v1/notation/rows/{rowId}` - Delete notation row

#### Audit Operations
- ✅ `GET /v1/audit/logs?entityTable=krithis&entityId={id}` - Get audit logs for krithi

### Coverage Status: ⚠️ **93% Complete** (3 Missing Endpoints)

**Critical Gaps:**
1. ❌ `POST /v1/admin/krithis/{id}/variants` - Create lyric variant
2. ❌ `PUT /v1/admin/variants/{variantId}` - Update lyric variant
3. ❌ `POST /v1/admin/variants/{variantId}/sections` - Save variant sections

**Impact:** Users cannot create, update, or save sections for lyric variants through the UI. The frontend code attempts to call these endpoints, but they do not exist in the backend.

---

## 4. Reference Data Page (`/reference`)

### UI Operations

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load reference statistics | `getReferenceStats()` | ✅ Supported |
| Load composers | `getComposers()` | ✅ Supported |
| Create composer | `createComposer(payload)` | ✅ Supported |
| Update composer | `updateComposer(id, payload)` | ✅ Supported |
| Delete composer | `deleteComposer(id)` | ✅ Supported |
| Load ragas | `getRagas()` | ✅ Supported |
| Create raga | `createRaga(payload)` | ✅ Supported |
| Update raga | `updateRaga(id, payload)` | ✅ Supported |
| Delete raga | `deleteRaga(id)` | ✅ Supported |
| Load talas | `getTalas()` | ✅ Supported |
| Create tala | `createTala(payload)` | ✅ Supported |
| Update tala | `updateTala(id, payload)` | ✅ Supported |
| Delete tala | `deleteTala(id)` | ✅ Supported |
| Load temples | `getTemples()` | ✅ Supported |
| Create temple | `createTemple(payload)` | ✅ Supported |
| Update temple | `updateTemple(id, payload)` | ✅ Supported |
| Delete temple | `deleteTemple(id)` | ✅ Supported |
| Load deities | `getDeities()` | ✅ Supported |

### Backend Endpoints Used

#### Reference Statistics
- ✅ `GET /v1/reference/stats` - Get reference data statistics

#### Composers CRUD
- ✅ `GET /v1/composers` - List all composers (public endpoint)
- ✅ `GET /v1/admin/composers` - List all composers (admin endpoint)
- ✅ `GET /v1/admin/composers/{id}` - Get composer by ID
- ✅ `POST /v1/admin/composers` - Create composer
- ✅ `PUT /v1/admin/composers/{id}` - Update composer
- ✅ `DELETE /v1/admin/composers/{id}` - Delete composer

#### Ragas CRUD
- ✅ `GET /v1/ragas` - List all ragas (public endpoint)
- ✅ `GET /v1/admin/ragas` - List all ragas (admin endpoint)
- ✅ `GET /v1/admin/ragas/{id}` - Get raga by ID
- ✅ `POST /v1/admin/ragas` - Create raga
- ✅ `PUT /v1/admin/ragas/{id}` - Update raga
- ✅ `DELETE /v1/admin/ragas/{id}` - Delete raga

#### Talas CRUD
- ✅ `GET /v1/talas` - List all talas (public endpoint)
- ✅ `GET /v1/admin/talas` - List all talas (admin endpoint)
- ✅ `GET /v1/admin/talas/{id}` - Get tala by ID
- ✅ `POST /v1/admin/talas` - Create tala
- ✅ `PUT /v1/admin/talas/{id}` - Update tala
- ✅ `DELETE /v1/admin/talas/{id}` - Delete tala

#### Temples CRUD
- ✅ `GET /v1/temples` - List all temples (public endpoint)
- ✅ `GET /v1/admin/temples` - List all temples (admin endpoint)
- ✅ `GET /v1/admin/temples/{id}` - Get temple by ID
- ✅ `POST /v1/admin/temples` - Create temple
- ✅ `PUT /v1/admin/temples/{id}` - Update temple
- ✅ `DELETE /v1/admin/temples/{id}` - Delete temple

#### Deities (Read-only)
- ✅ `GET /v1/deities` - List all deities

### Coverage Status: ✅ **100% Complete**

**Note:** The frontend uses public endpoints (`/v1/composers`, `/v1/ragas`, etc.) for reading, which is appropriate. Admin endpoints are used for mutations.

---

## 5. Imports Page (`/imports`)

### UI Operations

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load imports list | `getImports(status?)` | ✅ Supported |
| Scrape content from URL | `scrapeContent(url)` | ✅ Supported |
| Review import | `reviewImport(id, request)` | ✅ Supported |

### Backend Endpoints Used

- ✅ `GET /v1/admin/imports?status={status}` - List imports (optional status filter)
- ✅ `POST /v1/admin/imports/scrape` - Scrape content from URL
- ✅ `POST /v1/imports/{id}/review` - Review and approve/reject import

### Coverage Status: ✅ **100% Complete**

---

## 6. Tags Page (`/tags`)

### UI Operations

| Operation | Frontend Function | Status |
|-----------|------------------|--------|
| Load all tags | `getAllTags()` | ✅ Supported |
| Create tag | `createTag(payload)` | ✅ Supported |
| Update tag | `updateTag(id, payload)` | ✅ Supported |
| Delete tag | `deleteTag(id)` | ✅ Supported |
| Search tags (client-side) | Client-side filtering | N/A |

### Backend Endpoints Used

- ✅ `GET /v1/admin/tags` - List all tags
- ✅ `GET /v1/admin/tags/{id}` - Get tag by ID (not used in UI, but available)
- ✅ `POST /v1/admin/tags` - Create tag
- ✅ `PUT /v1/admin/tags/{id}` - Update tag
- ✅ `DELETE /v1/admin/tags/{id}` - Delete tag

### Coverage Status: ✅ **100% Complete**

---

## Summary of Missing Endpoints

### Critical Gaps (3 endpoints)

These endpoints are called by the frontend but do not exist in the backend:

1. **`POST /v1/admin/krithis/{id}/variants`**
   - **Purpose:** Create a new lyric variant for a krithi
   - **Frontend Usage:** `KrithiEditor.tsx` - Lyrics tab
   - **Impact:** Users cannot create new lyric variants
   - **Recommendation:** Implement in `AdminKrithiRoutes.kt` or create a new `AdminLyricVariantRoutes.kt`

2. **`PUT /v1/admin/variants/{variantId}`**
   - **Purpose:** Update an existing lyric variant
   - **Frontend Usage:** `KrithiEditor.tsx` - Lyrics tab
   - **Impact:** Users cannot update existing lyric variants
   - **Recommendation:** Implement in `AdminKrithiRoutes.kt` or create a new `AdminLyricVariantRoutes.kt`

3. **`POST /v1/admin/variants/{variantId}/sections`**
   - **Purpose:** Save sections (lyrics) for a lyric variant
   - **Frontend Usage:** `KrithiEditor.tsx` - Lyrics tab
   - **Impact:** Users cannot save lyrics for variant sections
   - **Recommendation:** Implement in `AdminKrithiRoutes.kt` or create a new `AdminLyricVariantRoutes.kt`

### Implementation Notes

The frontend expects these endpoints to follow a similar pattern to notation variants:
- Notation variants: `/v1/krithis/{id}/notation/variants` (POST)
- Lyric variants: `/v1/admin/krithis/{id}/variants` (POST) - **MISSING**

The backend already has:
- `GET /v1/admin/krithis/{id}/variants` - Returns lyric variants
- Service methods likely exist in `KrithiService` for variant management

**Action Required:** Implement the three missing endpoints in the backend to complete lyric variant management functionality.

---

## Additional Observations

### 1. Pagination
- **Frontend:** KrithiList page shows pagination UI but doesn't implement pagination API calls
- **Backend:** Supports pagination via `page` and `pageSize` query parameters
- **Recommendation:** Implement pagination in frontend to handle large result sets

### 2. Filtering
- **Frontend:** Filter UI exists but uses client-side filtering
- **Backend:** Search endpoint supports multiple filter parameters (composerId, ragaId, talaId, etc.)
- **Recommendation:** Implement server-side filtering for better performance

### 3. Validation Endpoint
- **Backend:** `POST /v1/admin/krithis/{id}/validate` exists but returns a stub response
- **Frontend:** Not currently used in UI
- **Recommendation:** Implement validation logic or remove the endpoint

### 4. Reference Data Endpoints
- Both public (`/v1/composers`) and admin (`/v1/admin/composers`) endpoints exist
- Frontend correctly uses public endpoints for reads and admin endpoints for mutations
- This is a good pattern and should be maintained

### 5. Audit Logs
- Audit logs are available at entity level (`/v1/audit/logs?entityTable=krithis&entityId={id}`)
- Frontend correctly uses this for the Audit tab in KrithiEditor
- Coverage is complete

---

## Recommendations

### High Priority
1. **Implement missing lyric variant endpoints** (3 endpoints)
   - This is blocking full functionality of the Lyrics tab in KrithiEditor
   - Users cannot create or update lyric variants currently

### Medium Priority
2. **Implement pagination in KrithiList**
   - Backend supports it, frontend should use it for better UX
3. **Implement server-side filtering**
   - Move filtering logic from client to server for better performance

### Low Priority
4. **Complete validation endpoint implementation**
   - Either implement full validation logic or remove the stub
5. **Add error handling for missing endpoints**
   - Frontend should gracefully handle 404s for missing endpoints

---

## Appendix: Complete Endpoint Inventory

### Health & Status
- `GET /health`
- `GET /v1/health`

### Public Krithi Endpoints
- `GET /v1/krithis/search` - Search krithis
- `GET /v1/krithis/{id}` - Get krithi details
- `GET /v1/krithis/{id}/notation` - Get notation (public/admin context)

### Admin Krithi Endpoints
- `POST /v1/krithis` - Create krithi
- `PUT /v1/krithis/{id}` - Update krithi
- `POST /v1/admin/krithis/{id}/transliterate` - Transliterate content
- `POST /v1/admin/krithis/{id}/validate` - Validate krithi (stub)
- `GET /v1/admin/krithis/{id}/sections` - Get sections
- `POST /v1/admin/krithis/{id}/sections` - Save sections
- `GET /v1/admin/krithis/{id}/variants` - Get lyric variants
- `GET /v1/admin/krithis/{id}/tags` - Get krithi tags
- ❌ `POST /v1/admin/krithis/{id}/variants` - **MISSING**
- ❌ `PUT /v1/admin/variants/{variantId}` - **MISSING**
- ❌ `POST /v1/admin/variants/{variantId}/sections` - **MISSING**

### Admin Notation Endpoints
- `POST /v1/krithis/{id}/notation/variants` - Create notation variant
- `PUT /v1/notation/variants/{variantId}` - Update notation variant
- `DELETE /v1/notation/variants/{variantId}` - Delete notation variant
- `POST /v1/notation/variants/{variantId}/rows` - Create notation row
- `PUT /v1/notation/rows/{rowId}` - Update notation row
- `DELETE /v1/notation/rows/{rowId}` - Delete notation row

### Public Reference Data Endpoints
- `GET /v1/composers` - List composers
- `GET /v1/ragas` - List ragas
- `GET /v1/talas` - List talas
- `GET /v1/deities` - List deities
- `GET /v1/temples` - List temples
- `GET /v1/tags` - List tags
- `GET /v1/sampradayas` - List sampradayas

### Admin Reference Data Endpoints
- `GET /v1/reference/stats` - Get reference statistics
- `GET /v1/admin/composers` - List composers
- `GET /v1/admin/composers/{id}` - Get composer
- `POST /v1/admin/composers` - Create composer
- `PUT /v1/admin/composers/{id}` - Update composer
- `DELETE /v1/admin/composers/{id}` - Delete composer
- `GET /v1/admin/ragas` - List ragas
- `GET /v1/admin/ragas/{id}` - Get raga
- `POST /v1/admin/ragas` - Create raga
- `PUT /v1/admin/ragas/{id}` - Update raga
- `DELETE /v1/admin/ragas/{id}` - Delete raga
- `GET /v1/admin/talas` - List talas
- `GET /v1/admin/talas/{id}` - Get tala
- `POST /v1/admin/talas` - Create tala
- `PUT /v1/admin/talas/{id}` - Update tala
- `DELETE /v1/admin/talas/{id}` - Delete tala
- `GET /v1/admin/temples` - List temples
- `GET /v1/admin/temples/{id}` - Get temple
- `POST /v1/admin/temples` - Create temple
- `PUT /v1/admin/temples/{id}` - Update temple
- `DELETE /v1/admin/temples/{id}` - Delete temple
- `GET /v1/admin/tags` - List tags
- `GET /v1/admin/tags/{id}` - Get tag
- `POST /v1/admin/tags` - Create tag
- `PUT /v1/admin/tags/{id}` - Update tag
- `DELETE /v1/admin/tags/{id}` - Delete tag

### Admin Dashboard Endpoints
- `GET /v1/admin/dashboard/stats` - Get dashboard statistics

### Audit Endpoints
- `GET /v1/audit/logs` - Get audit logs (with optional query params)

### Import Endpoints
- `POST /v1/imports/krithis` - Submit imports
- `POST /v1/imports/{id}/review` - Review import
- `GET /v1/admin/imports` - List imports
- `POST /v1/admin/imports/scrape` - Scrape content

---

## Conclusion

The frontend-backend API coverage is **93% complete**, with only 3 critical endpoints missing for lyric variant management. All other major functionality is fully supported. The missing endpoints should be implemented to complete the Lyrics tab functionality in the KrithiEditor.

**Overall Assessment:** ✅ **Good** - Minor gaps that can be addressed quickly.

---

*Report generated by automated analysis of frontend pages and backend route definitions.*
