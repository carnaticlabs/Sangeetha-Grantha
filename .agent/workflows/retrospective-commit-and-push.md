---
description: Categorize all uncommitted changes, create track files and implementation summary docs, then commit in atomic chunks (one Ref per commit) and push. Use when you have many mixed changes and want to batch-commit by feature with full traceability.
---

# Retrospective Commit and Push

This workflow automates: **(1)** scanning and categorizing changes, **(2)** creating or updating track files and implementation summary docs in `application_documentation/`, **(3)** committing in atomic changesets with one Ref per commit, **(4)** excluding secrets and local config, **(5)** pushing to the remote.

**Prerequisites:** Follow [.agent/skills/change-mapper/SKILL.md](../skills/change-mapper/SKILL.md) and [.agent/skills/commit-policy/SKILL.md](../skills/commit-policy/SKILL.md). Never use `git add .` or `git commit -a` unless every changed file belongs to the same feature and same Ref.

---

## 1. Scan and List Changes

Run:

```bash
git status
git diff --stat
```

List every **modified** and **untracked** file (exclude `config/development.env` and `config/local.env` from any staging or commit).

---

## 2. Categorize into Changesets

Group files into **Changesets** by feature or track. Use these heuristics:

| Pattern / Content | Likely Changeset | Ref Doc Location |
|:---|:---|:---|
| `database/migrations/*.sql`, Exposed table definitions, DAL repositories | **Database/Schema** | `application_documentation/04-database/schema.md` or an implementation doc |
| `modules/backend/api/.../routes/`, `services/`, `AppModule.kt`, `Routing.kt` | **Backend API** | `application_documentation/10-implementations/track-NNN-*.md` |
| `modules/frontend/sangita-admin-web/src/pages/`, `components/`, `api/client.ts` | **Frontend UI** | `application_documentation/01-requirements/admin-web/prd.md` or implementation doc |
| `tools/krithi-extract-enrich-worker/src/` | **Extraction Pipeline** | `application_documentation/10-implementations/track-NNN-*.md` |
| `compose.yaml`, `Makefile`, `.claude/`, `.agent/` | **Infrastructure/Tooling** | `application_documentation/02-architecture/tech-stack.md` or implementation doc |
| `application_documentation/**`, `conductor/tracks/*` | **Documentation** | The documentation file itself |

- **Unmatched files:** Either assign to an existing track/Ref or create a new implementation summary under `application_documentation/10-implementations/` and (if needed) a new `conductor/tracks/TRACK-XXX-*.md`.
- **One Ref per commit:** Each changeset must have exactly one documentation file in `application_documentation/` that will be the `Ref:` for that commit.

---

## 3. Create or Update Track Files and Summary Docs

For **each** changeset:

1. **Track file**
   - If a conductor track already exists, ensure it's updated (progress log, status).
   - If the change is new, create `conductor/tracks/TRACK-XXX-<slug>.md` (see existing tracks for format) and add a row to `conductor/tracks.md`.

2. **Implementation summary**
   - Create or update a markdown file in `application_documentation/10-implementations/` that will serve as the commit Ref.
   - Use the standard metadata table (Status, Version, Last Updated, Author) and include:
     - Purpose
     - Code changes summary (table: File | Change)
     - Exact `Ref:` line

---

## 4. Mask Sensitive Data in Staged Docs

Before committing any changeset that includes documentation:

- Replace literal secrets (e.g. `JWT_SECRET=...`, `ADMIN_TOKEN=...`, `API_KEY=...`) with placeholders such as `<set-via-env-or-secrets-manager>`.
- Re-run or rely on pre-commit hooks that block Secret/Password/Token patterns.

---

## 5. Commit Each Changeset (Atomic)

For **each** changeset, in order:

1. **Stage only that changeset's files** — never stage `config/development.env` or `config/local.env`.

2. **Commit with Ref** using the format:
   ```text
   <TRACK-ID>: <Short Summary>

   Ref: application_documentation/<path-to-summary-doc>.md

   - <Bullet 1>
   - <Bullet 2>
   ```

3. **If pre-commit fails** (e.g. sensitive data): fix the reported file, `git add` it again, and retry.

4. **Repeat** for the next changeset.

---

## 6. Exclude Local Config (If Ever Staged)

```bash
git restore --staged config/development.env config/local.env
```

---

## 7. Push

```bash
git push origin main
```

---

## 8. Leftovers

If some modified or untracked files did **not** fit any changeset:

- Do **not** commit them in this run.
- Report the list to the user and ask whether to create a new track/Ref or leave them uncommitted.
