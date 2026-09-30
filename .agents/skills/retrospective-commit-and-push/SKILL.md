---
name: retrospective-commit-and-push
description: Logically organize all the changes, commit them with a meaningful message, push the changes, watch the PR, merge the changes onto remote main and sync local with remote branch. Delete any stale local and remote branches.
---

# Retrospective Commit and Push

This skill automates the safe, end-to-end delivery of uncommitted changes across multiple tracks or modules. It takes changes from an uncommitted working tree all the way through to merged remote `main`, local branch synchronization, and stale branch cleanup.

The lifecycle enforces:
1. **Surgical Scan & Logical Categorization** by feature, track, or domain layer.
2. **Synchronizing Documentation** (`application_documentation/10-implementations/` and `conductor/tracks/`).
3. **Atomic Commits** with meaningful messages and mandatory `Ref:` lines.
4. **Security Filtering** (excluding local `.env` and masking secrets).
5. **Pushing** to the upstream remote branch.
6. **Watching the Pull Request (PR)** until all automated CI checks succeed.
7. **Merging onto Remote Main** preserving logical commits.
8. **Syncing Local with Remote Main**.
9. **Deleting Stale Local and Remote Branches**.

> [!IMPORTANT]
> **Non-Negotiable Safety Guardrails**:
> - Never use `git add .` or `git commit -a`. Stage files atomically per changeset.
> - Never commit directly on `main`. Work must originate from `track-<nnn>-<kebab-slug>` or `<type>/<kebab-slug>`.
> - Never stage or commit `.env`, `config/development.env`, `config/local.env`, credentials, or high-entropy tokens.
> - Do not force-push. Do not skip hooks.
> - Never merge while any CI check is pending, failing, or cancelled.
> - Do not override branch protection or required reviews, and do not bypass hooks.
> - Never delete an unmerged branch. Never delete `main`. Always use `git branch -d` (safe delete).

---

## 1. Scan and Enumerate Changes

Inspect the active workspace to identify all modified and untracked files:

```bash
git status
git diff --stat
```

Confirm which branch is active. If on `main`, switch to or create the appropriate feature/track branch before staging any files:
```bash
# Branch naming: track-<nnn>-<kebab-slug> or <type>/<kebab-slug>
git checkout -b track-XXX-my-feature
```

---

## 2. Categorize into Changesets

Group changed and untracked files into distinct, atomic changesets using domain heuristics:

| Pattern / Layer | Target Changeset | Ref Doc Location |
|:---|:---|:---|
| `database/migrations/*.sql`, Exposed table objects | **Database / Schema** | `application_documentation/04-database/schema.md` or track doc |
| `modules/backend/api/...` | **Backend API** | `application_documentation/10-implementations/track-NNN-*.md` |
| `modules/frontend/sangita-admin-web/...` | **Frontend UI** | `application_documentation/10-implementations/track-NNN-*.md` |
| `modules/mobile/...` | **Mobile Multiplatform** | `application_documentation/10-implementations/track-NNN-*.md` |
| `tools/krithi-extract-enrich-worker/...` | **Extraction Worker** | `application_documentation/10-implementations/track-NNN-*.md` |
| `compose.yaml`, `Makefile`, `.agents/`, `.claude/`, `.cursor/` | **Infrastructure & Tooling** | `application_documentation/02-architecture/tech-stack.md` or track doc |
| `conductor/tracks/*`, `application_documentation/**` | **Documentation** | Included in respective track changeset or doc commit |

- **One Ref Per Commit**: Each changeset must link to exactly one documentation file under `application_documentation/` (or `conductor/tracks/` as fallback).
- **Leftover Files**: If any modified or untracked files do not fit the identified changesets, leave them unstaged and report them to the user.

---

## 3. Synchronize Conductor Tracks & Summary Docs

For each identified changeset:
1. **Track File**: Verify `conductor/tracks/TRACK-XXX-*.md` has up-to-date Progress Log entries and status. If new work, create the track file and update the index in `conductor/tracks.md`.
2. **Implementation Summary**: Verify a markdown summary exists in `application_documentation/10-implementations/track-XXX-*.md` to serve as the commit's `Ref:` target.

---

## 4. Security Verification

Before staging files, verify that secrets and local configuration files are excluded:

```bash
# Unstage any accidental environment files
git restore --staged config/development.env config/local.env 2>/dev/null || true

# Verify diff is clean of secrets
git diff | grep -iE "(SG_GEMINI_API_KEY|API_KEY|SECRET|PASSWORD|PRIVATE_KEY)" || echo "No secrets detected"
```

Mask any sensitive tokens in documentation files (e.g. use `<set-via-env>` instead of real values).

---

## 5. Atomic Commit Sequence

For each changeset in dependency order (e.g., Database -> Backend -> Frontend / Worker -> Docs / Infra):

```bash
# 1. Stage only files belonging to this specific changeset
git add <file1> <file2> ...

# 2. Verify staged diff
git diff --cached --stat

# 3. Commit with structured format
git commit -m "<TRACK-ID>: <Short Summary>

Ref: application_documentation/10-implementations/<doc-file>.md

- <Key change 1>
- <Key change 2>"
```

*Note: Omit `<TRACK-ID>:` only when no conductor track exists (e.g. `<type>(<scope>): <Short Summary>`).*

---

## 6. Push Changes to Remote

Push the current branch and establish upstream tracking:

```bash
git push -u origin HEAD
```

**Guardrails**:
- Do not force-push.
- Do not skip pre-push hooks.
- If `origin/main` has progressed and the branch is behind, merge `origin/main` into the branch locally, verify tests pass, and push again. Do not rebase a branch that has already been pushed.

---

## 7. Watch the Pull Request (PR)

Check if a pull request already exists for the current branch:

```bash
gh pr view --json number,url,state,title 2>/dev/null || true
```

If no PR exists, create one targeting `main`:

```bash
gh pr create --base main --head $(git rev-parse --abbrev-ref HEAD) \
  --title "<TRACK-ID>: <Short Summary>" \
  --body "## Summary
- <High-level overview of changes>

Ref: application_documentation/10-implementations/<doc-file>.md
Track: conductor/tracks/<track-file>.md

## Test Plan
- [x] Verified automated tests pass locally"
```

### Watch CI Checks
Monitor the PR checks until they finish:

```bash
gh pr checks --watch
```

**Resolving Check Failures**:
- If any check fails, do not proceed to merge.
- Inspect failure details:
  ```bash
  gh run view --log-failed
  ```
- Fix the issue on the same branch, commit with a proper `Ref:`, push to remote, and rerun `gh pr checks --watch`.

---

## 8. Merge into Remote Main

Once all checks pass and requirements are satisfied, merge the PR into remote `main`:

```bash
gh pr merge --merge --delete-branch
```

*Notes*:
- `--merge` keeps individual logical commits on `main`.
- `--delete-branch` instructs GitHub to delete the remote head branch upon successful merge.
- If branch protection or required reviews block the merge, stop and report the pull request URL and blockers to the user. Do not override branch protection or required reviews, and do not bypass hooks.

---

## 9. Sync Local with Remote Branch

Once the PR is merged on remote:

```bash
# 1. Fetch latest remote state and prune deleted tracking refs
git fetch origin --prune

# 2. Switch to main
git checkout main

# 3. Pull latest remote changes (fast-forward only)
git pull --ff-only origin main
```

Verify that local `main` is completely in sync with `origin/main`:
```bash
git status
git log -1
```

If fast-forward fails due to local divergence on `main`, stop and alert the user.

---

## 10. Delete Stale Local and Remote Branches

Clean up merged branches while safeguarding active and unmerged work.

> [!WARNING]
> Stale branches are those already merged into `origin/main`, or local branches whose upstream was pruned (`[gone]`).
> Never delete `main`. Never delete an unmerged branch. Always use safe delete (`git branch -d`).

### 1. Delete the merged feature branch locally
```bash
# Safe delete (only succeeds if merged)
git branch -d <branch-name>
```

### 2. Verify remote branch is deleted
If the remote branch was not deleted during PR merge:
```bash
git push origin --delete <branch-name> 2>/dev/null || true
```

### 3. Delete other local branches already merged into origin/main
```bash
git branch --merged origin/main | grep -v -E "^\*|main" | while read -r b; do
  [ -n "$b" ] && git branch -d "$b"
done
```

### 4. Delete stale local branches whose upstream is gone
```bash
git branch -vv | awk '/: gone]/ {print $1}' | while read -r b; do
  [ -n "$b" ] && git branch -d "$b" 2>/dev/null || git branch -D "$b"
done
```

### 5. Delete other remote branches already merged into origin/main
```bash
git branch -r --merged origin/main | grep -v -E "origin/main|origin/HEAD" | sed 's/origin\///' | while read -r b; do
  [ -n "$b" ] && git push origin --delete "$b"
done
```

### 6. Report Branch Status
Provide a clear summary of:
- Which branches were deleted (local and remote).
- Which branches remain active and unmerged.
