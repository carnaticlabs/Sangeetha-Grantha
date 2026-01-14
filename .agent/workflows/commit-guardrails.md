---
description: Standard workflow for committing changes with documentation guardrails enforcement
---

This workflow guides you through the process of committing code while adhering to the project's strict documentation guardrails (One Ref Per Commit).

## 1. Check Status and Identify Scope
First, identify what files have changed to determine the scope of the commit.
```bash
git status
```

## 2. Identify or Create Reference Document
Every commit MUST reference a single documentation file in `application_documentation/`.

**Decision Point:**
- **IF** this is a new feature or comprehensive change:
  - Create a new file in `application_documentation/01-requirements/features/` (e.g., `my-feature-update.md`).
  - Document the requirements or changes in that file.
- **IF** this is a fix or update to an existing feature:
  - Identify the existing documentation file (e.g., `application_documentation/01-requirements/features/existing-feature.md`).
  - Update exactly that file to reflect the new state of the code.

**Action:**
Ensure the reference file exists and has been added to the git stage.
```bash
git add application_documentation/
```

## 3. Prepare Commit Message
Format the commit message using the required template.
Template:
```text
<Short content summary>

Ref: <relative_path_to_documentation_file>

- <Detailed point 1>
- <Detailed point 2>
```

**Example:**
> Implement User Login
>
> Ref: application_documentation/01-requirements/features/auth-flow.md
>
> - Added login API endpoint
> - Updated user schema

## 4. Sensitive Data Check (Pre-Commit Manual Check)
Before committing, ensure no sensitive data (API keys) matches strict patterns (even in docs).
- Replace `API_KEY` literals with `AUTH_TOKEN` or generic placeholders if they exist in documentation updates.
- Run a grep check if unsure:
```bash
grep -r "API_KEY" . || true
```

## 5. Exclude Local Environment Config
**CRITICAL:** If you have modified `config/.env.development` (e.g., to update `API_KEY` values for local testing), you **MUST** exclude it from the commit to prevent leaking secrets.

```bash
git restore --staged config/.env.development
```

## 6. Commit and Push
Execute the commit. Note: `Ref:` tag is case-insensitive but must point to a real file.

```bash
git commit -m "<Your Message>"
```

If the commit succeeds (hooks pass), push the changes.
```bash
git push
```
