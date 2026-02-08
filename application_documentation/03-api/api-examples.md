| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# API Examples (cURL)

This document provides practical cURL examples for all Sangita Grantha API endpoints.

---

## Setup

### Base URL

```bash
# Local development
export BASE_URL="http://localhost:8080"

# Production
export BASE_URL="https://api.sangitagrantha.org"
```

### Authentication Setup

```bash
# Get admin user ID (replace with actual query or known ID)
export ADMIN_USER_ID="$(psql -h localhost -U sangita -d sangita_grantha -t -c \
  "SELECT id FROM users WHERE email = 'admin@sangitagrantha.org'" | tr -d ' ')"

# Get JWT token
export JWT=$(curl -s -X POST "$BASE_URL/auth/token" \
  -H "Content-Type: application/json" \
  -d "{\"adminToken\": \"dev-admin-token\", \"userId\": \"$ADMIN_USER_ID\"}" \
  | jq -r '.token')

echo "JWT: ${JWT:0:50}..."
```

---

## 1. Health Check

### Basic Health

```bash
curl -s "$BASE_URL/health" | jq
```

**Response:**
```json
{
  "status": "ok",
  "database": "connected"
}
```

---

## 2. Public Endpoints

### 2.1 Search Krithis

**Basic Search:**
```bash
curl -s "$BASE_URL/v1/krithis/search" | jq
```

**Search with Query:**
```bash
curl -s "$BASE_URL/v1/krithis/search?q=vatapi" | jq
```

**Search with Filters:**
```bash
# By composer
curl -s "$BASE_URL/v1/krithis/search?composerId=<uuid>" | jq

# By raga
curl -s "$BASE_URL/v1/krithis/search?ragaId=<uuid>" | jq

# Combined filters
curl -s "$BASE_URL/v1/krithis/search?q=endaro&composerId=<uuid>&ragaId=<uuid>" | jq

# With pagination
curl -s "$BASE_URL/v1/krithis/search?page=2&size=10" | jq
```

**Response:**
```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Vatapi Ganapatim",
      "incipit": "Vātāpi gaṇapatim bhajeham",
      "composerId": "...",
      "primaryRagaId": "...",
      "talaId": "...",
      "musicalForm": "KRITHI",
      "workflowState": "PUBLISHED"
    }
  ],
  "page": 1,
  "size": 25,
  "total": 123
}
```

### 2.2 Get Krithi Detail

```bash
curl -s "$BASE_URL/v1/krithis/<krithi-uuid>" | jq
```

**Response:**
```json
{
  "krithi": {
    "id": "...",
    "title": "Vatapi Ganapatim",
    "incipit": "Vātāpi gaṇapatim bhajeham",
    "composerId": "...",
    "primaryRagaId": "...",
    "talaId": "...",
    "deityId": "...",
    "templeId": null,
    "primaryLanguage": "sa",
    "musicalForm": "KRITHI",
    "isRagamalika": false,
    "workflowState": "PUBLISHED"
  },
  "composer": { "id": "...", "name": "Muthuswami Dikshitar" },
  "primaryRaga": { "id": "...", "name": "Hamsadhvani" },
  "tala": { "id": "...", "name": "Adi" },
  "deity": { "id": "...", "name": "Ganesha" },
  "lyricVariants": [...],
  "sections": [...],
  "tags": [...]
}
```

### 2.3 Reference Data

**Composers:**
```bash
curl -s "$BASE_URL/v1/composers" | jq
```

**Ragas:**
```bash
curl -s "$BASE_URL/v1/ragas" | jq

# With melakarta filter
curl -s "$BASE_URL/v1/ragas?melakarta=true" | jq
```

**Talas:**
```bash
curl -s "$BASE_URL/v1/talas" | jq
```

**Deities:**
```bash
curl -s "$BASE_URL/v1/deities" | jq
```

**Temples:**
```bash
curl -s "$BASE_URL/v1/temples" | jq
```

**Tags:**
```bash
curl -s "$BASE_URL/v1/tags" | jq
```

---

## 3. Admin Endpoints

> **Note:** All admin endpoints require the `Authorization: Bearer $JWT` header.

### 3.1 Authentication

**Login:**
```bash
curl -s -X POST "$BASE_URL/auth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "adminToken": "dev-admin-token",
    "userId": "<admin-user-uuid>"
  }' | jq
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresInSeconds": 86400,
  "user": {
    "id": "...",
    "email": "admin@sangitagrantha.org",
    "roles": ["admin"]
  }
}
```

### 3.2 Admin Krithi Management

**List All Krithis (Admin):**
```bash
curl -s "$BASE_URL/v1/admin/krithis" \
  -H "Authorization: Bearer $JWT" | jq
```

**Filter by Workflow State:**
```bash
curl -s "$BASE_URL/v1/admin/krithis?workflowState=draft" \
  -H "Authorization: Bearer $JWT" | jq
```

**Create Krithi:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/krithis" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "New Test Krithi",
    "incipit": "Test incipit text",
    "composerId": "<composer-uuid>",
    "primaryRagaId": "<raga-uuid>",
    "talaId": "<tala-uuid>",
    "primaryLanguage": "sa",
    "musicalForm": "KRITHI",
    "isRagamalika": false
  }' | jq
```

**Update Krithi:**
```bash
curl -s -X PUT "$BASE_URL/v1/admin/krithis/<krithi-uuid>" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Krithi Title",
    "workflowState": "in_review"
  }' | jq
```

**Delete Krithi:**
```bash
curl -s -X DELETE "$BASE_URL/v1/admin/krithis/<krithi-uuid>" \
  -H "Authorization: Bearer $JWT" | jq
```

### 3.3 Lyric Variants

**Add Lyric Variant:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/krithis/<krithi-uuid>/variants" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "language": "ta",
    "script": "tamil",
    "variantLabel": "Tamil Traditional",
    "sourceReference": "SSP Vol. 1"
  }' | jq
```

**Update Variant:**
```bash
curl -s -X PUT "$BASE_URL/v1/admin/variants/<variant-uuid>" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "variantLabel": "Updated Label",
    "sourceReference": "New Source"
  }' | jq
```

### 3.4 Sections

**Define Sections:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/krithis/<krithi-uuid>/sections" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "sections": [
      { "sectionType": "PALLAVI", "orderIndex": 0 },
      { "sectionType": "ANUPALLAVI", "orderIndex": 1 },
      { "sectionType": "CHARANAM", "orderIndex": 2 }
    ]
  }' | jq
```

**Add Section Text to Variant:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/variants/<variant-uuid>/sections" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "sections": [
      {
        "sectionId": "<section-uuid>",
        "lyricText": "Pallavi lyrics here...",
        "lyricTextNormalized": "pallavi lyrics here"
      }
    ]
  }' | jq
```

### 3.5 Tags

**Assign Tags:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/krithis/<krithi-uuid>/tags" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "tagIds": ["<tag-uuid-1>", "<tag-uuid-2>"]
  }' | jq
```

**Remove Tag:**
```bash
curl -s -X DELETE "$BASE_URL/v1/admin/krithis/<krithi-uuid>/tags/<tag-uuid>" \
  -H "Authorization: Bearer $JWT" | jq
```

### 3.6 Reference Data Management

**Create Composer:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/composers" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Composer Name",
    "birthYear": 1750,
    "deathYear": 1820,
    "place": "Thanjavur",
    "notes": "Brief biographical notes"
  }' | jq
```

**Create Raga:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/ragas" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Raga Name",
    "melakartaNumber": null,
    "parentRagaId": "<parent-raga-uuid>",
    "arohanam": "S R2 G3 M1 P D2 N3 S",
    "avarohanam": "S N3 D2 P M1 G3 R2 S"
  }' | jq
```

**Create Tala:**
```bash
curl -s -X POST "$BASE_URL/v1/admin/talas" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Tala Name",
    "angaStructure": "4+2+2",
    "beatCount": 8
  }' | jq
```

---

## 4. Import Pipeline

### 4.1 List Imports

```bash
# All pending imports
curl -s "$BASE_URL/v1/admin/imports/krithis?status=pending" \
  -H "Authorization: Bearer $JWT" | jq

# By source
curl -s "$BASE_URL/v1/admin/imports/krithis?sourceId=<source-uuid>" \
  -H "Authorization: Bearer $JWT" | jq
```

### 4.2 Get Import Detail

```bash
curl -s "$BASE_URL/v1/admin/imports/krithis/<import-uuid>" \
  -H "Authorization: Bearer $JWT" | jq
```

### 4.3 Upload CSV

```bash
curl -s -X POST "$BASE_URL/v1/admin/imports/upload" \
  -H "Authorization: Bearer $JWT" \
  -F "file=@/path/to/krithis.csv" \
  -F "sourceId=<source-uuid>" | jq
```

### 4.4 Map Import to Krithi

```bash
curl -s -X POST "$BASE_URL/v1/admin/imports/krithis/<import-uuid>/map" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "krithiId": "<existing-krithi-uuid>",
    "notes": "Mapped to existing Vatapi Ganapatim record"
  }' | jq
```

### 4.5 Reject Import

```bash
curl -s -X POST "$BASE_URL/v1/admin/imports/krithis/<import-uuid>/reject" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "notes": "Duplicate entry - already exists in catalog"
  }' | jq
```

---

## 5. Notation (Varnams/Swarajathis)

### 5.1 Get Notation Variants

```bash
curl -s "$BASE_URL/v1/admin/krithis/<krithi-uuid>/notation" \
  -H "Authorization: Bearer $JWT" | jq
```

### 5.2 Create Notation Variant

```bash
curl -s -X POST "$BASE_URL/v1/admin/krithis/<krithi-uuid>/notation" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "notationType": "SWARA",
    "talaId": "<tala-uuid>",
    "kalai": 1,
    "eduppuOffsetBeats": 0,
    "variantLabel": "Lalgudi Bani",
    "sourceReference": "SSP Notation Book",
    "isPrimary": true,
    "notationRows": [
      {
        "sectionId": "<section-uuid>",
        "orderIndex": 0,
        "swaraText": "S R G M | P D N S ||",
        "sahityaText": "Va ta pi ga na pa tim",
        "talaMarkers": "| ||"
      }
    ]
  }' | jq
```

### 5.3 Update Notation

```bash
curl -s -X PUT "$BASE_URL/v1/admin/notation/<notation-uuid>" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "variantLabel": "Updated Label",
    "isPrimary": false
  }' | jq
```

### 5.4 Delete Notation

```bash
curl -s -X DELETE "$BASE_URL/v1/admin/notation/<notation-uuid>" \
  -H "Authorization: Bearer $JWT" | jq
```

---

## 6. Audit Logs

```bash
# Get recent audit logs
curl -s "$BASE_URL/v1/audit/logs" \
  -H "Authorization: Bearer $JWT" | jq

# Filter by entity
curl -s "$BASE_URL/v1/audit/logs?entityType=krithi" \
  -H "Authorization: Bearer $JWT" | jq

# Filter by action
curl -s "$BASE_URL/v1/audit/logs?action=CREATE" \
  -H "Authorization: Bearer $JWT" | jq
```

**Response:**
```json
[
  {
    "id": "...",
    "actorId": "...",
    "action": "CREATE",
    "entityType": "krithi",
    "entityId": "...",
    "oldValue": null,
    "newValue": { "title": "..." },
    "createdAt": "2026-01-29T10:30:00Z"
  }
]
```

---

## 7. Error Handling

### Common Error Responses

**400 Bad Request:**
```json
{
  "code": "validation_error",
  "message": "Validation failed",
  "fields": {
    "title": "Title is required",
    "composerId": "Invalid UUID format"
  },
  "timestamp": "2026-01-29T10:30:00Z"
}
```

**401 Unauthorized:**
```json
{
  "code": "unauthorized",
  "message": "Invalid or expired token",
  "timestamp": "2026-01-29T10:30:00Z"
}
```

**404 Not Found:**
```json
{
  "code": "not_found",
  "message": "Krithi not found",
  "timestamp": "2026-01-29T10:30:00Z"
}
```

**409 Conflict:**
```json
{
  "code": "conflict",
  "message": "Composer with this name already exists",
  "timestamp": "2026-01-29T10:30:00Z"
}
```

---

## 8. Shell Script Helpers

### Complete CRUD Test Script

```bash
#!/bin/bash
# test-api.sh - Complete API test script

set -e
BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "=== Sangita Grantha API Test ==="

# Health check
echo -n "Health check: "
curl -sf "$BASE_URL/health" | jq -r '.status'

# Get JWT (assumes admin user exists)
echo "Getting JWT..."
ADMIN_USER_ID=$(psql -h localhost -U sangita -d sangita_grantha -t -c \
  "SELECT id FROM users WHERE email = 'admin@sangitagrantha.org'" | tr -d ' ')
JWT=$(curl -sf -X POST "$BASE_URL/auth/token" \
  -H "Content-Type: application/json" \
  -d "{\"adminToken\": \"dev-admin-token\", \"userId\": \"$ADMIN_USER_ID\"}" \
  | jq -r '.token')
echo "JWT obtained: ${JWT:0:20}..."

# Search test
echo -n "Search test: "
SEARCH_COUNT=$(curl -sf "$BASE_URL/v1/krithis/search" | jq '.total')
echo "$SEARCH_COUNT krithis found"

# Get first composer
COMPOSER_ID=$(curl -sf "$BASE_URL/v1/composers" | jq -r '.[0].id')
echo "Using composer: $COMPOSER_ID"

# Create test krithi
echo "Creating test krithi..."
KRITHI_ID=$(curl -sf -X POST "$BASE_URL/v1/admin/krithis" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"API Test Krithi $(date +%s)\",
    \"composerId\": \"$COMPOSER_ID\",
    \"musicalForm\": \"KRITHI\",
    \"primaryLanguage\": \"sa\"
  }" | jq -r '.id')
echo "Created krithi: $KRITHI_ID"

# Update krithi
echo "Updating krithi..."
curl -sf -X PUT "$BASE_URL/v1/admin/krithis/$KRITHI_ID" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"title": "Updated API Test Krithi"}' > /dev/null
echo "Updated"

# Delete krithi
echo "Deleting krithi..."
curl -sf -X DELETE "$BASE_URL/v1/admin/krithis/$KRITHI_ID" \
  -H "Authorization: Bearer $JWT" > /dev/null
echo "Deleted"

echo "=== All tests passed ==="
```

---

## Related Documents

- [API Contract](./api-contract.md)
- [OpenAPI Sync](./openapi-sync.md)
- [Integration Spec](./integration-spec.md)
- [Troubleshooting](../00-onboarding/troubleshooting.md)