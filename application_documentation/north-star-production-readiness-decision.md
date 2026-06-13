| Metadata | Value |
|:---|:---|
| **Status** | Awaiting decisions |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Seshadri (decisions) · prepared from north-star + integration-tests analysis |
| **Source docs** | [north-star-evaluation.md](./north-star-evaluation.md), [07-quality/integration-tests-approach.md](./07-quality/integration-tests-approach.md), [ADR-013](./02-architecture/decisions/ADR-013-db-migration-with-flyway.md) |
| **Feeds into** | Detailed implementation plan + conductor tracks (TRACK-110+) under [TRACK-109 epic](../conductor/tracks/TRACK-109-production-readiness-roadmap.md) |

# North-Star Production Readiness — Decision Checklist

> **How to use this file:** each decision has a **`▶ My decision:`** line — write your answer there (accept the recommendation, pick an option letter, or write your own). Use the **`Notes:`** line for caveats or conditions. When done, hand this back and I'll generate the implementation plan and track files from your answers.
>
> **Quick path:** if you agree with a recommendation, just write `Accept` on its decision line. For a section where you accept everything, write `Accept all` at the section header.

**Legend** — 🔴 time-sensitive (interacts with in-flight work) · 🟡 shapes structure · 🟢 default-ready (sane recommendation, low risk to accept)

---

## Status tracker

| # | Decision | Type | Recommendation | My call | Done |
|:---|:---|:---|:---|:---|:---:|
| D1 | Versioned canon vs Trinity import timing | 🔴 | (a) pause at checkpoint, land revisioning first | | ☐ |
| D2 | Flyway rename freeze window | 🔴 | After next TRACK-093 checkpoint commit | | ☐ |
| D3 | Password hashing algorithm + rehash | 🔴 | argon2id, rehash-on-login | | ☐ |
| D4 | Where the initiative lives in registry | 🟡 | (a) TRACK-110–113 under TRACK-109 | | ☐ |
| D5 | Fate of TRACK-035 & TRACK-014 | 🟡 | Revive 035; supersede 014 | | ☐ |
| D6 | Scope boundary (defer N4/N6/public) | 🟡 | Defer all three; OpenAPI direction TBD | | ☐ |
| D7 | CI mode (Testcontainers vs services) | 🟢 | Testcontainers everywhere | | ☐ |
| D8 | Merge gating policy | 🟢 | Blocking, PR-triggered, from day one | | ☐ |
| D9 | Test time budgets | 🟡 | unit <30s, integ <3min, E2E nightly | | ☐ |
| D10 | Test organization (tags vs source sets) | 🟢 | `@Tag("integration")` + task | | ☐ |
| D11 | Shared test-support module | 🟢 | Duplicate now, extract at DAL suite | | ☐ |
| D12 | E2E money paths | 🟡 | The three listed | | ☐ |
| D13 | Frontend component tests placement | 🟢 | Separate track, after CI | | ☐ |
| D14 | Flyway distribution | 🟢 | Docker image, version-pinned | | ☐ |
| D15 | Seed tiering specifics | 🟡 | 01(−admin)/03/04/05 → `R__`; 02 → dev | | ☐ |
| D16 | Legacy tracking-table drop timing | 🟢 | After one verified `db-reset` cycle | | ☐ |
| D17 | Podman spike timing | 🟢 | Defer to Phase 3 | | ☐ |
| D18 | Repo hygiene (N8) bundling | 🟢 | Separate small track, before CI | | ☐ |

---

## A. Time-sensitive sequencing calls 🔴
*These interact with TRACK-093 (Trinity import, In Progress) and get harder every week. Decide these first.*

### D1 — Versioned canon: before or after the Trinity import completes?
**Context:** north-star N5 / Phase 2. Retrofitting revision history onto ~1,245 imported krithis is far harder than recording it from day one, and TRACK-093 is in progress now.
**Options:**
- **(a)** Pause TRACK-093 at a checkpoint → land `krithi_revisions` + provenance FKs → resume.
- **(b)** Finish the import, accept the retrofit cost later.

**Recommendation:** (a) *if* the import has a natural pause point. This is the single most consequential ordering decision on the list.

> **▶ My decision:**
> **Notes (e.g., is there a clean pause point in TRACK-093?):**
The krithis in the database can be re-imported from scratch, we can go ahead with the pause. It's better to have a clean pause
> point and make necessary changes to fix this holistically.
---

### D2 — Flyway rename freeze window
**Context:** the `V`-prefix rename (ADR-013) touches all 43 migration files and must not race a TRACK-093 / TRACK-096 migration landing mid-rename.
**Decide:** the ~2-day cutover window, and whether new migrations are frozen during it.

**Recommendation:** do it immediately after the next TRACK-093 checkpoint commit.

> **▶ My decision (window + freeze yes/no):**
> **Notes:**
Lets freeze the new migrations for a day and prioritize the rename to go ahead with Flyway.
---

### D3 — Password hashing: algorithm + rehash strategy
**Context:** north-star N1 (Blocker). `hashPassword()` returns plaintext. Blocks all of Phase 0.
**Decide two things:**
- Algorithm: **argon2id** vs **bcrypt**
- Rehash: **on-next-login** (transparent) vs **forced reset** (all users re-set passwords)

**Recommendation:** argon2id + rehash-on-login (single curator = low ceremony). One day of work — do it first.

> **▶ My decision (algorithm + rehash):**
> **Notes:**
Agree with the recommendation. argon2id + rehash-on-login
---

## B. Conductor structure 🟡
*Shapes the implementation plan and the track files I'll generate.*

### D4 — Where does this initiative live in the track registry?
**Options:**
- **(a)** New track family TRACK-110+ feeding TRACK-109's W2 (CI/CD) workstream.
- **(b)** Everything under the TRACK-109 epic directly.

**Recommendation:** (a). TRACK-109 is an epic by design ("each workstream spawns its own TRACK-XXX"). Proposed mapping:

| Track | Scope | Maps to approach-doc steps |
|:---|:---|:---|
| TRACK-110 | Testcontainers substrate + Flyway cutover | Steps 1–2 |
| TRACK-111 | DAL suite + CI activation | Steps 3–4 |
| TRACK-112 | Money-path service & API scenarios | Step 5 |
| TRACK-113 | Worker + E2E | Step 6 |

> **▶ My decision (option + any track-split changes):**
> **Notes:**
Agree with the recommendation. Option (a).
---

### D5 — Fate of the two deferred test tracks
**Context:**
- **TRACK-035** (Playwright E2E) — already has completed scaffolding (`e2e/` config, global setup, auth fixture).
- **TRACK-014** (bulk-import QA) — overlaps the new S5/A-series scenarios.

**Recommendation:** revive TRACK-035 as-is for Step 6 (the E2E layer); mark TRACK-014 **Superseded** by the new scenario suite with a pointer.

> **▶ My decision (035: revive / fresh — 014: supersede / revive):**
> **Notes:**
Agree with the recommendation. Revive TRACK-035 as-is. But ensure it fits in with larger testcontainers-substrate.
---

### D6 — Scope boundary of this initiative
**Context:** keep this initiative to **Tests + CI + Flyway** only, explicitly deferring:
- **N4** — ops/backups/DR → TRACK-109 Milestone B
- **N6** — OpenAPI generation / contract enforcement
- **Public read surface** (N9)

**Recommendation:** yes, defer all three. *Caveat:* north-star pairs the OpenAPI decision with Phase 1 — decide now only **that** it's deferred, not the generation direction.

> **▶ My decision (confirm deferrals):**
> **Notes (anything you want pulled INTO scope?):**
Agree with the recommendation. Yes, defer all three.
---

## C. Test architecture defaults to ratify 🟢
*All from [integration-tests-approach.md](./07-quality/integration-tests-approach.md). Each has a sane default — `Accept all` here is a legitimate answer.*

> **▶ Section shortcut — accept all C defaults?** (yes / no — if no, answer individually below):

### D7 — CI mode
Testcontainers everywhere (identical config) vs service-container escape hatch in CI (~30s faster).
**Recommendation:** Testcontainers everywhere; revisit only if CI minutes hurt. *(GitHub remote confirmed: `carnaticlabs/Sangeetha-Grantha` → Actions; no `.github/` exists yet.)*
> **▶ My decision:** Agree with the recommendation. Testcontainers everywhere.

### D8 — Merge gating policy
Does red `integrationTest` block merge to main from day one or advisory first? PR-triggered or push-to-main?
**Recommendation:** blocking, PR-triggered, from the first green run (advisory CI never graduates).
> **▶ My decision:** Agree with the recommendation. Blocking, PR-triggered

### D9 — Time budgets
Accept unit < 30s, integration < 3 min per commit, E2E nightly-only? (Caps how many S/A scenarios run per-PR.)
**Recommendation:** accept as stated.
> **▶ My decision:** Agree with the recommendation.

### D10 — Test organization
`@Tag("integration")` + `integrationTest` task (proposed) vs separate source sets.
**Recommendation:** ratify tags — lowest friction with the existing 9 classes.
> **▶ My decision:** I would prefer whatever that works with Testcontainers.

### D11 — Shared test-support
Minimal duplication in api/dal vs a dedicated `backend/test-support` module.
**Recommendation:** start duplicated, extract the module when the DAL suite lands.
> **▶ My decision:** Use a structure that makes sense with Testcontainers.

### D12 — E2E money paths
Confirm the three: **login → review → approve**, **bulk import (happy + one failure row)**, **krithi edit with raga change**.
**Recommendation:** confirm as listed.
> **▶ My decision (confirm / swap any):** Agree with the recommendation. Confirm the three listed.

### D13 — Frontend component tests placement
Vitest on `CuratorReviewPage` / `BulkImport` — inside this initiative or a separate track?
**Recommendation:** separate track, after CI exists; don't dilute the backend critical path.
> **▶ My decision:** Agree with the recommendation. Separate track.

---

## D. Flyway execution details 🟡
*ADR-013 is decided; these are execution calls.*

### D14 — Flyway distribution for `make` targets
Docker image (`flyway/flyway`, version pinned in compose/Make) vs native CLI via mise/brew.
**Recommendation:** Docker image — consistent with the dev workflow and pins the version in-repo.
> **▶ My decision:**
> **Notes:**
Agree with the recommendation. Docker image.
---
### D15 — Seed tiering specifics
Confirm the mapping of the five `database/seed_data/` files:

| File | Proposed tier | Mechanism |
|:---|:---|:---|
| `01_reference_data.sql` (minus admin user) | Reference | `R__` repeatable migration |
| `02_sample_data.sql` | Dev sample | `make seed-dev` only |
| `03_composer_aliases.sql` | Reference | `R__` repeatable migration |
| `04_import_sources_authority.sql` | Reference | `R__` repeatable migration |
| `05_raga_reference_data.sql` | Reference | `R__` repeatable migration |
| admin user (carved out of 01) | Environment | env-driven first-run bootstrap *(interacts with D3)* |

**Recommendation:** confirm as tabled.
> **▶ My decision (confirm / adjust):**
> **Notes:**
Agree with the recommendation. Confirm as tabled.
---

### D16 — Legacy tracking-table drop timing
How long do `schema_migrations` / `_sqlx_migrations` survive after baseline — one verified `make db-reset` cycle, or a calendar window?
**Recommendation:** drop after one verified `db-reset` cycle (fresh DBs don't carry them anyway).
> **▶ My decision:**
> **Notes:**
Agree with the recommendation. Drop after one verified `db-reset` cycle.
---

## E. Defer-and-confirm 🟢

### D17 — Podman spike: now or Phase 3?
The approach doc recommends defer (§3.5). Confirm so it doesn't creep into the testing tracks.
**Recommendation:** defer to north-star Phase 3 (production reality), where rootless/Quadlet deliver real value.
> **▶ My decision:**
Agree with the recommendation. Defer to Phase 3.
---

### D18 — Repo hygiene (N8): bundle or separate?
Stray `Users/` / `org/` trees, root logs, and the tracked `ADMIN_TOKEN` rotation are "an afternoon" per the evaluation; doing it first lowers the CI noise floor.
**Recommendation:** separate small track, scheduled **before** CI activation.
> **▶ My decision:**
> **Notes:**
Agree with the recommendation. Separate small track.
---

## Cross-cutting dependency map
*Read this before finalizing — some answers constrain others.*

- **D3 → D15:** the password-hashing choice determines the admin-user bootstrap mechanism (the env-driven first-run script must produce a hash in the chosen algorithm).
- **D1 ↔ D2:** if you pause TRACK-093 for versioned canon (D1a), that pause is also the cleanest Flyway-rename window (D2) — consider landing both in the same freeze.
- **D18 → D8:** repo hygiene before CI means the first CI run is green against a clean root (fewer false-positive scan failures).
- **D4 → everything in C/D:** the track split decides which decisions get documented in which track file.
- **D6:** confirming deferrals keeps TRACK-110–113 scoped; pulling N6/OpenAPI in would add a fifth track.

## Suggested first-week sequence (if you accept the recommendations)
1. **D3** — hash passwords (1 day, unblocks Phase 0).
2. **D18** — repo hygiene + token rotation (an afternoon).
3. **D2** — Flyway rename in the next checkpoint freeze window.
4. **D1** — decide versioned-canon timing against TRACK-093's pause point (highest-stakes; don't rush, but don't let the import outrun it).
5. Stand up **TRACK-110** (substrate + Flyway), then **D7/D8** CI activation as TRACK-111 lands.
