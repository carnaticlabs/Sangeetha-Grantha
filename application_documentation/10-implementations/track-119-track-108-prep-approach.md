| Metadata | Value |
|:---|:---|
| **Status** | Draft — for review |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-14 |
| **Author** | Prep analysis (Claude, for Seshadri) |
| **Covers** | [TRACK-119](../../conductor/tracks/TRACK-119-oauth-otp-auth.md) (OAuth/OTP auth) · [TRACK-108](../../conductor/tracks/TRACK-108-semantic-search.md) (Semantic search) |

# Prep & Approach — TRACK-119 (Interactive Auth) and TRACK-108 (Semantic Search)

This document is the pre-implementation analysis for the two "Ready" tracks. It validates
readiness against the conductor registry and the live codebase/database, records what already
exists vs. what is missing, and lays out a phased approach with the decisions that need an
explicit call before implementation starts.

---

## 1. Readiness validation

### 1.1 Dependency check (both tracks are genuinely unblocked)

| Track | Declared dependency | Status | Verified in code |
|:---|:---|:---|:---|
| TRACK-119 | TRACK-114 (argon2id `PasswordHasher`) | ✅ Completed | `api/.../support/PasswordHasher.kt` present; plaintext `hashPassword()` removed; ADR-004 v1.2 addendum records it |
| TRACK-108 | TRACK-107 (`google-genai` SDK + Batch) | ✅ Completed | Worker on `google-genai>=1.0.0`; `enrich_batch()` Batch-API path in `gemini_enricher.py`; model repointed |

Supporting substrate also in place: TRACK-110/111 (Testcontainers + Flyway test substrate, CI),
TRACK-115 (secret rotation), TRACK-118 (frontend component tests), ktor `rate-limit` plugin
already a dependency **and installed** (`plugins/RequestValidation.kt:46` installs a global
`RateLimit`).

### 1.2 Caveats — "all other tracks completed" is not quite exact

These do not block starting either track, but they shape sequencing:

- **TRACK-117 (versioned canon)** — implementation done, **re-import pending**. Krithi content
  (lyrics/sections) will change when the re-import runs. → TRACK-108's *full* embedding backfill
  should run **after** the re-import; build the pipeline now against a slice.
- **TRACK-093 (Trinity import)** — paused awaiting the same re-import. More corpus = better
  search; same sequencing note as above.
- **TRACK-096** — cleanup phase intentionally waits for the re-import; no interaction with these
  two tracks.
- **TRACK-112 (money-path API tests)** — "Ready" means ready-to-start, not done. Relevant only
  in one way: the new auth and search endpoints should get money-path-style scenario tests, so
  if TRACK-112 runs first, add those scenarios there; if not, each track carries its own.

**Conclusion: both tracks are validated as startable today.** TRACK-119 is P1 (deployment
blocker); TRACK-108 is P2 (feature). They touch disjoint code paths and can proceed in parallel.

### 1.3 Live corpus scale (queried 2026-07-14)

| Entity | Count |
|:---|---:|
| krithis | 1,226 |
| krithi_sections | 4,728 |
| krithi_lyric_variants | 6,809 (~5.5 scripts/languages per krithi) |
| krithi_lyric_sections | 14,898 |
| users / role_assignments | 1 / 1 (single bootstrap admin) |

---

## 2. TRACK-119 — Interactive Auth (OAuth / OTP)

### 2.1 Current state (verified)

The vulnerability is exactly as the track describes, plus one detail the track does not call out:

1. **`POST /v1/auth/token`** ([AuthRoutes.kt:22-38](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AuthRoutes.kt)) —
   gated only by shared `ADMIN_TOKEN`; looks the user up by email/userId (no credential check);
   mints a JWT with **`request.roles` verbatim** (line 36).
2. **`POST /v1/auth/refresh`** (lines 42-55) — re-issues a token with the **roles read from the
   presented JWT**, never re-checked against the DB. Even after `/token` is fixed, refresh would
   perpetuate stale/forged roles for as long as a chain of refreshes lives. Both endpoints must
   derive roles from storage.
3. **JWT plumbing is sound and reusable** — `JwtConfig` (HMAC256, issuer/audience/TTL),
   `Security.kt` JWT validation. No change needed to token *format*, only to how claims are sourced.
4. **RBAC storage already exists and is populated** — `roles` (capabilities JSONB),
   `role_assignments`, and `UserRepository.getUserRoles(userId)`
   ([UserRepository.kt:127](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/UserRepository.kt)). The fix is wiring, not schema.
5. **`users.password_hash` is nullable by design** — V02 comment: *"can be null if external
   identity provider is used"*. The schema anticipated OAuth; no migration needed for the
   passwordless direction on the users table itself.
6. **Rate limiting** — `RateLimit` plugin already installed globally; adding a named, stricter
   limiter for `/v1/auth/*` is configuration, not new dependency work.
7. **Frontend** — [Login.tsx](../../modules/frontend/sangita-admin-web/src/pages/Login.tsx) is a
   raw admin-token + email form; JWT kept in `localStorage` (`api/client.ts:30`). Both get
   replaced/adjusted in this track.
8. **Audit** — `audit_log` has `actor_user_id`/`actor_ip`; **no auth events are audited today**.

### 2.2 What is genuinely missing (net-new build)

- Any OAuth/OIDC integration (no Google/Apple client libs, no callback routes, no state/PKCE
  handling).
- Any OTP delivery channel — the backend has **no email or SMS capability at all**. This is the
  largest hidden cost in the "or OTP" option.
- Identity-linking storage: nothing maps an external identity (`iss`+`sub`) to a `users` row.
- Server-side session/refresh-token revocation story (JWTs are stateless and 15-min-ish TTL via
  env; refresh is unauthenticated-beyond-JWT).

### 2.3 Recommended provider mix (input to ADR-015)

**Recommendation: Google OIDC (authorization-code + PKCE) as the primary interactive login, with
email-OTP as the fallback/secondary path. Defer Apple Sign-In.**

- The user base is a handful of curators with Google identities; Google OIDC is one well-trodden
  integration and removes all credential storage.
- Apple Sign-In matters only when the **iOS app** ships with third-party login (App Store rule:
  offering Google login on iOS requires offering Apple too). The admin web console has no such
  constraint. Design the `user_identities` table provider-agnostically so Apple is additive later.
- Email OTP covers curators without Google accounts and doubles as the account-recovery path.
  It needs an email sender — recommend a minimal SMTP/API client (e.g. Resend/SES/SMTP) behind a
  `MailSender` interface, dev-mode logging the code instead of sending. Mobile-SMS OTP is
  explicitly out of scope for v1 (cost + carrier hassle, no user need).

### 2.4 Proposed schema changes (one Flyway migration, `V48__interactive_auth.sql`)

```sql
-- external identities (Google today, Apple later)
CREATE TABLE user_identities (
  id            UUID PRIMARY KEY,          -- uuidv7 default per V37 convention
  user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider      TEXT NOT NULL,             -- 'google' | 'apple' | 'email-otp'
  subject       TEXT NOT NULL,             -- OIDC sub / email for otp
  email         TEXT,                      -- provider-asserted email at link time
  created_at    TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  last_login_at TIMESTAMPTZ,
  UNIQUE (provider, subject)
);

-- short-lived OTP challenges (hashed code, never plaintext)
CREATE TABLE otp_challenges (
  id           UUID PRIMARY KEY,
  email        TEXT NOT NULL,
  code_hash    TEXT NOT NULL,              -- argon2id via existing PasswordHasher
  expires_at   TIMESTAMPTZ NOT NULL,
  attempts     INT NOT NULL DEFAULT 0,
  consumed_at  TIMESTAMPTZ,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- server-side refresh sessions (revocable)
CREATE TABLE auth_sessions (
  id                 UUID PRIMARY KEY,
  user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  refresh_token_hash TEXT NOT NULL,
  expires_at         TIMESTAMPTZ NOT NULL,
  revoked_at         TIMESTAMPTZ,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  ip                 INET,
  user_agent         TEXT
);
```

Access tokens stay short-lived stateless JWTs; refresh moves to an opaque, hashed, revocable
token in `auth_sessions`. That gives a real logout/revocation story without per-request DB hits.

### 2.5 Phased implementation plan

**Phase A — Close the escalation hole (no new dependencies; ship first, small PR).**
- [ ] `/v1/auth/token`: ignore `request.roles`; derive from `UserRepository.getUserRoles(user.id)`.
- [ ] `/v1/auth/refresh`: re-derive roles from DB (and reject inactive users) instead of copying claims.
- [ ] Named strict `RateLimit` on `/v1/auth/*` (e.g. 5/min per IP) alongside the global limiter.
- [ ] AUDIT_LOG on token issue / refresh / rejection (action `AUTH_*`, `actor_ip` populated).
- [ ] Tests: role-forgery scenario (request roles ≠ assigned roles → assigned win), refresh-derives-from-DB, throttle-429.
- *Result: the privilege-escalation hole is closed even while the shared token still exists.*

**Phase B — ADR-015 + Google OIDC.**
- [ ] ADR-015 "Interactive Authentication — OAuth/OTP" (supersedes the auth-flow part of ADR-004; RBAC part stays).
- [ ] Migration `V48__interactive_auth.sql` (above) + Exposed tables + repos (DTO-out, dbQuery, typed errors).
- [ ] Backend: `GET /v1/auth/oauth/google/start` (state+PKCE, redirect) and `GET /v1/auth/oauth/google/callback`
      (code→token exchange server-side, verify ID token via Google JWKS, upsert `user_identities`,
      **fail closed if no matching active user** — admin pre-provisions users; no open signup).
- [ ] Issue access JWT + refresh session; frontend callback route stores tokens; replace Login.tsx with "Sign in with Google" (+ OTP entry).
- [ ] Audit login success/failure/link events.

**Phase C — Email OTP.**
- [ ] `MailSender` interface + dev logger + one real provider impl (env-gated).
- [ ] `POST /v1/auth/otp/request` (always 200 to avoid user enumeration; rate-limited hard) and
      `POST /v1/auth/otp/verify` (argon2id-hash compare via existing `PasswordHasher`, max 5 attempts, 10-min expiry, single-use).
- [ ] Rehash-on-login: wire `PasswordHasher.verifyAllowingLegacy` into any surviving local-credential path (bootstrap admin), re-storing argon2id on success — closes the TRACK-114 deferred item even if the path is rarely used.

**Phase D — Retire the shared token.**
- [ ] `ADMIN_TOKEN` exchange behind `AUTH_BREAK_GLASS=true` env flag, default **off**; loudly audited when used.
- [ ] Rotate the token (coordinate with TRACK-115 procedure) and update `bootstrap-admin` docs to the OTP/OAuth first-login flow.
- [ ] Update ADR-004 status, `06-backend/security-requirements.md`, OpenAPI spec, getting-started.

### 2.6 Acceptance mapping

| Track acceptance criterion | Where satisfied |
|:---|:---|
| Cannot obtain JWT with unassigned roles | Phase A |
| No shared-secret path mints arbitrary admin token in deployed env | Phase A (derived roles) + Phase D (break-glass off by default) |
| Login throttling demonstrable | Phase A (+ tighter OTP limits in C) |
| Auth events audited | Phases A–D |

---

## 3. TRACK-108 — Semantic Search (Embeddings + pgvector)

### 3.1 Key findings from prep

1. **⚠️ The stock DB image has no pgvector.** `compose.yaml` runs `postgres:18.3-alpine` and the
   Testcontainers substrate pins the same image
   ([SangitaPostgres.kt:19](../../modules/backend/test-support/src/main/kotlin/com/sangita/grantha/backend/testsupport/SangitaPostgres.kt)).
   `CREATE EXTENSION vector` will fail everywhere until the image moves to **`pgvector/pgvector:pg18`**
   (official pgvector build of the same Postgres major). This is a three-place, must-stay-in-sync
   change: `compose.yaml`, `SangitaPostgres.kt`, and any CI service-container config — plus
   `current-versions.md`/tech-stack doc sync per Critical Rule #5.
2. **Exposed has no vector column type.** The DAL will need either a small custom `ColumnType`
   for `vector(n)` or raw-SQL queries inside `DatabaseFactory.dbQuery { }` for the ANN search.
   Recommendation: custom column type for inserts (type-safe backfill/upsert), raw
   parameterised SQL for the `ORDER BY embedding <=> :query LIMIT k` search (Exposed can't
   express the operator; keep it in one `SemanticSearchRepository`).
3. **Multi-script corpus shapes the embedding grain.** 6,809 lyric variants are ~5.5
   renderings (scripts) of the *same* text per krithi. Embedding every `krithi_lyric_section`
   row (14,898) would create ~5 near-duplicate vectors per section that all resolve to the same
   krithi — wasted cost and polluted top-k. **Embed once per `krithi_section` (4,728 rows) from a
   single preferred variant** (priority: roman-diacritic → normalized text → any), plus **one
   whole-krithi embedding** (title + incipit + concatenated preferred-variant sections; 1,226
   rows). ≈ 6,000 vectors total. `gemini-embedding-001` is multilingual, so a query typed in
   Telugu/Tamil/Devanagari still lands on the roman-embedded content.
4. **Scale is trivial — decisions can favour simplicity.** 6k × 768-dim float32 ≈ 18 MB + HNSW
   index. Backfill ≈ 6k embed calls (one Batch job, well under any quota; even sync would take
   minutes). No sharding/partitioning concerns; HNSW defaults (`m=16, ef_construction=64`) fine.
5. **Versioned canon (TRACK-117) interplay — the spec predates it.** Sections now have
   `krithi_section_revisions`. The embedding row should record **content provenance** so
   staleness is detectable: store a `content_hash` (sha256 of the embedded text) alongside
   `model_version`/`dims`. The re-embed job then becomes a reconciliation: recompute hashes for
   current canon text, re-embed rows whose hash differs — idempotent, resumable, and exactly the
   "reconciliation as first-class concern" the track asks for. It also makes the pending
   TRACK-093/117 re-import a non-event: run reconciliation afterwards and only changed content
   re-embeds.
6. **Worker split honoured.** Embedding generation goes in the Python worker
   (`tools/krithi-extract-enrich-worker`) next to `gemini_enricher.py`, reusing its
   `genai.Client` + Batch pattern from TRACK-107 ("Intelligence in Python"). Query-time
   embedding of the *user's search string* is the one exception — it must be low-latency inline
   in the Kotlin API; the backend already has a `GeminiApiClient` with rate limiting that can
   gain a single `embedContent` call ("Ingestion in Kotlin" keeps the read path self-contained).
7. **Existing keyword search is `ILIKE` over normalized columns**
   ([KrithiSearchRepository.kt](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiSearchRepository.kt)) —
   useful as the exact-match arm of a hybrid ranker, but v1 should ship pure semantic first and
   add hybrid only if the eval shows precision gaps (the track already flags scope creep).

### 3.2 Proposed schema (`V49__krithi_embeddings.sql`)

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE krithi_embeddings (
  id            UUID PRIMARY KEY,                 -- uuidv7 default
  krithi_id     UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
  section_id    UUID REFERENCES krithi_sections(id) ON DELETE CASCADE,  -- NULL = whole-krithi
  embedding     vector(768) NOT NULL,
  model_version TEXT NOT NULL,                    -- 'gemini-embedding-001'
  dims          INT  NOT NULL DEFAULT 768,
  content_hash  TEXT NOT NULL,                    -- sha256 of embedded text (staleness detection)
  source_variant_id UUID REFERENCES krithi_lyric_variants(id) ON DELETE SET NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  UNIQUE (krithi_id, section_id, model_version)   -- one live vector per grain per model
);

CREATE INDEX krithi_embeddings_hnsw_idx
  ON krithi_embeddings USING hnsw (embedding vector_cosine_ops);
```

(Queries filter `WHERE model_version = :active` so a model upgrade can dual-write, be evaluated,
then cut over — never mixing versions in one ranking, per the track's risk note.)

### 3.3 Phased implementation plan

**Phase 0 — Infra prerequisite (small, isolated PR).**
- [ ] Swap `postgres:18.3-alpine` → `pgvector/pgvector:pg18` in `compose.yaml` + `SangitaPostgres.kt` (+ CI if it names an image); verify `make db-reset` and the Testcontainers suites pass unchanged; sync `current-versions.md`/tech-stack.

**Phase 1 — Schema.**
- [ ] `V49__krithi_embeddings.sql` (above); update `04-database/schema.md`; DAL table object + custom `vector` column type; D-suite round-trip test.

**Phase 2 — Embedding generation (Python worker).**
- [ ] `embedder.py`: preferred-variant selection, text assembly (section + whole-krithi), sha256 hashing, `client.models.embed_content` with `output_dimensionality=768`, Batch path for backfill with sync fallback (mirror `enrich_batch()`).
- [ ] Backfill CLI: checkpoint by `krithi_id`, idempotent upsert keyed on `(krithi_id, section_id, model_version)`; skip when `content_hash` unchanged.
- [ ] Reconciliation mode: same job, re-run any time; this *is* the re-embed hook. (Optional later: trigger from lyric-mutation code paths; not needed while curation volume is low.)
- [ ] Run backfill on a ~50-krithi slice now; **full-catalogue run scheduled after the TRACK-117/093 re-import** so vectors are built from post-canon text.

**Phase 3 — Query API (Kotlin).**
- [ ] `GeminiApiClient.embedContent(text)` (existing rate limiter applies).
- [ ] `SemanticSearchRepository.findSimilar(queryVec, k, minScore)` — raw SQL, cosine distance, join to krithi summary + matched section label; `publishedOnly` filter aligned with `KrithiSearchRepository`.
- [ ] `POST /v1/search/semantic` (`{query, limit}` → ranked `{krithi, score, matchedSection}`); read-only (no audit needed); rate-limited (each call spends an embed request); OpenAPI spec update.
- [ ] "More like this": `GET /v1/krithis/{id}/similar` — pure vector lookup of the stored whole-krithi embedding, **no Gemini call**, cheapest first UI win.

**Phase 4 — UI + eval gate.**
- [ ] Admin console: semantic search box (extend search page) + "Find similar" on krithi detail; show similarity + matched-section snippet.
- [ ] Eval set: ~30 hand-labelled query→expected-krithi pairs (same-deity/same-kshetra/remembered-line cases; the `carnatic-musicologist` agent can help draft); measure recall@10.
- [ ] Decide 768 vs 1536 dims on eval results (MRL: re-truncate, don't re-architect); record the decision in the track file.

### 3.4 Acceptance mapping

| Track acceptance criterion | Where satisfied |
|:---|:---|
| pgvector via Flyway; full catalogue populated | Phases 0–2 (full run post-re-import) |
| Musically sensible neighbours; recall@k bar | Phase 4 eval gate |
| Latency acceptable; no new datastore | HNSW over ~6k rows (sub-ms scan); Postgres-only |
| Re-embed on lyric change; model_version everywhere | `content_hash` reconciliation + versioned rows |

---

## 4. Cross-track sequencing recommendation

1. **TRACK-119 Phase A immediately** — smallest diff, removes the P1 escalation hole, no new deps.
2. **TRACK-108 Phase 0+1 in parallel** — the image swap and migration are independent of auth.
3. Then interleave: 119-B (OIDC) is the long pole with external setup (Google Cloud OAuth client);
   108-2/3 are self-contained and can fill review gaps.
4. **Hold TRACK-108's full backfill until the TRACK-117/093 re-import lands** (cheap either way,
   but avoids embedding text that is about to be replaced).
5. Registry updates when work starts: mark both tracks In Progress; new ADR-015 for auth;
   TRACK-108 needs no ADR (architecture fixed in the track) unless the provider mix changes.

## 5. Decisions needed from Seshadri before implementation

| # | Decision | Recommendation |
|:---|:---|:---|
| P1 | Auth provider mix for v1 | Google OIDC + email OTP; defer Apple until iOS ships |
| P2 | Email delivery provider for OTP | Interface + dev-log now; pick Resend/SES/SMTP when deploying |
| P3 | Open signup vs pre-provisioned users | Pre-provisioned only (fail closed on unknown Google identity) |
| P4 | Refresh strategy | Opaque revocable refresh sessions (`auth_sessions`), short-lived JWT access tokens |
| P5 | Embedding grain | Preferred-variant per section + whole-krithi (~6k vectors), not per lyric-variant |
| P6 | Backfill timing | Pipeline + slice now; full run after TRACK-117 re-import |

Ref: application_documentation/north-star-evaluation.md
