| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P0 — Blocker (north-star N1) |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W6 Security) |
| **Decisions** | D3 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | none |
| **Blocks** | TRACK-110 (admin-user bootstrap hash) |

# TRACK-114: Authentication Hardening — Password Hashing (N1)

## Goal

Replace the plaintext `hashPassword()` (`UserManagementService.kt:143`, currently returns its input verbatim with a `// NOT SECURE` TODO) with **argon2id**, using **transparent rehash-on-next-login**. Closes the single most dangerous finding in the north-star evaluation. Estimated ~1 day; execute first.

## Context

North-star finding N1 (Blocker): JWT issuance and role claims sit on top of credentials stored in plaintext at rest. There is also no login throttling or lockout. For a single-curator dev system this has been survivable; it must be fixed before any non-localhost deployment. Decision D3 selected **argon2id + rehash-on-login** (low ceremony for a single curator).

## Implementation Plan

### Phase 1 — Hashing
- [ ] Add an argon2id-capable library to `gradle/libs.versions.toml` (Critical Rule #2 — no hardcoded versions). Evaluate `password4j` (argon2id + bcrypt, simple API) vs `de.mkammerer:argon2-jvm`.
- [ ] Implement `hashPassword` / `verifyPassword` with argon2id; document chosen cost parameters (memory, iterations, parallelism).
- [ ] Expose a small shared helper so the login path **and** the admin bootstrap (TRACK-110 / D15) produce identical hash format.

### Phase 2 — Transparent migration
- [ ] Detect legacy (plaintext) credentials safely (format/marker check).
- [ ] Rehash-on-login: on successful auth against a legacy record, transparently re-store as argon2id.

### Phase 3 — Abuse controls
- [ ] Add login throttling / basic lockout (N1 also flags no login rate limiting).
- [ ] AUDIT_LOG entry on password change / rehash (Critical Rule #3).

### Phase 4 — Tests
- [ ] Unit tests: hash ≠ plaintext; verify roundtrip; rehash path; wrong-password rejection; throttling trips.

## Acceptance Criteria

- No plaintext password at rest.
- Existing curator logs in without a manual reset (rehash-on-login works).
- Login throttling demonstrable.
- Tests green.

## Docs to Update

- `application_documentation/02-architecture/decisions/ADR-004-authentication-strategy.md` (note argon2id).
- `application_documentation/00-meta/current-versions.md` (new library).

## References

- [North-Star Evaluation N1](../../application_documentation/north-star-evaluation.md)
- [Implementation Plan — TRACK-114](../../application_documentation/north-star-production-readiness-implementation-plan.md)
