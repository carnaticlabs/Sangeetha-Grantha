---
name: update-documentation
description: Full documentation sweep. Use when asked to update README.md and application_documentation/ for all changes and features made so far, after a batch of tracks lands, or when docs may have drifted from code.
---

# Update Documentation (full sweep)

Brings `README.md` and `application_documentation/` in line with the code as it is now. Style, headers and links follow [documentation-guardian](../../../.agents/skills/documentation-guardian/SKILL.md); do not restate those rules here. Docs describe repository behavior only: never claim production deployment or native-device acceptance that the evidence does not show.

## 1. Establish what changed

1. Find the last documentation sweep: `git log --oneline -n 1 --grep="docs" -- README.md`, then read the `Last Updated` date in `README.md`.
2. List changes since then: `git log --oneline <that-commit>..HEAD`, `git status --short`, `git diff --stat`.
3. Read `conductor/tracks.md` and every track completed or changed in that window. Tracks are the unit of "features made so far".
4. Run `make check-doc-impact` for the mechanical hits. Treat it as a floor: it does not see paths missing from its rule map.

Write the result as a short change inventory (track, what shipped, which layer) before editing anything.

## 2. Update in this order

1. **Per-track records**: each completed track has `application_documentation/10-implementations/track-<id>-*.md` and a correct status in `conductor/tracks/`. Create missing ones.
2. **Layer docs**, matched to what changed: `01-requirements` (features, domain model), `02-architecture`, `03-api` (`api-contract.md`, `search.md`, and `openapi/sangita-grantha.openapi.yaml`), `04-database` (schema, from `database/migrations/`), `05-frontend`, `06-backend`, `07-quality`, `08-operations`, `09-ai`.
3. **Versions**: if any dependency changed, update `00-meta/current-versions.md`, `02-architecture/tech-stack.md` and `00-onboarding/getting-started.md` together (CLAUDE.md rule 5).
4. **Indexes and status maps**: `application_documentation/README.md`, section `README.md` files, `01-requirements/features/README.md`, `pending_implementation.md`, and the state-of-nation doc if its claims moved.
5. **Root `README.md` last**, since it summarizes everything above: capabilities, search modes, commands table (check against `Makefile` targets), project status and next steps, document map. Bump `Version` and `Last Updated`.
6. **CLAUDE.md** only if commands, skills or rules changed. Keep it canonical and do not duplicate content.

## 3. Rules while editing

- Verify every factual claim against code, a migration, a Makefile target or a track file. If you cannot verify it, leave the old text and list it in the report; do not invent.
- Prefer editing the existing sentence over appending a changelog paragraph. Remove statements that are no longer true.
- Move superseded docs to `archive/` with Status `Archived` per the retention plan; do not delete them.
- Do not touch archive contents, committed `V__` migrations, or `.env` files.

## 4. Verify

Run, and paste the output of, in this order:

```bash
make check-docs
make check-doc-impact
```

Then re-read the root `README.md` once as a new reader: every link resolves, every command exists, no feature is described that is not implemented.

## 5. Report

List files changed grouped by step, the claims left unverified, and any doc gaps that need a new track. Do not commit; commits go through `/retrospective-commit-and-push`, whose Docs-Impact step will then pass.
