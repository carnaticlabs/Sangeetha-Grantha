Logically organize all the changes, commit them with a meaningful message, push, watch the pull request, merge into remote `main`, sync the local branch with the remote, and delete stale local and remote branches.

Follow [commit-policy](../../.agents/skills/commit-policy/SKILL.md) and [git-conventions](../../.cursor/rules/git-conventions.mdc) for branch names, the `Ref:` line, and secrets. Invoking `/commit` authorizes this full sequence. Do not stop after the local commit.

## 1. Organize the changes

1. Inspect `git status`, `git diff`, and `git log` so the message matches the diff and recent style.
2. Confirm the branch is `track-<nnn>-<kebab-slug>` or `<type>/<kebab-slug>`. Do not use a `cursor/` prefix unless the user asked. If the work is on `main`, create that branch before committing. Do not commit directly on `main`.
3. Group every relevant change into logical changesets (one feature, fix, or track per commit). Stage each set with explicit paths. Never `git add .` or `git commit -a`.
4. Leave unrelated files unstaged. Never stage `.env`, `config/local.env`, `config/development.env`, credentials, or secrets.

## 2. Commit

For each changeset, commit with a message that matches the diff:

```text
<TRACK-ID>: <short summary>

Ref: application_documentation/<path>.md

- <bullet>
```

Omit `TRACK-ID` only when no conductor track exists. Exactly one `Ref:` line, and the file must exist (prefer `application_documentation/10-implementations/` for implementation work). Do not create an empty commit. If a hook rejects the commit, fix the cause and make a new commit.

## 3. Push

Push the branch and set upstream:

```bash
git push -u origin HEAD
```

Do not force-push. Do not skip hooks. If `origin/main` has moved and the branch is behind, merge `origin/main` into the branch and push again. Do not rebase a branch that has already been pushed.

## 4. Pull request

Reuse the open pull request for this branch. If none exists, create one with `gh pr create` against `main`. The title should match the commit summary. The body should say what changed and how to test it.

Watch checks until they finish:

```bash
gh pr checks --watch
```

Read failing logs before doing anything else. Fix the failure on the same branch, commit, push, and watch again. Do not merge while any check is pending or failing.

## 5. Merge into remote main

When every check has passed, merge with a merge commit so the logical commits stay on `main`:

```bash
gh pr merge --merge --delete-branch
```

If GitHub rejects the merge (failing checks, required review, or branch protection), stop and report the pull request URL and the blocker. Do not override branch protection or required reviews, and do not bypass hooks.

## 6. Sync local with remote

After the merge lands on `origin/main`:

```bash
git fetch origin --prune
git checkout main
git pull --ff-only origin main
```

Local `main` must match `origin/main`. If fast-forward fails, stop and report the divergence.

## 7. Delete stale branches

Stale means already merged into `origin/main`, or a local branch whose upstream is gone after that prune. Never delete `main`. Never delete an unmerged branch. Use `git branch -d` (not `-D`).

1. Delete the branch that was just merged, locally and on `origin`, if it still exists.
2. Delete other local branches that `git branch --merged origin/main` lists, except `main`.
3. Delete other remote branches that `git branch -r --merged origin/main` lists, except `origin/main` and `origin/HEAD`:

```bash
git push origin --delete <branch>
```

4. Report which branches were deleted and which were left because they are not merged.
