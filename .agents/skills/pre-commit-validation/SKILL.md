---
name: pre-commit-validation
description: Validates staged changes against commit policy before committing, scanning for secrets, blocking environment files, checking atomic boundaries, and enforcing documentation Ref links. Use before committing any staged files to prevent policy violations.
---

# Pre-Commit Validation

This skill automates validation of staged git changes against project commit policies, security guardrails, and documentation traceability requirements.

## 1. Comprehensive Validation Sequence

Run these checks whenever preparing a commit:

### Step 1: Capture Staged Files
```bash
git diff --cached --name-only
```

### Step 2: Secret & Sensitive Pattern Scan
Detect common credentials or high-entropy tokens:
```bash
git diff --cached | grep -iE "(API_KEY|SECRET|PASSWORD|TOKEN|PRIVATE_KEY)" || echo "No secrets detected"
```
> [!CAUTION]
> If any secrets or sensitive patterns are detected, immediately unstage the file!

### Step 3: Block Local Environment Files
Ensure no local config files (`config/*.env`, `config/local.env`, `config/development.env`) are staged:
```bash
git diff --cached --name-only | grep -E "config/.*\.env" && echo "ERROR: Env file staged!" || echo "OK: No env files staged"
```
**Remediation**:
```bash
git restore --staged config/development.env config/local.env 2>/dev/null || true
```

### Step 4: Documentation Reference (`Ref:`) Verification
Every commit must link to a valid documentation file in `application_documentation/` (or `conductor/tracks/` as fallback):
1. **Implementation work**: Link to `application_documentation/10-implementations/track-<nnn>-<slug>.md`
2. **Architecture / Schema**: Link to `application_documentation/04-database/` or `02-architecture/`
3. Verify file exists on disk before committing!

### Step 5: Atomic Changeset Verification
Ensure staged files belong strictly to a single logical feature or track:
```bash
git diff --cached --stat
```
**Red Flags**:
- Touching unrelated modules (e.g. `modules/frontend/` and `tools/krithi-extract-enrich-worker/` in one commit unless tightly coupled to a single track contract).
- Mixing feature implementation with unrelated cosmetic refactorings.

---

## 2. One-Liner Quick Check

```bash
git diff --cached | grep -iE "(API_KEY|SECRET|PASSWORD|TOKEN|PRIVATE_KEY)" && echo "FAIL: Secrets detected" && exit 1
git diff --cached --name-only | grep -E "config/.*\.env" && echo "FAIL: Env file staged" && exit 1
echo "Staged files:" && git diff --cached --name-only
```

---

## 3. Commit Message Checklist & Format

Formatted commit message structure:

```text
<TRACK-ID>: <Short summary (50 chars max)>

Ref: application_documentation/<path-to-doc>.md

- <Key change 1>
- <Key change 2>
```

If no track exists (e.g., small chore or doc fix):
```text
<Type>: <Short summary>

Ref: application_documentation/<path-to-doc>.md

- <Key change>
```

---

## 4. Post-Validation Delivery

Once validation passes and changes are committed per changeset:
- Refer to [.agents/skills/retrospective-commit-and-push/SKILL.md](../retrospective-commit-and-push/SKILL.md) and [.agents/skills/commit-policy/SKILL.md](../commit-policy/SKILL.md) to complete the full delivery lifecycle:
  1. **Push**: `git push -u origin HEAD`
  2. **Watch PR**: `gh pr checks --watch`
  3. **Merge**: `gh pr merge --merge --delete-branch`
  4. **Sync Local**: `git checkout main && git pull --ff-only origin main`
  5. **Clean Stale Branches**: Safe delete merged local branches (`git branch -d`) and delete stale remote branches.
