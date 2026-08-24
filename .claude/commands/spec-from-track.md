Write a requirements and design spec from an accepted track Intent. Do not implement code.

Track: $ARGUMENTS

1. Find `conductor/tracks/TRACK-*-*.md` for the given id (or the in-progress track if omitted). Read [conductor-track-manager](../../.agents/workflows/conductor-track-manager.md).
2. Stop unless **Intent Status is Accepted**. If it is Draft, fill or correct Intent with the user, then wait for them to set Status to Accepted. Do not invent acceptance.
3. Load constraints:
   - `CLAUDE.md` (Flyway, `DatabaseFactory.dbQuery`, `AUDIT_LOG`, commit `Ref:`)
   - Matching layer skill (`ktor-exposed-backend`, `postgres-flyway-db`, `react-vite-frontend`, `python-extraction-worker`, `kmp-compose-mobile`)
   - [Domain Model §6](../../application_documentation/01-requirements/domain-model.md) when the change touches krithi structure, ragas, talas, seed SQL, or imports
4. If the work is import, seed, lyric sections, raga/tala/composer data, or generated SQL, run the `carnatic-musicologist` subagent on the proposed spec. It reports only; do not silently “fix” lakshana.
5. Write or replace the track **Spec** section:
   - Requirements (what must be true)
   - Design (where it lives in this repo, APIs, data, UI)
   - Flagged concerns (contradictions: Flyway vs ad-hoc SQL, missing audit, Exposed entities leaked, junction tables omitted, lakshana conflicts)
   - Open questions carried forward from Intent
   - Set **Spec Status to Draft** (never Accepted)
6. Ask the user to accept or correct the Spec. Do not run `/plan-from-spec` or edit product code until they set Spec Status to Accepted.
