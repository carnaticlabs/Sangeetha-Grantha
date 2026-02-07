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

List every **modified** and **untracked** file (exclude `config/development.env` from any staging or commit).

---

## 2. Categorize into Changesets

Group files into **Changesets** by feature or track. Use these heuristics for this repo:

| Pattern / Content | Likely Changeset | Ref Doc Location |
|:---|:---|:---|
| `database/migrations/*.sql`, `*TempleSourceCache*`, `GeocodingService`, `TempleScrapingService`, `ApiEnvironment` (geo/gemini), `ImportReview.tsx`/`BulkImport.tsx` (temple/deity), `NameNormalizationService`, bulk-import workers | **TRACK-029** (Kshetra/Temple) | `application_documentation/01-requirements/features/bulk-import/02-implementation/kshetra-temple-mapping-implementation.md` |
| `composer_aliases`, `ComposerAliasRepository`, `ComposerRepository` (findOrCreate alias), `EntityResolutionService` (alias map) | **TRACK-031** (Composer deduplication) | `application_documentation/01-requirements/features/bulk-import/02-implementation/composer-deduplication-implementation.md` |
| `ScrapedLyricVariantDto`, `lyricVariants`, `WebScrapingService` prompt, `ImportService` (lyricVariants), `GeminiApiClient` (escape newlines) | **TRACK-032** (Multi-language lyrics) | `application_documentation/01-requirements/features/bulk-import/02-implementation/multi-language-lyric-extraction-implementation.md` |
| `application_documentation/**` (onboarding, meta, architecture, api, quality, operations), `.agent/workflows/*.md`, `conductor/tracks/TRACK-028*`, `TRACK-030*` | **Documentation & diagrams** | `application_documentation/00-meta/documentation-and-diagram-updates-2026-01.md` |
| `tools/sangita-cli/**`, `commands/docs.rs` | **TRACK-033** (CLI docs) | `application_documentation/08-operations/cli-docs-command.md` |

- **Unmatched files:** Either assign to an existing track/Ref or create a new implementation summary under `application_documentation/` and (if needed) a new `conductor/tracks/TRACK-XXX-*.md`.
- **One Ref per commit:** Each changeset must have exactly one documentation file in `application_documentation/` that will be the `Ref:` for that commit.

---

## 3. Create or Update Track Files and Summary Docs

For **each** changeset:

1. **Track file**
   - If a conductor track already exists (e.g. `conductor/tracks/TRACK-029-*.md`), ensure it’s updated (progress log, status).
   - If the change set is a new feature and has no track, create `conductor/tracks/TRACK-XXX-<slug>.md` (see existing tracks for format) and add a row to `conductor/tracks.md`.

2. **Implementation summary**
   - Create or update a **single** markdown file in `application_documentation/` that will serve as the commit Ref:
     - **Bulk-import features:** `application_documentation/01-requirements/features/bulk-import/02-implementation/<feature>-implementation.md`
     - **Documentation/diagram batch:** `application_documentation/00-meta/documentation-and-diagram-updates-2026-01.md` (or a dated equivalent)
     - **CLI/tooling:** `application_documentation/08-operations/cli-docs-command.md` (or another ops doc)
   - Use the standard metadata table (Status, Version, Last Updated, Author) and include:
     - Purpose
     - Categorization of changes (DB, DAL, API, Frontend, etc.)
     - Code changes summary (retrospective table: File | Change)
     - Exact **Commit Reference** line: `Ref: application_documentation/.../filename.md`

---

## 4. Mask Sensitive Data in Staged Docs

Before committing any changeset that includes documentation:

- In **any** staged markdown, replace literal secrets (e.g. `JWT_SECRET=...`, `ADMIN_TOKEN=...`, `API_KEY=...`) with placeholders such as `<set-via-env-or-secrets-manager>` or `<secret-ref>`.
- Re-run or rely on pre-commit hooks that block Secret/Password/Token patterns.

---

## 5. Commit Each Changeset (Atomic)

For **each** changeset, in order:

1. **Stage only that changeset’s files**
   - Never stage `config/development.env`.
   - Example (TRACK-029):
     ```bash
     git add database/migrations/21__create_temple_source_cache.sql \
       modules/backend/dal/.../TempleSourceCacheTable.kt \
       ... (all files in this changeset)
       application_documentation/.../kshetra-temple-mapping-implementation.md
       conductor/tracks/TRACK-029-*.md
     ```

2. **Commit with Ref**
   - Message format:
     ```text
     <Short title>: <Summary>

     Ref: application_documentation/<path-to-summary-doc>.md

     - <Bullet 1>
     - <Bullet 2>
     ```
   - The `Ref:` path must match the implementation summary doc created in step 3.
   - Run: `git commit -m "<message>"`.

3. **If pre-commit fails** (e.g. sensitive data): fix the reported file (mask secrets), `git add` it again, and retry the commit.

4. **Repeat** for the next changeset until all categorized changes are committed.

---

## 6. Exclude Local Config (If Ever Staged)

If `config/development.env` was staged by mistake:

```bash
git restore --staged config/development.env
```

Do **not** commit it.

---

## 7. Push

After all changesets are committed:

```bash
git push origin main
```

(Or your default branch and remote.)

---

## 8. Leftovers

If some modified or untracked files did **not** fit any changeset:

- Do **not** commit them in this run.
- Report the list to the user and ask whether to create a new track/Ref or leave them uncommitted.

---

## Quick Reference

| Step | Action |
|:---|:---|
| 1 | `git status` + `git diff --stat` |
| 2 | Group files into changesets (TRACK-029, TRACK-031, TRACK-032, docs, TRACK-033, etc.) |
| 3 | Create/update conductor track + implementation summary doc in `application_documentation/` |
| 4 | Mask secrets in any staged docs |
| 5 | For each changeset: `git add <files>`, `git commit -m "..."` with `Ref: application_documentation/...` |
| 6 | `git restore --staged config/development.env` if needed |
| 7 | `git push origin main` |
| 8 | Report uncommitted leftovers to user |
