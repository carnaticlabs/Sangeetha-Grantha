| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P1 — hard blocker for any non-localhost deployment |
| **Type** | Stub — carries the deferred interactive-auth items split out of TRACK-114 |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W6 Security) |
| **Decisions** | D3 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)); supersedes the password-login assumption in north-star N1 |
| **Depends on** | TRACK-114 (argon2id `PasswordHasher` in place) |

# TRACK-119: Interactive Auth — OAuth / OTP (carries deferred N1 items)

## Goal

Replace the shared-admin-token login with a real, per-user interactive authentication path — targeting **OAuth providers (Google, Apple)** and/or **OTP to mobile/email** — and close the auth gaps that TRACK-114 deliberately scoped out.

## Why this exists (split from TRACK-114)

TRACK-114's north-star N1 framing assumed login verified a password. It does not: `POST /v1/auth/token` ([AuthRoutes.kt](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AuthRoutes.kt)) is gated by a **shared `ADMIN_TOKEN`**, then issues a JWT with **caller-supplied `roles`** after an email/userId lookup — no password is checked. Because the user's chosen direction is passwordless (OAuth/OTP), building a full password-login flow under TRACK-114 would have been throwaway work. TRACK-114 was therefore scoped to "secure the hash at rest" only (argon2id `PasswordHasher`, no plaintext stored), and these items moved here. See [[auth-future-direction]].

## Scope (deferred from TRACK-114)

- [ ] **Interactive login** — OAuth (Google/Apple) and/or OTP (mobile/email). Decide the provider mix in an ADR (extend or supersede ADR-004).
- [ ] **Stop honoring caller-supplied `roles`** — derive roles from the authenticated user's stored `role_assignments`, never from the request body. *(This + the shared token is the real privilege-escalation hole; it is the deployment blocker.)*
- [ ] **Retire / break-glass the shared `ADMIN_TOKEN`** login exchange once interactive auth exists (coordinate with the TRACK-115 rotation).
- [ ] **Rehash-on-login** — wire `PasswordHasher.verifyAllowingLegacy` into whatever credential path survives (if any local credentials remain), re-storing argon2id on success.
- [ ] **Login throttling / lockout** — Ktor `rate-limit` plugin is already a dependency; apply it to the auth endpoint(s).
- [ ] **AUDIT_LOG** on auth events (login, token issue, rehash, lockout) per Critical Rule #3.

## Acceptance Criteria

- A user cannot obtain a JWT carrying roles they were not assigned.
- No shared-secret path can mint an arbitrary-identity admin token in a deployed environment.
- Login throttling demonstrable.
- Auth events audited.

## Security note

Until this lands, the system **must not be deployed beyond localhost** — the shared-token + self-assigned-roles path is an open privilege escalation. This is the explicit gate north-star N1 cares about.

## References

- [North-Star Evaluation N1](../../application_documentation/north-star-evaluation.md)
- [ADR-004: Authentication Strategy](../../application_documentation/02-architecture/decisions/ADR-004-authentication-strategy.md)
- [TRACK-114](./TRACK-114-password-hashing.md) (hash-at-rest, completed portion)
