---
description: Generates a clear, structured prompt for a given workflow and specific conductor tracks, ready to be copy-pasted to another AI session or agent.
---

# Generate Prompt Workflow

This workflow creates a highly optimized, context-aware prompt that the user can copy and paste into a new AI session or agent context. It ensures the receiving agent strictly follows the requested workflow and focuses only on the specific tracks provided.

## How to use this workflow

When the user asks you to run `/generate-prompt` (or similar requests specifying a workflow and tracks), follow these steps:

### 1. Identify Inputs
Determine the **Target Workflow** (e.g., `/retrospective-commit-and-push`) and the **Target Tracks** (e.g., `TRACK-093`, `TRACK-097`) from the user's request.

### 2. Read the Target Workflow (If Necessary)
If you do not already know the exact steps of the requested workflow, use `view_file` to read its definition in `.agent/workflows/`. This ensures the `Execution Requirements` you generate will accurately reflect the workflow's actual rules.

### 3. Generate the Prompt
Output a structured prompt inside a markdown code block so the user can easily copy it. Use the template below.

---

### Output Template

```text
Please execute the [WORKFLOW_NAME] workflow to address the pending changes for the following active tracks:

[LIST_OF_TRACKS_FORMATTED_WITH_MENTIONS]

Execution Requirements:

1. **Scope & Isolation**: Analyze all modified and untracked files. Isolate and group the files strictly into [NUMBER_OF_TRACKS] changesets corresponding to [TRACK_IDS]. Leave any unrelated files unstaged.
2. **Documentation Sync**: Update the progress logs and statuses in the listed Conductor track files. Ensure the corresponding implementation summary documents in `application_documentation/10-implementations/` are created or updated. These will serve as the mandatory `Ref:` targets for the commits.
3. **Workflow Rules ([WORKFLOW_NAME])**: Follow the exact steps defined in the [WORKFLOW_NAME] workflow. [Add 1-2 sentences summarizing the core rules from the workflow, e.g., "Create separate, atomic commits for each track. Adhere strictly to the project's commit policy."].
4. **Security Guardrails**: Mask any sensitive data in staged docs and ensure local configurations (`config/development.env`, `config/local.env`) are completely excluded.
5. **Finalization**: [e.g., Once successfully committed locally, push the changes to origin main.]
```

### Example

If the user asks: "Generate a prompt for /retrospective-commit-and-push on TRACK-093 and TRACK-097", your generated text should look like this:

```text
Please execute the /retrospective-commit-and-push workflow to organize, commit, and push the pending changes for the following active tracks:

@[conductor/tracks/TRACK-093-trinity-krithi-bulk-import.md]
@[conductor/tracks/TRACK-097-guru-guha-blog-source-adapter.md]

Execution Requirements:

1. **Scan & Categorize**: Analyze all modified and untracked files. Isolate and group the files strictly into two changesets corresponding to TRACK-093 and TRACK-097. Leave any unrelated files unstaged.
2. **Documentation Sync**: Update the progress logs and statuses in both Conductor track files. Ensure the corresponding implementation summary documents in application_documentation/10-implementations/ are created or updated. These will serve as the mandatory Ref: targets for the commits.
3. **Atomic Commits**: Create separate, atomic commits for each track. Adhere strictly to the project's commit policy (e.g., standard metadata tables, no git commit -a, and formatting the message as <TRACK-ID>: <Short Summary> with the required Ref: application_documentation/... line).
4. **Security Guardrails**: Mask any sensitive data in staged docs and ensure local configurations (config/development.env, config/local.env) are completely excluded.
5. **Push**: Once successfully committed locally, push the changes to origin main.
```
