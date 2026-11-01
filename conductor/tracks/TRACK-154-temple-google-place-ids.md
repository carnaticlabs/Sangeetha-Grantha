| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.1.0 |
| **Last Updated** | 2026-11-01 |
| **Author** | Sangeetha Grantha Team |

# Track: Temple Google Place IDs

**ID:** TRACK-154
**Status:** Complete — Intent, Spec and Plan Accepted; all Plan steps done
**Owner:** Seshadri
**Created:** 2026-10-16
**Updated:** 2026-11-01

## Goal

Give each temple in the catalogue a reviewed Google `place_id`, so [TRACK-151](TRACK-151-rasika-kshetra-exploration.md) can open or draw the temple on Google Maps without our database storing Google-derived coordinates.

## Context

- **Decision (2026-10-16):** Seshadri chose a Google-based design over OpenStreetMap and Wikidata. This replaces version 1.0.0 of this track, which proposed an OSM and Wikidata coordinate pipeline.
- **Measured on the local database (2026-10-16):** 97 temples, none with latitude or longitude. By kind: 2 `LOCALITY`, 84 `COMPLEX`, 11 `SANNIDHI`, 1 `MANDAPAM`. Every temple has a name, city and state. The 12 sub-shrines sit under three parents (ten sannidhis and the Navagraha mandapa under the Tiruvarur complex, one each under the Mayiladuthurai and Kulittalai temples).
- **Google policy (Geocoding policy page, read 2026-10-16):** results shown on a map must appear on a Google Map; caching, pre-fetching and storing content is generally restricted; the `place_id` may be stored indefinitely. The Maps Platform Terms page did not load in full, so the finer rules (for example any short lat/lng caching allowance) must be confirmed in the Spec before anything relies on them.
- **Consequence:** `temples.latitude` and `temples.longitude` must not be filled from Google results. They stay for hand-entered or curator-measured values only. The new identity column is `google_place_id`.
- **Pilot findings that still apply:** name matching is the hard part. Nominatim returned a road for Tiruvarur and a shop for Pashupatinath. Catalogue transliterations (for example Tiruvanaikkaval) differ from map spellings. A Google text search will need the same candidate review, not blind acceptance.
- **Schema today:** `TemplesTable` has latitude and longitude only. No place identity, precision or review state. The admin Temple modal lets a curator type latitude and longitude. `R__seed_09_dikshitar_cycle_memberships.sql` upserts temples but its update list excludes coordinates, so a rerun does not erase them.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (2026-10-24)

### Problem

A kshetra map needs correct places. Matching a temple by name can pick a different temple of the same name (there are several Subrahmanya Swamy and Kasi Viswanatha temples), a town, or a nearby business. Google's terms stop us storing its coordinates, so the durable thing to keep is a verified `place_id`. Without recorded review, nobody can tell a checked match from a guess.

### Proposed outcome

1. **One-time candidate lookup.** An offline script runs Places Text Search for each non-sannidhi temple, using the catalogue name, city and state, and writes candidates (place id, Google's name and address, match score, rank) to a candidate file. It runs server-side or locally, never from the app, with the API key supplied through an environment variable that is never committed or logged.
2. **Review.** Each temple ends `CONFIRMED`, `LOCALITY_ONLY` (the place is the town, not the temple) or `UNRESOLVED`, with an approver. Automatic confirmation only when Google's name and locality match the catalogue exactly after normalisation and there is a single candidate; everything else is a short curator review list.
3. **Sub-shrines inherit.** Sannidhis and mandapams link to their parent's place and say so; they do not get their own Google place.
4. **Storage.** A new versioned migration adds `google_place_id` and a place review state to `temples`; a repeatable seed applies the reviewed file with system-attributed audit rows. No Google name, address or coordinate is stored beyond the place id. Hand-entered coordinates, if any, are never overwritten.
5. **Public contract.** The V2 temple payload exposes the place id and its precision only for `CONFIRMED` rows. A consumer never receives a guessed place.

### Constraints

- Flyway only; never edit a committed `V__` file; corpus data goes in a repeatable seed (TRACK-139 marker rules).
- Comply with Google Maps Platform terms: store only `place_id`, show Google-sourced content on Google Maps with attribution, respect rate limits.
- No API keys in the repository, logs, DTOs or the mobile binary's public config for this track. The lookup key is for the one-time script only; any app-side key belongs to the display decision in TRACK-151.
- Cost: Places Text Search is billed. The Spec must give the expected call count (about 85 temples, plus retries) and the budget cap.
- Never present a `LOCALITY_ONLY` place as the temple.

### Open questions

1. Who supplies and rotates the API key for the lookup, and where is it held (a local environment variable only)?
2. Review tool: the admin Temple modal (needs a place-id field) or a reviewed JSON file in `database/data/`?
3. Is a Google Cloud billing account with a spending cap already in place?
4. Do we also want hand-measured coordinates stored for temples where Google has no match, and by whom?
5. Place data refresh: how often do we re-verify place ids (they can change), and who owns it?

### Decisions on open questions (2026-10-24)

1. The lookup API key is supplied by Seshadri as a local environment variable only (never committed or logged).
2. Review uses a reviewed JSON file in `database/data/`, applied by a repeatable seed; no admin UI change.
3. A Google Cloud billing account with a spending cap is in place; the Spec states the call count against it.
4. Hand-measured coordinates for no-match temples: deferred.
5. Place-id refresh cadence and owner: deferred.

### Completion evidence (fixed in Plan)

Candidate script with recorded-response fixtures and tests, a coverage report by review state, a reviewed data file with approvals, migration and seed tests (rerun, hand-entry protection, no coordinate writes from Google), `make test`, `make test-integration`, `make check-docs`.

*(Superseded by the fixed list in the Plan below: fixtures are synthetic, not recorded, for the terms reason in Flagged concerns.)*

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-10-25

### Requirements

**R1. Candidate lookup (offline, one-time).**
1. A script queries Places API (New) Text Search once per lookup target, using `"<catalogue name>, <city>, <state>"`, and writes ranked candidates to a **local working file outside version control** (`storage/` is gitignored; confirm in the Plan).
2. Lookup targets are `temples` rows whose `place_kind` is not `SANNIDHI` or `MANDAPAM` (sub-shrines never get their own lookup).
3. The API key comes only from the environment (`GOOGLE_PLACES_API_KEY`), is sent in the request header (never the URL), and is never logged, written to a file, or placed in a DTO or the mobile config. Error messages and the call log redact it.
4. The request uses a minimal field mask (place id, display name, formatted address) and `pageSize` of 5 or fewer. The script requests no location or geometry fields.
5. The script enforces a **hard call ceiling** (`PLACES_MAX_CALLS`, default 150) and stops before exceeding it; it is resumable from the working file so a rerun does not repeat answered queries.

**R2. Expected call count against the spending cap.**
- Targets: about 85 (97 temples minus 12 sub-shrines). *Discrepancy to resolve by re-measuring: the Intent's kind counts (2 + 84 + 11 + 1) sum to 98, not 97; the Plan's first step re-runs the count with `GROUP BY place_kind`.*
- Pass 1: one call per target, **about 85 calls**.
- Pass 2 (retry): one variant query (alternate spelling from `temple_names`, or city and state dropped) for each target that is not auto-confirmed. **Planned total about 110–130 calls; worst case 170 if every target needs a retry.** The default ceiling of 150 therefore stops a runaway, and Seshadri raises it deliberately if the worst case is approached.
- This volume is small and one-time. Before the first live call, Seshadri confirms in the Google Cloud console that (a) the billing account's spending cap or budget alert is set, (b) the key is restricted to Places API (New), and (c) the SKU for the requested fields is priced as expected on the current Google pricing page. **The Spec does not assert a price**, because the SKU depends on the field mask and Google's pricing changes; the figure to compare against the cap is calls × the then-current per-call SKU rate.
- The script prints the call count at the end of each run so spend can be reconciled with the console.

**R3. Review states and rules.**
- `CONFIRMED`: the place is this temple. `LOCALITY_ONLY`: Google only has the town or an area, not the temple. `UNRESOLVED`: no acceptable candidate.
- A temple that has not been reviewed has a NULL review state (no fourth state; NULL reads as "not looked at").
- Auto-confirm only when there is **exactly one candidate** and its name and locality equal the catalogue's after normalisation (the repo's existing `name_normalized` and diacritic stripping rules). Everything else goes to a curator review list ordered by risk.
- Every non-NULL state records an approver (`place_reviewed_by`) and a date (`place_reviewed_at`). An automatic confirmation is attributed to `system:TRACK-154:auto`, with the curator's sign-off recorded in the reviewed file's approval list.
- Duplicate guard: no two `CONFIRMED` rows share a `google_place_id`.
- A `LOCALITY_ONLY` place is never presented as the temple, anywhere.

**R4. Sub-shrines inherit.**
- `SANNIDHI` and `MANDAPAM` rows hold no place id and no review state of their own. The V2 payload resolves them through `parent_temple_id` to the parent's place and labels the precision `PARENT_COMPLEX`.
- If the parent is not `CONFIRMED`, the sub-shrine has no place. The seed fails if a sub-shrine row in the reviewed file carries its own place id.

**R5. Storage.**
- A new **versioned** migration (`V68__temple_google_place_review.sql`, next free number to be confirmed; the `V46` gap is existing) adds to `temples`: `google_place_id TEXT`, `place_review_state place_review_state_enum`, `place_reviewed_by TEXT`, `place_reviewed_at TIMESTAMPTZ`. Schema only: no data.
- `CHECK` constraints: `CONFIRMED` requires a non-null place id; `UNRESOLVED` requires null; a place id requires a review state; a partial unique index on `google_place_id` where the state is `CONFIRMED`.
- Nothing from Google but the `place_id` is stored: no name, address, coordinates, rating, photos or URLs. `latitude` and `longitude` are not written by this track (hand-entered values stay untouched).

**R6. Reviewed file and repeatable seed.**
- The reviewed result is a JSON file `database/data/track154-temple-place-review.json`: one entry per reviewed temple, keyed by the **natural key** (`name_normalized`, `city`, `state`, `country`; temple UUIDs are not stable across environments), holding the catalogue name, state, `google_place_id` (when applicable), approver, date and a short curator note. It holds no Google name, address or coordinates.
- A repeatable seed `R__seed_12_temple_place_review.sql` applies it. Flyway cannot read repo files, so the file's content is embedded in the SQL as a JSONB literal by a generator script, and a test fails when the embedded copy and the JSON differ (the pattern TRACK-147 uses for its manifest).
- Seed behaviour: idempotent; touches only the four new columns plus `updated_at`; matches by natural key and **skips with a notice** (not an error) when a temple is absent, so an older or partial database still migrates; writes one `AUDIT_LOG` row per changed temple (`entity_table = 'temples'`, `actor_user_id` NULL, `metadata.actor = 'system:TRACK-154'`), and none when nothing changed.
- Protection: the seed overwrites a row only when its review fields are NULL or were last written by `system:TRACK-154`. A value set by any other actor is left alone and reported.

**R7. V2 contract (jointly with TRACK-151).**
- TRACK-151 owns the V2 temple DTOs in the shared domain; TRACK-154 owns the column, the rule and the mapper logic. The agreed shape: an optional `place` object on the V2 temple **detail** DTO (not the list DTO) with `googlePlaceId: String` and `precision: PlacePrecision` (`TEMPLE` | `PARENT_COMPLEX`). `place` is absent (null) unless the effective state is `CONFIRMED`.
- `LOCALITY_ONLY`, `UNRESOLVED` and unreviewed rows expose nothing; the DTO has no field for review state, approver or notes.
- The rule is enforced in the repository or mapper, not by the client, and has a test per state.

**R8. Out of scope.** Admin UI changes, hand-measured coordinates, refresh cadence, the Google Maps SDK map, and the V1 API (no place id in V1).

### Design

- **Schema:** `place_review_state_enum ('CONFIRMED','LOCALITY_ONLY','UNRESOLVED')`; columns per R5. `TemplesTable` in `modules/backend/dal/.../tables/CoreTables.kt` gains matching columns, and the enum gets a Kotlin counterpart beside `PlaceKind`. The `postgres-flyway-db` skill rules apply (PG18, `V__` for schema, `R__seed_NN_` for data, idempotent).
- **Script:** Python, beside the existing generator scripts in `tools/krithi-extract-enrich-worker/scripts/` (uv-managed, tests in the worker's `tests/`), as three commands: `lookup` (calls Google, writes the local candidate file), `triage` (applies the auto-confirm rule, emits the review list and a coverage report by state), `build` (turns the curator-edited review into the committed JSON and regenerates the embedded SQL literal). Only `lookup` touches the network.
- **Data flow:** `temples` (read-only, via a SQL export or psql) → `lookup` → local candidates → `triage` → curator edits → committed JSON (+ approvals) → `build` → `R__seed_12` → `temples` columns → V2 repository mapper → V2 DTO.
- **Backend:** the V2 repository from TRACK-151 reads the new columns and resolves the parent for sub-shrines in one query (self-join on `parent_temple_id`), returning an explicit DTO; no Exposed entity leaks. Routes call services and repositories as elsewhere. No mutation route is added, so the only writes are the seed's audited ones.
- **Link construction** (TRACK-151, Open in Google Maps): built client-side from `googlePlaceId`, using Google's documented Maps URL form, which needs no app API key. TRACK-151 confirms the exact URL form and its attribution needs.
- **Docs:** update `application_documentation/04-database/schema.md`, the OpenAPI spec (via TRACK-151's V2 changes), and an implementation note in `application_documentation/10-implementations/`.

### Flagged concerns

1. **Google terms (must be confirmed before the first live call, in writing in the Plan).** (a) Storing only `place_id` is permitted indefinitely; (b) the candidate file holds Google names and addresses, which is caching/storing Google content, so it stays local and is deleted after the review rather than committed; (c) Places results used for a map must appear on a Google Map with attribution, and a plain link to Google Maps is the TRACK-151 phase-one path; (d) whether a temporary working copy during review is acceptable, and any short caching allowance, which the Intent notes was not readable in full; (e) place ids can go stale and Google asks that they be refreshed, so the app must tolerate a link that no longer resolves (refresh cadence is deferred, see Open questions). Anything not confirmed is treated as forbidden.
2. **"Recorded-response fixtures" in the Intent's evidence conflict with (b).** Real recorded responses are stored Google content. The Plan uses hand-written synthetic responses in the API's shape.
3. **The "TRACK-139 marker rules" are not defined in TRACK-139's text.** What TRACK-139 does say is that corpus content never goes in `V__` and that reference data lives in idempotent `R__` seeds with provenance and system attribution. R6 is written to that reading; if a specific marker convention was meant (for example a header comment or provenance tag), name it before the Spec is accepted.
4. **Count discrepancy** in the Intent's kind breakdown (R2).
5. **Audit:** the seed writes `AUDIT_LOG` rows with a system actor, as `R__seed_11` does. No admin or API write path exists, so no other audit gap arises.
6. **Contract risk with TRACK-151:** that session is editing TRACK-151 now. If its Spec defines a different DTO shape, R7 must be reconciled before either Spec is accepted. A hand-over note is in the shared notes folder.
7. **Lakshana (Domain Model §6)** does not apply: no krithi, raga, tala or composition data changes. The reviewed file does carry curator judgement about which shrine a place is; where a temple name is ambiguous (several Subrahmanya Swamy or Kasi Viswanatha temples), the curator checks the deity, town and parent complex, and may consult the `carnatic-musicologist` subagent for doubtful kshetra identity. It was not run on this Spec, since nothing here is musicological content.
8. **Junction tables:** none involved. `krithis.temple_id` stays as is.

### Open questions carried forward

1. **Deferred (Intent 4):** hand-measured coordinates for no-match temples.
2. **Deferred (Intent 5):** place-id refresh cadence and owner. Needs an owner before the app relies on a stored id for long.
3. **New:** do the 2 `LOCALITY`-kind rows count as temples for review? Proposed: yes, they are looked up like the others; a confirmed place on a locality-kind row is the locality, which is correct for that row, but TRACK-151 should decide whether to show it as a temple map pin.
4. **New:** the exact marker convention from TRACK-139 (flagged concern 3).
5. **New:** whether TRACK-151 accepts the R7 shape.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-10-25

*Written ahead of Spec acceptance at the requesting session's direction. Nothing here is implemented, and no work starts until the Spec and this Plan are both Accepted.*

### Files that change

| Area | File | Change |
|:---|:---|:---|
| Migration | `database/migrations/V68__temple_google_place_review.sql` (number confirmed at write time) | New enum, four columns, CHECKs, partial unique index, column comments. Schema only. |
| Seed | `database/migrations/R__seed_12_temple_place_review.sql` | Generated; embeds the reviewed JSON; guarded, audited, idempotent. |
| Data | `database/data/track154-temple-place-review.json` | Reviewed placements with approvals. No Google content. |
| Script | `tools/krithi-extract-enrich-worker/scripts/temple_place_lookup.py` (and `src/` helper if the worker layout needs it) | `lookup`, `triage`, `build`. |
| Script tests | `tools/krithi-extract-enrich-worker/tests/test_temple_place_lookup.py` + synthetic fixtures | See Proof. |
| DAL | `modules/backend/dal/.../tables/CoreTables.kt`, a `PlaceReviewState` enum, `TempleRepository.kt` | New columns; read path for V2 with parent resolution. |
| Shared domain | V2 temple DTO (owned by TRACK-151) | Only the `place` field and `PlacePrecision`, coordinated with TRACK-151. |
| Backend tests | dal/api tests and a migration/seed integration test | See Proof. |
| Docs | `application_documentation/04-database/schema.md`, `10-implementations/` note, OpenAPI via TRACK-151, `conductor/tracks.md` | Sync. |
| Ignore rules | `.gitignore` | Only if `storage/` does not already cover the candidate file. |

Not touched: `latitude`/`longitude` writers, the admin Temple modal, V1 routes, the mobile binary's config.

### Order of work

1. **Pre-flight (no code):** re-measure temple counts by kind; settle the Google terms points (flagged concern 1) and record the answers in this track; Seshadri confirms the cap, key restriction and SKU; reconcile the R7 shape with TRACK-151.
2. **Migration and Exposed table** (R5), with a migration test on a fresh Testcontainers database and on a database at V67.
3. **Script and synthetic-fixture tests** (R1–R3); run against fixtures only.
4. **Live lookup pass 1** by Seshadri with the key in the environment; review the printed call count; **pass 2** retries; local candidate file stays local.
5. **Triage and curator review** to produce the reviewed JSON with approvals.
6. **Generate the seed**, drift test, rerun and protection tests, then apply on the dev database with `make migrate`.
7. **Repository mapping and V2 rule tests**, in step with TRACK-151's endpoints.
8. **Docs sync**, delete the local candidate file, record the final call count and state coverage in the Progress Log.

### Risks

- A wrong auto-confirmation sends users to the wrong temple. Mitigation: the strict rule in R3, and the review list is risk-ordered.
- Terms not confirmed. Mitigation: step 1 gates step 4.
- Cost overrun. Mitigation: the hard ceiling and the cap check.
- Place ids go stale. Mitigation: the link tolerates failure; refresh is a deferred decision.
- Natural-key mismatch if temple names are reseeded (R__seed_09 changes a name). Mitigation: the seed reports skipped entries and a test lists them.
- Ordering with TRACK-151: the V2 mapper cannot land before TRACK-151's DTOs. Mitigation: step 7 waits for them.

### Proof (completion evidence, fixed)

1. Script unit tests on **synthetic** fixtures: auto-confirm rule, single versus multiple candidates, key redaction (no key in logs or errors), call ceiling stops the run, resume skips answered queries, field mask contains no geometry fields.
2. Coverage report by review state (CONFIRMED / LOCALITY_ONLY / UNRESOLVED / unreviewed) pasted into the Progress Log, with the final call count.
3. The reviewed JSON present with an approver and date on every entry.
4. Migration test: columns, CHECKs and unique index behave; rerunning `make migrate` is a no-op.
5. Seed tests: idempotent rerun produces no new audit rows; a hand-set `latitude`/`longitude` survives; a row reviewed by another actor is not overwritten; no coordinate column is written; a sub-shrine with its own place id fails the seed; the embedded SQL matches the JSON.
6. V2 mapping tests: place present only for `CONFIRMED`, absent for `LOCALITY_ONLY`, `UNRESOLVED` and unreviewed; sub-shrine inherits `PARENT_COMPLEX` from a confirmed parent and gets nothing from an unconfirmed one.
7. Live check: `make db-reset` then the V2 temple detail for Tiruvarur and one of its sannidhis, showing the expected `place`.
8. Commands: `make test`, `make test-integration`, `make check-docs`, and `make agent-evals` if any agent-config file changed.

## Progress Log

- **2026-10-24**: Spec drafted (Draft) and Plan proposed from the accepted Intent. No code, migration or script written.
- **2026-11-01**: Complete. Lookup pass 1 used 85 calls and pass 2 used 6: 91 of the 150-call ceiling. Coverage: 84 CONFIRMED, 1 LOCALITY_ONLY, 0 UNRESOLVED (85 reviewed temples; the 12 sub-shrines inherit). Approver: Seshadri; the five hand-resolved ids (Bhavani Shrine, Himachala Kumari, Agastiswarar, Saraswati, Manali) were validated in Google Maps. The seed was applied to the dev database: 85 applied, 0 absent, 0 protected, rerun a no-op. Tiruvarur check: the complex returns TEMPLE and its sannidhis PARENT_COMPLEX with the same place id, and the id opens the Tyagarajaswami temple in Google Maps. PRs merged: #46 (migration, script, seed tooling), #48 (repository mapping and V2 tests), #50 (reviewed data and seed, plus the Flyway placeholder fix), #51 (V69: Saraswati Temple corrected from Basrur, Karnataka to Basara, Telangana), #52 (SarAvatI taTa sthala note corrected). The local candidate files were deleted. Open: place-id refresh cadence and owner (deferred in the Intent).
