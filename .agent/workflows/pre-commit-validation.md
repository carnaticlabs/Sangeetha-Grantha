---
description: Validates staged changes against commit-policy before committing, ensuring documentation references and no secrets.
---

# Pre-Commit Validation

This workflow automates the validation of staged changes against the project's commit-policy skill before executing a commit. Use this to catch policy violations early.

## 1. Full Validation (Recommended)

**Trigger:** "Validate my commit" or "Check before commit"

### Step 1: Capture Staged Files
```bash
git diff --cached --name-only
```

### Step 2: Secrets Scan
Check for sensitive patterns in staged files:
```bash
git diff --cached | grep -iE "(API_KEY|SECRET|PASSWORD|TOKEN|PRIVATE_KEY)" || echo "No secrets detected"
```

**If secrets found:** STOP and unstage the offending file.

### Step 3: Environment File Check
Verify no env files with secrets are staged (e.g. `config/development.env`, `config/local.env`, any `config/*.env`):
```bash
git diff --cached --name-only | grep -q "config/.env" && echo "ERROR: Env file staged!" || echo "OK: No env files staged"
```

**If staged:** Remove them:
```bash
git restore --staged config/development.env config/local.env
```

### Step 4: Documentation Reference Check
Every commit needs a `Ref:` to a file in `application_documentation/` or `conductor/tracks/`.

**Decision Tree:**
1. **Is there a relevant doc in `application_documentation/`?**
   - Search: `find application_documentation -name "*.md" | xargs grep -l "<feature-keyword>"`
   - If found → Use it as the Ref
2. **Is this tracked work?**
   - Check `conductor/tracks.md` for active tracks
   - If working on a track → Use `conductor/tracks/TRACK-XXX-*.md` as Ref
3. **Neither exists?**
   - Create a new doc in `application_documentation/01-requirements/features/` or `application_documentation/07-quality/`

### Step 5: Atomic Commit Check
Verify all staged files belong to the same logical change:

```bash
git diff --cached --stat
```

**Red Flags:**
- Files from unrelated modules (e.g., `frontend/` AND `backend/dal/` without clear connection)
- Mix of feature code and unrelated refactoring
- Documentation for multiple unrelated features

**If mixed concerns:** Split into multiple commits using:
```bash
git reset HEAD <file>  # Unstage specific files
```

## 2. Quick Validation

**Trigger:** "Quick commit check"

Run all checks in one command:
```bash
# Secrets check
git diff --cached | grep -iE "(API_KEY|SECRET|PASSWORD|TOKEN|PRIVATE_KEY)" && echo "FAIL: Secrets detected" && exit 1

# Env file check
git diff --cached --name-only | grep -q "config/.env" && echo "FAIL: Env file staged" && exit 1

# Show staged files for review
echo "Staged files:"
git diff --cached --name-only
```

## 3. Validation Checklist

Before every commit, verify:

| Check | Command | Pass Criteria |
|:---|:---|:---|
| No secrets | `git diff --cached \| grep -iE "API_KEY\|SECRET"` | No matches |
| No .env files | `git diff --cached --name-only \| grep ".env"` | No matches |
| Doc reference exists | `ls application_documentation/...` | File exists |
| Single concern | `git diff --cached --stat` | Related files only |
| Accurate message | `git diff --cached` | Message matches diff |

## 4. Commit Message Template

Once validation passes, format the commit message:

```text
<Type>: <Short summary (50 chars max)>

Ref: application_documentation/<path>.md

- <Detail 1>
- <Detail 2>

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

**Types:**
- `Feature`: New functionality
- `Fix`: Bug fix
- `Refactor`: Code restructuring (no behavior change)
- `Docs`: Documentation only
- `Test`: Test additions/fixes
- `Chore`: Build, config, tooling

## 5. Automated Fix Actions

### Unstage Secrets File
```bash
git restore --staged <file-with-secrets>
```

### Unstage Environment Config
```bash
git restore --staged config/development.env
```

### Split Mixed Commit
```bash
# Unstage unrelated files
git reset HEAD <unrelated-file>

# Commit first changeset
git commit -m "First change..."

# Stage and commit second changeset
git add <unrelated-file>
git commit -m "Second change..."
```

## 6. Integration with Skills

This workflow enforces rules from:
- `.agent/skills/commit-policy/SKILL.md` - Ref requirement, security
- `.agent/skills/change-mapper/SKILL.md` - Atomic commits, no lazy `git add .`

When I help you commit, I automatically run these validations.
