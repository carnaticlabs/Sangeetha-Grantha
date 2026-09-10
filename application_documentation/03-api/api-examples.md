| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# API request examples

---

These examples target the local backend at `http://localhost:8080`. They show mounted routes and valid request shapes; responses depend on the current database. Use IDs returned by your catalogue rather than copying an ID from an old report.

## Health and public catalogue

```bash
curl --fail http://localhost:8080/health
curl --fail 'http://localhost:8080/v2/catalogue/krithis?page=0&pageSize=5'
curl --fail --get http://localhost:8080/v2/catalogue/krithis \
  --data-urlencode 'query=vatapi' \
  --data-urlencode 'page=0' \
  --data-urlencode 'pageSize=30'
curl --fail http://localhost:8080/v2/catalogue/discovery
curl --fail 'http://localhost:8080/v2/catalogue/ragas?query=kalyani'
curl --fail 'http://localhost:8080/v2/catalogue/composers?query=tyagaraja'
```

Catalogue pagination is zero-based. Parameters are `query` and `pageSize`, not `q` and `size`. An empty `items` array can be valid on a fresh or unpublished corpus.

## Read one stored variant

Replace the placeholders with a composition ID and one of the variant IDs from its detail response:

```http
GET /v2/catalogue/krithis/{id}
GET /v2/catalogue/krithis/{id}/lyrics/{variantId}
```

The first response lists variants and a nullable default. The second returns one stored reading with language/script/source metadata and sections or unsegmented text where available. Both IDs must belong together. Do not add query parameters to detail/lyrics routes.

## Exercise validation and version boundaries

```bash
curl -i 'http://localhost:8080/v2/catalogue/krithis?page=-1'
curl -i 'http://localhost:8080/v2/catalogue/krithis?q=vatapi'
curl -i 'http://localhost:8080/v2/catalogue/krithis?page=0&page=1'
```

Each should return `400` under the catalogue parser. The same core route suffixes exist under `/v1/catalogue`, but V1 excludes `UNESTABLISHED`. V2 includes those compositions when published. Successful catalogue responses carry `Cache-Control: no-store`.

## Search by meaning

```bash
curl --fail http://localhost:8080/v1/search/hybrid \
  -H 'Content-Type: application/json' \
  -d '{"query":"compositions about Ganesha","limit":10}'
```

Use `/semantic` for vector-only retrieval. Add `composerId` or `ragaId` UUID fields to constrain results. With no active profile, hybrid falls back to lexical and semantic returns an empty list. See [search operations](./search.md).

## Token exchange and an authenticated read

The following development example assumes the matching default token and a provisioned user:

```bash
curl --fail http://localhost:8080/v1/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"adminToken":"dev-admin-token","email":"admin@sangitagrantha.org"}'
```

Use the returned `token` in the header for an admin read:

```http
GET /v1/admin/krithis/search
Authorization: Bearer <token>
```

Refresh through `POST /v1/auth/refresh` with a valid Bearer JWT. Role claims are loaded from storage. Do not request roles in the token-exchange body; see [authentication](../00-meta/quick-reference-auth.md).

## Mutations

Use the exact request DTO for the chosen operation. The [API contract](./api-contract.md) links each mounted route family to its implementation. Import review is `POST /v1/admin/imports/{id}/review`, and targeted reingestion is `POST /v1/admin/imports/{id}/reingest`. These change data; inspect the intended record and payload before executing them.

[UI integration](./integration-spec.md) · [OpenAPI status](./openapi-sync.md) · [Post-import verification](../07-quality/qa/test-plan.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
