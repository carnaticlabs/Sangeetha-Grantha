| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P0 — Blocker (north-star N1) |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W6 Security) |
| **Decisions** | D3 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | none |
| **Blocks** | TRACK-110 (admin-user bootstrap hash) |
| **Split out to** | [TRACK-119](./TRACK-119-oauth-otp-auth.md) (interactive login, rehash-on-login wiring, throttling, shared-token/roles fix) |

# TRACK-114: Authentication Hardening — Password Hashing (N1)

## Goal

Replace the plaintext `hashPassword()` (`UserManagementService.kt:143`, returned its input verbatim with a `// NOT SECURE` TODO) with **argon2id**, so **no password is stored in plaintext at rest**, and expose a shared `PasswordHasher` helper that TRACK-110's admin bootstrap reuses. Estimated ~0.5 day; execute first.

> **Re-scoped 2026-06-13.** Investigation found that login does **not** verify a password — `POST /v1/auth/token` ([AuthRoutes.kt](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AuthRoutes.kt)) is gated by a shared `ADMIN_TOKEN` and issues a JWT with caller-supplied `roles`, checking no credential. The N1 framing ("JWT issuance sits on top of plaintext credentials") therefore does not match the code, and the real escalation risk is the shared-token + self-assigned-roles path. Given the chosen future direction is **passwordless (OAuth/OTP)**, a full password-login flow would be throwaway work. This track is scoped to *securing the hash at rest only*; **interactive login, rehash-on-login wiring, login throttling, and the shared-token / caller-roles fix are split out to [TRACK-119](./TRACK-119-oauth-otp-auth.md)**. See [[auth-future-direction]].

## Context

North-star finding N1 (Blocker): credentials stored in plaintext at rest. Decision D3 selected **argon2id**. The shared `PasswordHasher` also ships `verifyAllowingLegacy` (returns a `needsRehash` flag) so the future interactive login (TRACK-119) can do rehash-on-login without redesign — but nothing verifies passwords today, so that path is intentionally dormant.

## Implementation Plan

### Phase 1 — Hashing (done)
- [x] Added `password4j` 1.8.2 to `gradle/libs.versions.toml` (chosen over `de.mkammerer:argon2-jvm` for the cleaner API; Critical Rule #2).
- [x] Implemented `PasswordHasher.hash` / `verify` with argon2id; cost params documented (m=19456 KiB, t=2, p=1, 32-byte hash — OWASP minimum) in `support/PasswordHasher.kt`.
- [x] Helper is a shared `object` (not private) so login and the TRACK-110 admin bootstrap (D15) produce identical PHC-format hashes.
- [x] Replaced the plaintext `hashPassword` in `UserManagementService.createUser` / `updateUser`.

### Phase 2 — Tests (done)
- [x] Unit tests: hash ≠ plaintext; random-salt uniqueness; verify roundtrip; wrong-password rejection; legacy-plaintext detection; `verifyAllowingLegacy` rehash-flag paths.

### Deferred to TRACK-119 (interactive auth)
- [ ] Rehash-on-login wiring (helper ready via `verifyAllowingLegacy`; no login verifies a password yet).
- [ ] Login throttling / lockout; AUDIT_LOG on auth events.
- [ ] Stop honoring caller-supplied `roles`; retire the shared `ADMIN_TOKEN` login exchange.

## Acceptance Criteria

- [x] No plaintext password at rest.
- [x] Tests green.
- [x] Shared hash helper available for the TRACK-110 admin bootstrap.
- ~~Existing curator logs in without a manual reset (rehash-on-login)~~ → TRACK-119 (no password login exists today).
- ~~Login throttling demonstrable~~ → TRACK-119.

## Docs to Update

- `application_documentation/02-architecture/decisions/ADR-004-authentication-strategy.md` (note argon2id).
- `application_documentation/00-meta/current-versions.md` (new library).

## References

- [North-Star Evaluation N1](../../application_documentation/north-star-evaluation.md)
- [Implementation Plan — TRACK-114](../../application_documentation/north-star-production-readiness-implementation-plan.md)
