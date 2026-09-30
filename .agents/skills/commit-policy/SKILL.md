| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-30 |
| **Author** | Sangeetha Grantha Team |

# Commit Policy

You are responsible for ensuring that all changes committed to the repository adhere to strict traceability, security guardrails, and full delivery lifecycle conventions.

## 0. Branch naming

Do not use Cursor’s default `cursor/` prefix unless the user explicitly asks for it.

| Work | Branch name | Example |
|:---|:---|:---|
| Conductor track | `track-<nnn>-<kebab-slug>` | `track-121-frontend-toolchain` |
| No track | `<type>/<kebab-slug>` (`fix`, `feat`, `docs`, `chore`, `ci`) | `fix/ragamalika-parser-and-section-matching` |

Reuse the current branch when it already matches. Do not commit on `main` unless the user explicitly asks.

Cursor agents: `.cursor/rules/git-conventions.mdc` restates this for always-on agent context.

## 1. Traceability (The Reference Rule)

**EVERY** commit must be linked to a specific documentation file in `application_documentation`. This ensures that every line of code exists for a documented reason.

### Commit Message Format

The preferred format includes a TRACK-ID prefix when a conductor track exists:

```text
<TRACK-ID>: <Short Summary>

Ref: application_documentation/<path-to-file>.md

- <Bullet 1>
- <Bullet 2>
```

If no track exists (e.g. a small documentation fix), omit the TRACK-ID:

```text
<Short Summary>

Ref: application_documentation/<path-to-file>.md

- <Bullet 1>
```

### Rules
1.  **Mandatory Reference**: You CANNOT suggest a commit message without a `Ref:` line.
2.  **Reference Hierarchy**:
    - **Priority**: Always reference a file in `application_documentation/` — preferably in `application_documentation/10-implementations/` for implementation work.
    - **Fallback**: Reference a `conductor/tracks/TRACK-*.md` file *only* if no relevant implementation doc exists.
3.  **Accuracy Check**: The `<Title>`, `<Short Summary>`, and `<Detailed Description>` MUST strictly match the actual changes in the `git diff`. Hallucinating version numbers or unintended changes is a critical failure.
4.  **Existing File**: The file referenced in `Ref:` MUST exist. If it doesn't, create an implementation doc first.
5.  **One Reference Per Commit**: A commit should address only one feature or requirement file. Do not combine unrelated changes.
6.  **Track ID**: Include the TRACK-ID in the commit title when a conductor track exists for the work.

## 2. Security (The No-Secrets Rule)

You must strictly prevent sensitive data from entering the codebase.

### Blocked Items
- **API Keys**: `SG_GEMINI_API_KEY`, `AWS_SECRET_ACCESS_KEY`, `OPENAI_API_KEY`, etc.
- **Configuration Files**: `config/.env`, `config/development.env`, `config/local.env`, `config/.env.production` (all gitignored).
- **Secrets/Tokens**: Any string that looks like a high-entropy secret.

### Pre-Commit Check
Before suggesting `git commit`, mentally (or actually) check:
1.  "Did I add any file that might contain a secret?"
2.  "Am I adding a `.env` file?" (If so, STOP and add it to `.gitignore` instead).
3.  **Strictly Ignore**: No `config/*.env` or `config/local.env` file with secrets must be staged or committed. Use `git restore --staged config/development.env config/local.env` if any were accidentally added.

## 3. End-to-End Delivery Workflow

Commit operations must drive changes through the entire delivery pipeline, not stopping at local commits:

### Step 1: Categorize & Organize Changes
- Group modified and untracked files into logical, atomic changesets (see `change-mapper` skill).
- Ensure each changeset links to exactly one documentation file in `application_documentation/10-implementations/`.
- Ensure Conductor track files (`conductor/tracks/TRACK-XXX-*.md`) and registry (`conductor/tracks.md`) are updated.
- Never use `git add .` or `git commit -a`. Stage files with explicit paths. Leave unrelated files unstaged.

### Step 2: Atomic Commits with Meaningful Messages
- Stage files per changeset: `git add <files>`
- Commit each changeset with the required `<TRACK-ID>: <Short Summary>` and `Ref: application_documentation/...` format.
- Repeat sequentially for each changeset in dependency order.

**Example of a Good Commit:**
```bash
git commit -m "TRACK-080: Add curator review UI with section issue tracking

Ref: application_documentation/10-implementations/track-080-curator-review-ui.md

- CuratorRoutes.kt: GET /v1/admin/curator/stats, GET /v1/admin/curator/section-issues
- CuratorService.kt: stats aggregation, section issue detection per variant
- CuratorReviewPage.tsx: two-tab UI (Pending Matches, Section Issues)
- BulkImportTaskRepository.kt: fixed idempotency key to include jobType"
```

### Step 3: Push Changes to Remote
- Push the current branch and set upstream:
  ```bash
  git push -u origin HEAD
  ```
- Never force-push (`git push --force`). Never skip pre-push hooks.
- If remote `main` has progressed, merge `origin/main` into the branch locally, run tests, and push.

### Step 4: Watch the Pull Request & CI Checks
- Reuse or create the pull request targeting `main`:
  ```bash
  gh pr create --base main --head $(git rev-parse --abbrev-ref HEAD) --title "<TRACK-ID>: <Summary>" --body "<Summary & Ref>"
  ```
- Watch CI checks until completion:
  ```bash
  gh pr checks --watch
  ```
- If checks fail, inspect failure logs (`gh run view --log-failed`), fix on the same branch, commit, push, and re-watch.
- Never merge while checks are failing or pending.

### Step 5: Merge onto Remote Main
- When all automated checks pass:
  ```bash
  gh pr merge --merge --delete-branch
  ```
- Preserves individual logical commits on `main` and deletes the remote head branch upon merge.
- Never use `--admin` to bypass branch protections or required reviews. If blocked, report the PR URL to the user.

### Step 6: Sync Local with Remote Main
- Once merged on remote:
  ```bash
  git fetch origin --prune
  git checkout main
  git pull --ff-only origin main
  ```
- Verify local `main` matches `origin/main` cleanly.

### Step 7: Delete Stale Local and Remote Branches
- Delete the merged feature branch locally:
  ```bash
  git branch -d <branch-name>
  ```
- Delete other local branches already merged into `origin/main` (excluding `main`):
  ```bash
  git branch --merged origin/main | grep -v -E "^\*|main" | while read -r b; do [ -n "$b" ] && git branch -d "$b"; done
  ```
- Delete a local branch whose upstream was pruned only when `git branch -d` accepts it (already merged). Leave unmerged branches in place:
  ```bash
  git branch -vv | awk '/: gone]/ {print $1}' | while read -r b; do [ -n "$b" ] && git branch -d "$b"; done
  ```
- Delete other remote branches already merged into `origin/main` (excluding `origin/main` and `origin/HEAD`):
  ```bash
  git branch -r --merged origin/main | grep -v -E "origin/main|origin/HEAD" | sed 's/origin\///' | while read -r b; do [ -n "$b" ] && git push origin --delete "$b"; done
  ```
- Never delete `main`. Never delete an unmerged branch.
