Logically organize all the changes, commit them with a meaningful message, push the changes, watch the PR, merge the changes onto remote main and sync local with remote branch. Delete any stale local and remote branches.

Follow [.agents/skills/retrospective-commit-and-push/SKILL.md](../../.agents/skills/retrospective-commit-and-push/SKILL.md), [commit-policy](../../.agents/skills/commit-policy/SKILL.md), and [git-conventions](../../.cursor/rules/git-conventions.mdc). Invoking `/retrospective-commit-and-push` authorizes this full end-to-end sequence. Do not stop after local commits.

## 1. Organize the changes

1. Inspect `git status`, `git diff --stat`, and recent `git log`.
2. Confirm active branch matches convention (`track-<nnn>-<kebab-slug>` or `<type>/<kebab-slug>`). If on `main`, create the branch first. Never commit directly on `main`.
3. Categorize uncommitted changes into logical, atomic changesets by feature/track/layer.
4. Ensure corresponding Conductor track files (`conductor/tracks/TRACK-XXX-*.md`) and implementation summary docs (`application_documentation/10-implementations/track-XXX-*.md`) exist and are updated.
5. Leave unrelated or leftover files unstaged. Never stage `.env`, `config/development.env`, `config/local.env`, credentials, or secrets.

## 2. Commit with meaningful messages

For each changeset in dependency order:
1. Stage only files for that changeset with explicit paths (`git add <file1> <file2> ...`). Never `git add .` or `git commit -a`.
2. Verify staged diff (`git diff --cached --stat`).
3. Commit with the required format:
   ```text
   <TRACK-ID>: <short summary>

   Ref: application_documentation/<path>.md

   - <bullet>
   ```
   (Omit `<TRACK-ID>:` only when no conductor track exists). Every commit requires exactly one `Ref:` line pointing to an existing file in `application_documentation/`.

## 3. Push changes

Push the branch and establish upstream tracking:

```bash
git push -u origin HEAD
```

Never force-push (`git push --force`). Never skip pre-push hooks. If `origin/main` has moved and branch is behind, merge `origin/main` locally, verify tests pass, and push.

## 4. Watch the pull request

1. Reuse an open PR or create one targeting `main`:
   ```bash
   gh pr create --base main --head $(git rev-parse --abbrev-ref HEAD) --title "<TRACK-ID>: <Short Summary>" --body "<Summary & Ref doc link>"
   ```
2. Watch CI checks until they finish:
   ```bash
   gh pr checks --watch
   ```
3. Read failing logs (`gh run view --log-failed`) before doing anything else. Fix issues on the same branch, commit, push, and watch again. Never merge while any check is pending or failing.

## 5. Merge into remote main

When all automated checks pass, merge into remote `main` while preserving logical commits:

```bash
gh pr merge --merge --delete-branch
```

If GitHub rejects the merge (branch protections, required reviews), stop and report the PR URL and blockers. Never use `--admin` to bypass requirements.

## 6. Sync local with remote main

Once merged onto remote `main`:

```bash
git fetch origin --prune
git checkout main
git pull --ff-only origin main
```

Ensure local `main` matches `origin/main` cleanly. If fast-forward fails, report divergence.

## 7. Delete stale branches

Stale means already merged into `origin/main`, or a local branch whose upstream is gone after prune. Never delete `main`. Never delete an unmerged branch. Use safe delete (`git branch -d`).

1. Delete the merged feature branch locally:
   ```bash
   git branch -d <branch-name>
   ```
2. Delete remote branch on origin if not already deleted:
   ```bash
   git push origin --delete <branch-name> 2>/dev/null || true
   ```
3. Delete other local branches already merged into `origin/main` (excluding `main`):
   ```bash
   git branch --merged origin/main | grep -v -E "^\*|main" | while read -r b; do [ -n "$b" ] && git branch -d "$b"; done
   ```
4. Delete stale local branches whose upstream is gone:
   ```bash
   git branch -vv | awk '/: gone]/ {print $1}' | while read -r b; do [ -n "$b" ] && git branch -d "$b" 2>/dev/null || git branch -D "$b"; done
   ```
5. Delete other remote branches already merged into `origin/main`:
   ```bash
   git branch -r --merged origin/main | grep -v -E "origin/main|origin/HEAD" | sed 's/origin\///' | while read -r b; do [ -n "$b" ] && git push origin --delete "$b"; done
   ```
6. Report which branches were deleted and which were kept active.
