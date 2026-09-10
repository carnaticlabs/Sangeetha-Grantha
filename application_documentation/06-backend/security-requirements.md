| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Backend security and visibility requirements

---

Authorization belongs at the server boundary. Public catalogue DTOs, JWT identity, stored role assignments, controlled transactions, and audit logging work together to protect editorial state.

## Current identity boundary

The mounted login exchange is `/v1/auth/token`, using the configured admin token and an existing user identity. JWT roles come from storage. `/v1/auth/refresh` requires a valid JWT and reloads assignments. Main admin routes require `grp_sangita_admin`; the dashboard-statistics route is an explicit optional-auth exception.

Account provisioning stores an argon2id password hash. Interactive password login, OAuth/OTP, and fine-grained editor/reviewer tiers remain separate work. See [authentication](../00-meta/quick-reference-auth.md) and [Routing](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Routing.kt).

## Public visibility

Catalogue responses must expose the allowlisted public model, not editorial notes or author/workflow fields. V1 excludes unclassified musical forms; V2 includes published `UNESTABLISHED` compositions. Missing/unpublished reader records use the same unavailable boundary. Lyric requests must validate composition/variant ownership.

Hybrid/semantic search without admin role requires both published composition state and visible search documents. Index existence alone does not grant public visibility. Successful catalogue reads use `no-store`; do not generalize reference-list caching to the reader.

## Mutations and provenance

Validate request types, identifiers, relationships, and operation permissions before persistence. Use `DatabaseFactory.dbQuery` and explicit DTO boundaries. Every mutation must write an audit event. Accepted corpus changes should preserve revision/source attribution through the applicable [canon path](../04-database/versioned-canon.md).

Reference and corpus integrity remain correctness requirements: populate junctions, preserve variants, and route ambiguous raga identities through controlled decisions. An authenticated caller is not evidence that a musicological assertion is correct.

## Configuration and verification

Use explicit deployment credentials and intended CORS/API origins. Treat browser-injected values as public and keep provider credentials server-side. Local defaults are documented behavior, not production configuration. See [configuration](../08-operations/config.md) and [deployment readiness](../08-operations/deployment.md).

Verify anonymous/admin visibility, invalid/expired tokens, non-admin role denial, refresh after role changes, and cross-composition variant requests. [Integration testing](../07-quality/integration-tests-approach.md) describes isolated checks; no security certification is implied by this guide.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
