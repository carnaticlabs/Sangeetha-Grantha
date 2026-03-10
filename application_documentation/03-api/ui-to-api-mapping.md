| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# UI to API Mapping


# UI ↔ API Mapping (Canonical)

This document maps Admin Web UI screens and actions to backend API endpoints.

---

## Frontend Implementation

The Admin Web frontend (`modules/frontend/sangita-admin-web`) uses:
- **React 19.2.4** with TypeScript
- **React Query** (TanStack Query) for data fetching and caching
- **React Router** for navigation
- **Fetch** for HTTP requests

API client is located in `src/api/` with typed request/response interfaces.

---

## Queries (Implemented)

### Public Endpoints

| UI Screen/Action | API Endpoint | Method | Description |
|------------------|--------------|--------|-------------|
| Health check | `/health` | GET | Service health status |
| Krithi search | `/v1/krithis/search` | GET | Public search (published only) |
| Krithi detail | `/v1/krithis/{id}` | GET | Public Krithi details |
| Notation view | `/v1/krithis/{id}/notation` | GET | Public notation (published only) |
| Reference data | `/v1/composers`, `/v1/ragas`, etc. | GET | Lists of reference entities |

### Admin Endpoints

| UI Screen/Action | API Endpoint | Method | Description |
|------------------|--------------|--------|-------------|
| Admin Krithi list | `/v1/admin/krithis` | GET | Admin search (all states) |
| Admin Krithi detail | `/v1/admin/krithis/{id}` | GET | Admin Krithi details |
| Admin notation | `/v1/admin/krithis/{id}/notation` | GET | Admin notation (all states) |
| Import list | `/v1/admin/imports/krithis` | GET | Import review queue |
| Dashboard stats | `/v1/admin/dashboard` | GET | Aggregated statistics |
| Audit logs | `/v1/admin/audit` | GET | Audit log queries |

### Query Key Structure

React Query keys follow this pattern:
```typescript
['krithis', 'search', { query, composerId, ... }]
['krithis', id]
['krithis', id, 'notation']
['admin', 'krithis', 'list', { workflowState, ... }]
['admin', 'imports', { status, ... }]
['admin', 'dashboard']
['admin', 'audit', { entityTable, entityId, ... }]
```

### Cache Invalidation

Cache invalidation occurs on mutations:
- Create/update Krithi → invalidate `['krithis']` and `['admin', 'krithis']`
- Create/update notation → invalidate `['krithis', id, 'notation']`
- Import mapping → invalidate `['admin', 'imports']` and `['krithis']`
- Tag assignment → invalidate `['krithis', id]`

---

## Mutations (Implementation Status)

### Implementation Legend

- ✅ **Implemented**: Fully functional in backend and frontend
- 🔄 **In Progress**: Partially implemented
- 📋 **Planned**: Designed but not yet implemented

### Krithi Mutations

| UI Action | API Endpoint | Method | Status | Notes |
|-----------|--------------|--------|--------|-------|
| Create Krithi | `/v1/admin/krithis` | POST | ✅ | Includes `musicalForm` field |
| Update Krithi | `/v1/admin/krithis/{id}` | PUT | ✅ | Full update |
| Add lyric variant | `/v1/admin/krithis/{id}/variants` | POST | ✅ | |
| Update lyric variant | `/v1/admin/variants/{variantId}` | PUT | ✅ | |
| Add section | `/v1/admin/krithis/{id}/sections` | POST | ✅ | |
| Add section text | `/v1/admin/variants/{variantId}/sections` | POST | ✅ | |
| Assign tags | `/v1/admin/krithis/{id}/tags` | POST | ✅ | |
| Remove tag | `/v1/admin/krithis/{id}/tags/{tagId}` | DELETE | ✅ | |
| Publish Krithi | `/v1/admin/krithis/{id}` | PUT | ✅ | Update `workflowState` |

### Notation Mutations (Varnams & Swarajathis)

| UI Action | API Endpoint | Method | Status | Notes |
|-----------|--------------|--------|--------|-------|
| Create notation variant | `/v1/admin/krithis/{id}/notation` | POST | ✅ | Requires `musicalForm` = VARNAM or SWARAJATHI |
| Update notation variant | `/v1/admin/krithis/{id}/notation` | PUT | ✅ | Metadata only |
| Update notation rows | `/v1/admin/notation/{variantId}/rows` | POST | ✅ | Replaces all rows |
| Delete notation variant | `/v1/admin/notation/{variantId}` | DELETE | ✅ | Cascades to rows |

### Import Mutations

| UI Action | API Endpoint | Method | Status | Notes |
|-----------|--------------|--------|--------|-------|
| Map import to Krithi | `/v1/admin/imports/krithis/{id}/map` | POST | ✅ | |
| Reject import | `/v1/admin/imports/krithis/{id}/reject` | POST | ✅ | |

### Mutation Hook Patterns

Example mutation hook pattern:
```typescript
const createKrithiMutation = useMutation({
  mutationFn: (data: KrithiCreateRequest) => 
    api.post('/admin/krithis', data),
  onSuccess: () => {
    queryClient.invalidateQueries(['admin', 'krithis'])
    queryClient.invalidateQueries(['krithis'])
  }
})
```

### Error Handling

- **400 Validation Error**: Display field-level errors from `fields` object
- **401 Unauthorized**: Redirect to login
- **403 Forbidden**: Show "Insufficient permissions" message
- **404 Not Found**: Show "Resource not found" message
- **500 Internal Error**: Show generic error, log details client-side

Error responses follow the structure:
```typescript
{
  code: string
  message: string
  fields?: Record<string, string>
  timestamp: string
}
```

### Authentication Flow

1. **Login**: `POST /v1/admin/login` with email/password
2. **Response**: JWT token + user info
3. **Storage**: Token stored in memory/localStorage (implementation-dependent)
4. **Request Headers**: `Authorization: Bearer <token>`
5. **Token Refresh**: Not implemented in v1 (tokens are short-lived, re-login required)

---

## UI Screen → API Mapping

### Krithi List Screen
- **Load**: `GET /v1/admin/krithis?workflowState=...&page=...`
- **Search**: `GET /v1/admin/krithis?q=...&composerId=...`
- **Filter**: Query parameters on same endpoint

### Krithi Detail/Edit Screen
- **Load**: `GET /v1/admin/krithis/{id}`
- **Load notation**: `GET /v1/admin/krithis/{id}/notation` (if VARNAM/SWARAJATHI)
- **Save**: `PUT /v1/admin/krithis/{id}`
- **Add variant**: `POST /v1/admin/krithis/{id}/variants`
- **Add notation**: `POST /v1/admin/krithis/{id}/notation`

### Import Review Screen
- **Load**: `GET /v1/admin/imports/krithis?status=pending`
- **Map**: `POST /v1/admin/imports/krithis/{id}/map`
- **Reject**: `POST /v1/admin/imports/krithis/{id}/reject`

### Dashboard Screen
- **Load**: `GET /v1/admin/dashboard`
- Returns aggregated statistics (counts by state, musical form, etc.)

---

## Notes

- All admin mutations require JWT authentication
- All mutations write to `audit_log` table
- Notation endpoints validate `musicalForm` before allowing operations
- Public endpoints only return published Krithis
- Admin endpoints can access all workflow states