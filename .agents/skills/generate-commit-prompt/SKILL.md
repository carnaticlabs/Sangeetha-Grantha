---
name: generate-commit-prompt
description: Generates a clear, structured prompt for a given workflow or skill and specific conductor tracks, ready to be copy-pasted to another AI session or agent. Use when delegating work between agents or handing off track implementation.
---

# Generate Prompt

This skill generates a highly optimized, context-aware prompt that a user can copy and paste into a new AI session or agent context. It ensures the receiving agent strictly follows project workflow rules and drives changes through the full delivery lifecycle: logical organization, meaningful commits, push, PR watch, merge into remote main, local sync, and stale branch deletion.

## How to Use This Skill

When the user asks to generate a prompt for a workflow or skill (e.g. `/retrospective-commit-and-push`, `/pre-commit-validation`) on specific tracks, execute the following steps:

### 1. Identify Target Inputs
- **Target Skill/Workflow**: e.g., `/retrospective-commit-and-push`
- **Target Tracks**: e.g., `TRACK-093`, `TRACK-097`

### 2. Inspect Target Skill Rules
Read the corresponding skill definition in `.agents/skills/<skill-name>/SKILL.md` to ensure the generated prompt reflects the exact rules, file paths, and verification steps.

### 3. Generate Structured Prompt
Produce the prompt in a fenced markdown block so the user can easily copy it.

---

## Output Template

```text
Please execute the [SKILL_NAME] skill to deliver pending changes for the following active tracks:

[LIST_OF_TRACKS_FORMATTED_WITH_MENTIONS]

Execution Requirements:

1. **Logically Organize Changes**: Analyze all modified and untracked files. Isolate and group files strictly into [NUMBER_OF_TRACKS] atomic changesets corresponding to [TRACK_IDS]. Leave any unrelated files unstaged.
2. **Documentation Sync**: Update progress logs and statuses in the listed Conductor track files. Ensure corresponding implementation summary documents in `application_documentation/10-implementations/` are created or updated. These serve as the mandatory `Ref:` targets for the commits.
3. **Meaningful Atomic Commits**: Create separate, atomic commits for each track. Adhere strictly to the project's commit policy (no `git add .`, formatting commit messages as `<TRACK-ID>: <Short Summary>` with mandatory `Ref: application_documentation/...` and descriptive bullet points).
4. **Security Guardrails**: Verify no credentials, tokens, or environment files (`config/development.env`, `config/local.env`) are staged or committed.
5. **Push & Watch PR**: Push changes to the remote branch (`git push -u origin HEAD`), open a pull request targeting `main` via `gh pr create` (or reuse existing), and watch automated CI checks via `gh pr checks --watch` until passing.
6. **Merge to Remote Main & Sync Local**: Once all checks pass, merge the PR into remote `main` using `gh pr merge --merge --delete-branch`. Sync local with remote main (`git checkout main && git pull --ff-only origin main`).
7. **Clean Stale Branches**: Delete merged local and remote branches (safe delete `git branch -d`, prune with `git fetch origin --prune`), clean up stale tracking branches, and never delete unmerged branches or `main`.
```

---

## Example

**Prompt Request:** "Generate a prompt for /retrospective-commit-and-push on TRACK-093 and TRACK-097"

**Generated Output:**

```text
Please execute the /retrospective-commit-and-push skill to organize, commit, push, merge, and clean up pending changes for the following active tracks:

- conductor/tracks/TRACK-093-trinity-krithi-bulk-import.md
- conductor/tracks/TRACK-097-guru-guha-blog-source-adapter.md

Execution Requirements:

1. **Logically Organize Changes**: Analyze all modified and untracked files. Isolate and group the files strictly into two changesets corresponding to TRACK-093 and TRACK-097. Leave any unrelated files unstaged.
2. **Documentation Sync**: Update progress logs and statuses in both Conductor track files. Ensure corresponding implementation summary documents in application_documentation/10-implementations/ are created or updated to serve as the mandatory Ref: targets.
3. **Atomic Commits**: Create separate, atomic commits for each track. Adhere strictly to the project commit policy (no git add ., formatting commit messages as <TRACK-ID>: <Short Summary> with Ref: application_documentation/... line and descriptive bullet points).
4. **Security Guardrails**: Mask sensitive tokens in docs and verify config/development.env and config/local.env are completely excluded.
5. **Push & Watch PR**: Push changes to the remote branch (git push -u origin HEAD), create or reuse the PR via gh pr create, and watch CI checks with gh pr checks --watch until all checks pass.
6. **Merge & Sync**: Merge the PR into remote main with gh pr merge --merge --delete-branch, checkout local main, and pull with git pull --ff-only origin main.
7. **Delete Stale Branches**: Safely delete the merged local branches (git branch -d), prune remote tracking refs (git fetch origin --prune), delete any remote branches already merged into origin/main, and report branch cleanup status.
```
