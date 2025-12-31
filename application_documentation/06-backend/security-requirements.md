# Sangita Grantha Security Requirements

> **Status**: Draft | **Version**: 0.2 | **Last Updated**: 2025-01-27
> **Owners**: Security Team, Backend Team

**Related Documents**
- [Architecture](./architecture.md)
- [Mutation Handlers](./mutation-handlers.md)
- [Api Contract](../03-api/api-contract.md)

# Security Requirements for Sangita Grantha

This document outlines security requirements and patterns for the
Sangita Grantha backend, focusing on:

- Authentication & authorization for admin workflows.
- Transport security (HTTPS).
- Input validation and error handling.
- Audit logging and data protection.

The public API is read-only in v1 and exposes only published Krithis.

---

## 1. Authentication & Authorization

### 1.1 Admin Authentication

- Admins authenticate via `POST /v1/admin/login`.
- Credentials:
  - v1: email + password; later, SSO/IdP integration may be added.
- Requirements:
  - Passwords must be hashed (bcrypt/Argon2) with adequate work factor.
  - Accounts must support lockout after repeated failures.
  - Login attempts (success and failure) should be audit logged.

### 1.2 JWT Tokens (Admin)

- Access tokens are JWTs signed with HMAC secret from env.
- Claims should include:
  - `userId` (UUID).
  - `email`.
  - `roles[]` (e.g. `admin`, `editor`, `reviewer`).
  - `type = "admin"`.
  - `iat`, `exp`.
- Expiry:
  - Access tokens: short-lived (e.g. 1 hour).
  - Refresh strategy is optional for v1; can be added later.

### 1.3 Role-Based Access Control (RBAC)

- Roles are defined in `roles` and assigned via `role_assignments`.
- Required roles by class of operation:

| Operation                | Required Role(s)         |
|--------------------------|--------------------------|
| View admin Krithi list   | `editor`, `reviewer`, `admin` |
| Create/update Krithis    | `editor`, `admin`        |
| Publish/archive Krithis  | `reviewer`, `admin`      |
| Create/update notation   | `editor`, `admin`        |
| Delete notation           | `admin`                  |
| Import mapping/review    | `editor`, `reviewer`, `admin` |
| Tag catalog maintenance  | `admin` (or specific curation role) |

- Enforcement must occur at route or service boundary using the
  principal and role claims.

---

## 2. HTTPS & Transport Security

- All production endpoints must be served over HTTPS.
- TLS version: 1.2 or newer.
- Secrets (JWT secret, DB password) must **never** be logged.

Recommended headers via Ktor `DefaultHeaders`:

- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Referrer-Policy: strict-origin-when-cross-origin`

---

## 3. Input Validation & Error Handling

- All admin mutations must validate incoming JSON against DTOs and
  business rules.
- Typical checks:
  - Required fields present.
  - Enum values valid.
  - UUIDs well-formed.
- Validation failures → `400 Bad Request` with structured error body
  (`code = validation_error`).
- Unexpected errors → `500 Internal Server Error` with generic message;
  details logged on server only.

---

## 4. Audit Logging

- Every mutation (create/update/publish/archive, import mapping, tag
  changes) must write an `audit_log` row.
- Required fields:
  - `actor_user_id` (admin user ID).
  - `action` (`CREATE`, `UPDATE`, `PUBLISH`, `ARCHIVE`, `IMPORT_MAP`, etc.).
  - `entity_table` and `entity_id`.
  - `changed_at` (UTC timestamp).
  - `diff` JSONB (before/after or request payload).
  - Optional `metadata` JSONB (e.g. `source: "admin_web"`).

Audit patterns are detailed in `mutation-handlers.md`.

---

## 5. Data Protection

- No end-user PII beyond admin emails and names is stored in v1
  (public app is read-only catalog).
- If additional PII is introduced later (e.g. user accounts), apply:
  - Strong password hashing.
  - Minimal data collection.
  - Retention and deletion policies.

---

## 6. API Security Summary

- Public endpoints:
  - Read-only.
  - Only serve Krithis with `workflow_state = 'published'`.
- Admin endpoints:
  - Require JWT with appropriate role claims.
  - Must not leak internal errors or stack traces.
  - Must be rate-limited if exposed to the public internet.

This document should be revisited whenever:
- New mutation types are added.
- Auth strategy changes.
- Sensitive data is introduced into the system.
