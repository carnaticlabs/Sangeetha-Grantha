| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-09 |
| **Author** | Sangeetha Grantha Team |

# OpenAPI Specification Sync

This document tracks the synchronization between the API contract documentation and the OpenAPI specification file.

---

## 1. Overview

### 1.1 Specification Files

| File | Purpose | Source of Truth |
|------|---------|-----------------|
| `openapi/sangita-grantha.openapi.yaml` | Machine-readable API spec | **Primary** |
| `application_documentation/03-api/api-contract.md` | Human-readable documentation | Secondary |
| `application_documentation/03-api/api-examples.md` | cURL examples and usage | Secondary |

### 1.2 Sync Policy

1. **OpenAPI spec is authoritative** for endpoint definitions
2. **API contract doc** provides context and business logic
3. **Changes must update both** to maintain consistency
4. **CI validation** should catch drift

---

## 2. Endpoint Coverage Validation

### 2.1 Public Endpoints

| Endpoint | OpenAPI | Contract Doc | Status |
|----------|---------|--------------|--------|
| `GET /health` | ✅ | ✅ | Synced |
| `GET /v1/krithis/search` | ✅ | ✅ | Synced |
| `GET /v1/krithis/{id}` | ✅ | ✅ | Synced |
| `GET /v1/composers` | ✅ | ✅ | Synced |
| `GET /v1/ragas` | ✅ | ✅ | Synced |
| `GET /v1/talas` | ✅ | ✅ | Synced |
| `GET /v1/deities` | ✅ | ✅ | Synced |
| `GET /v1/temples` | ✅ | ✅ | Synced |
| `GET /v1/tags` | ✅ | ✅ | Synced |

### 2.2 Admin Endpoints

| Endpoint | OpenAPI | Contract Doc | Status |
|----------|---------|--------------|--------|
| `POST /auth/token` | ✅ | ✅ | Synced |
| `GET /v1/admin/krithis` | ✅ | ✅ | Synced |
| `GET /v1/admin/krithis/{id}` | ✅ | ✅ | Synced |
| `POST /v1/admin/krithis` | ✅ | ✅ | Synced |
| `PUT /v1/admin/krithis/{id}` | ✅ | ✅ | Synced |
| `DELETE /v1/admin/krithis/{id}` | ✅ | ✅ | Synced |
| `POST /v1/admin/krithis/{id}/variants` | ✅ | ✅ | Synced |
| `PUT /v1/admin/variants/{id}` | ✅ | ✅ | Synced |
| `POST /v1/admin/krithis/{id}/sections` | ✅ | ✅ | Synced |
| `POST /v1/admin/krithis/{id}/tags` | ✅ | ✅ | Synced |
| `DELETE /v1/admin/krithis/{id}/tags/{tagId}` | ✅ | ✅ | Synced |

### 2.3 Import Endpoints

| Endpoint | OpenAPI | Contract Doc | Status |
|----------|---------|--------------|--------|
| `GET /v1/admin/imports/krithis` | ✅ | ✅ | Synced |
| `GET /v1/admin/imports/krithis/{id}` | ✅ | ✅ | Synced |
| `POST /v1/admin/imports/krithis/{id}/map` | ✅ | ✅ | Synced |
| `POST /v1/admin/imports/krithis/{id}/reject` | ✅ | ✅ | Synced |
| `POST /v1/admin/imports/upload` | ✅ | ✅ | Synced |

### 2.4 Notation Endpoints

| Endpoint | OpenAPI | Contract Doc | Status |
|----------|---------|--------------|--------|
| `GET /v1/admin/krithis/{id}/notation` | ✅ | ✅ | Synced |
| `POST /v1/admin/krithis/{id}/notation` | ✅ | ✅ | Synced |
| `PUT /v1/admin/notation/{id}` | ✅ | ✅ | Synced |
| `DELETE /v1/admin/notation/{id}` | ✅ | ✅ | Synced |

### 2.5 Audit Endpoints

| Endpoint | OpenAPI | Contract Doc | Status |
|----------|---------|--------------|--------|
| `GET /v1/audit/logs` | ✅ | ✅ | Synced |

---

## 3. Schema Validation

### 3.1 Core DTOs

| Schema | OpenAPI | Kotlin DTO | Status |
|--------|---------|------------|--------|
| `KrithiDto` | `KrithiDetail` | `KrithiDto.kt` | ✅ Synced |
| `KrithiSummary` | `KrithiSummary` | `KrithiDto.kt` | ✅ Synced |
| `ComposerDto` | `Composer` | `ComposerDto.kt` | ✅ Synced |
| `RagaDto` | `Raga` | `RagaDto.kt` | ✅ Synced |
| `TalaDto` | `Tala` | `TalaDto.kt` | ✅ Synced |
| `DeityDto` | `Deity` | `DeityDto.kt` | ✅ Synced |
| `TempleDto` | `Temple` | `TempleDto.kt` | ✅ Synced |

### 3.2 Enum Validation

| Enum | OpenAPI | Kotlin | Database | Status |
|------|---------|--------|----------|--------|
| `WorkflowState` | ✅ | ✅ | ✅ | Synced |
| `LanguageCode` | ✅ | ✅ | ✅ | Synced |
| `ScriptCode` | ✅ | ✅ | ✅ | Synced |
| `MusicalForm` | ✅ | ✅ | ✅ | Synced |
| `ImportStatus` | ✅ | ✅ | ✅ | Synced |

---

## 4. Validation Procedures

### 4.1 Manual Validation

```bash
# Validate OpenAPI spec syntax
npx @redocly/cli lint openapi/sangita-grantha.openapi.yaml

# Generate HTML documentation
npx @redocly/cli build-docs openapi/sangita-grantha.openapi.yaml -o docs/api.html

# Compare with running server (needs swagger-diff or similar)
npx swagger-diff openapi/sangita-grantha.openapi.yaml http://localhost:8080/openapi.json
```

### 4.2 CI Validation

```yaml
# .github/workflows/api-sync.yml
name: API Sync Validation

on:
  push:
    paths:
      - 'openapi/**'
      - 'application_documentation/03-api/**'
      - 'modules/shared/domain/**'

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Lint OpenAPI spec
        run: npx @redocly/cli lint openapi/sangita-grantha.openapi.yaml

      - name: Check for TODO items
        run: |
          if grep -r "TODO" application_documentation/03-api/; then
            echo "Warning: TODO items found in API docs"
          fi

      - name: Validate endpoint count
        run: |
          OPENAPI_COUNT=$(grep -c "paths:" openapi/sangita-grantha.openapi.yaml || echo 0)
          echo "OpenAPI endpoints: $OPENAPI_COUNT"
```

### 4.3 Contract Testing

Use Pact or similar for contract testing:

```kotlin
// Example Pact verification test
@Provider("SangitaGranthaAPI")
@PactFolder("pacts")
class ContractVerificationTest {
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun verifyPact(context: PactVerificationContext) {
        context.verifyInteraction()
    }
}
```

---

## 5. Change Management

### 5.1 When Adding a New Endpoint

1. **Update OpenAPI spec first** (`openapi/sangita-grantha.openapi.yaml`)
2. **Implement endpoint** in backend
3. **Update API contract doc** (`api-contract.md`)
4. **Add cURL example** (`api-examples.md`)
5. **Update this sync doc** if needed

### 5.2 When Modifying an Endpoint

1. **Check breaking change impact**
2. **Update OpenAPI spec**
3. **Update implementation**
4. **Update documentation**
5. **Update any affected tests**

### 5.3 Breaking Changes

For breaking changes, consider:
- API versioning (`/v2/...`)
- Deprecation notices
- Migration guides

---

## 6. Known Discrepancies

| Item | Description | Action |
|------|-------------|--------|
| None currently | - | - |

*This section tracks any known differences that are intentional or pending resolution.*

---

## 7. Tools & Resources

### 7.1 OpenAPI Tools

| Tool | Purpose | Command |
|------|---------|---------|
| Redocly CLI | Lint & build docs | `npx @redocly/cli lint` |
| Swagger UI | Interactive docs | Host `swagger-ui` with spec |
| Swagger Codegen | Generate clients | `swagger-codegen generate` |

### 7.2 Useful Commands

```bash
# Start local Swagger UI
docker run -p 8081:8080 -e SWAGGER_JSON=/spec/sangita-grantha.openapi.yaml \
  -v $(pwd)/openapi:/spec swaggerapi/swagger-ui

# Generate TypeScript types
npx openapi-typescript openapi/sangita-grantha.openapi.yaml -o types/api.ts

# Generate Kotlin client (if needed)
openapi-generator generate -i openapi/sangita-grantha.openapi.yaml \
  -g kotlin -o generated/kotlin-client
```

---

## 8. Related Documents

- [API Contract](./api-contract.md)
- [API Examples (cURL)](./api-examples.md)
- [Integration Spec](./integration-spec.md)
- [OpenAPI Spec](../../openapi/sangita-grantha.openapi.yaml)
## TRACK-140 V2 discovery contract

[TRACK-140](../../conductor/tracks/TRACK-140-rasika-discovery-experience.md) adds `/v2/catalogue` for the new Rasika client. The OpenAPI definitions and both payload fixtures in `shared/domain/model/catalogue/fixtures/` freeze the wire boundary before implementation. V1 retains known musical-form values and excludes UNESTABLISHED records from public lists/counts/details/lyrics; it never substitutes KRITHI. V2 includes published unclassified compositions in unfiltered results, with no form badge or specific-form match. New clients do not fall back silently to V1.

V2 includes discovery; krithi, raga and composer lists/details; stored lyrics; tala/deity/temple directories/details; and language/form directories. Exact raga/composer/tala/deity/temple UUID filters plus language/form combine with AND. Queries submit explicitly; repeated scalar/unknown parameters are rejected. References and source information are allowlisted, with absent section binding and reading-level completeness remaining unknown. Public responses use `Cache-Control: no-store`.

The future release C admin boundary is `/v1/admin/catalogue-features`: list/create, detail/update, publish/unpublish and atomic ordering. It uses the existing ADMIN role, expected revisions, bounded text/record counts and transactional audit. Contracts describe intended behaviour; passing integration/runtime evidence is recorded separately in the track.

Example V2 read: `GET /v2/catalogue/krithis?query=fixture&page=0&pageSize=30&musicalForm=VARNAM`. A valid unmatched filter returns an empty page. Invalid UUIDs or unknown form names return 400; unavailable/wrong-owner readings return the same public 404. The unestablished discovery fixture is synthetic and is not a live catalogue claim.
