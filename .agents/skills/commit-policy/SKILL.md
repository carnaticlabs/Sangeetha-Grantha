| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# Commit Policy

You are responsible for ensuring that all changes committed to the repository adhere to strict traceability and security guardrails.

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

## 3. Workflow

When you are ready to commit changes for the user:
1.  **Categorize**: Group changes into logical changesets (see `change-mapper` skill).
2.  **Create/Update Docs**: Ensure an implementation doc exists in `application_documentation/10-implementations/`.
3.  **Create/Update Track**: Ensure a conductor track exists in `conductor/tracks/` and is registered in `conductor/tracks.md`.
4.  **Stage**: `git add <specific files>` — never `git add .` unless verified.
5.  **Draft Message**: Compose message with TRACK-ID, Ref, and bullet points matching the diff.
6.  **Execute**: `git commit -m "..."` (or ask user to approve).

**Example of a Good Commit:**
```bash
git commit -m "TRACK-080: Add curator review UI with section issue tracking

Ref: application_documentation/10-implementations/track-080-curator-review-ui.md

- CuratorRoutes.kt: GET /v1/admin/curator/stats, GET /v1/admin/curator/section-issues
- CuratorService.kt: stats aggregation, section issue detection per variant
- CuratorReviewPage.tsx: two-tab UI (Pending Matches, Section Issues)
- BulkImportTaskRepository.kt: fixed idempotency key to include jobType"
```
