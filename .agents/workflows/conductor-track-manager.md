---
description: Manages the lifecycle of Conductor tracks (create, update, close) to streamline project management and documentation.
---

# Conductor Track Manager

This workflow automates the creation, updating, and closing of Conductor tracks, ensuring consistency and saving time.

Human gates (do not skip):

1. Write **Intent** (originator language). Stop until **Intent Status is Accepted**.
2. Run `/spec-from-track`. Stop until **Spec Status is Accepted**.
3. Run `/plan-from-spec`. Stop until **Plan Status is Accepted**.
4. Do not implement until Plan Status is Accepted. If implementation diverges, update **Plan** in the same change.

Do not create a parallel `intent/` directory. The track file is the source of truth.

## 1. Create New Track

**Trigger:** "Start a new track for [Feature Name]" or "Create TRACK-XXX"

1.  **Determine Track ID:**
    - Read `conductor/tracks.md` to find the next available ID (e.g., if the last is TRACK-034, use TRACK-035).
    - If the user provided an ID, verify it doesn't exist.

2.  **Scaffold Track File:**
    - Create `conductor/tracks/TRACK-<ID>-<slug>.md`.
    - Use the following template:

```markdown
| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | YYYY-MM-DD |
| **Author** | Sangita Grantha Team |

# Track: <Feature Name>
**ID:** TRACK-<ID>
**Status:** In Progress
**Owner:** <User/Role>
**Created:** YYYY-MM-DD
**Updated:** YYYY-MM-DD

## Goal
<Description of the goal>

## Context
- **Reference:** <Links to other docs>

## Intent
**Status:** Draft
**Accepted by:** —
**Accepted at:** —

### Problem
<What is broken or missing, in the originator's words.>

### Proposed outcome
<What better looks like.>

### Affected users and systems
<People and modules/services.>

### Constraints
<Non-negotiables: Flyway, audit, lakshana, no prod deploy, …>

### Open questions
<Unresolved questions. Carry into Spec if still open.>

## Spec
**Status:** Draft
**Accepted by:** —
**Accepted at:** —

### Requirements
<Filled by /spec-from-track after Intent is Accepted.>

### Design
<How it fits the existing codebase.>

### Flagged concerns
<Policy conflicts: Flyway, audit/auth, lakshana, secrets. Route to the user.>

### Open questions carried forward
<From Intent, answered or still open.>

## Plan
**Status:** Draft
**Accepted by:** —
**Accepted at:** —

### Files that change
<Filled by /plan-from-spec after Spec is Accepted.>

### Order of work
1. <Step>

### Risks
<What the change could break.>

### Proof
<Commands/tests that prove it. See CLAUDE.md Verifying your work.>

## Implementation Plan
- [ ] <Task 1>
- [ ] <Task 2>

## Progress Log
- **YYYY-MM-DD**: Track created.
```

3.  **Update Registry:**
    - Append the new track to the table in `conductor/tracks.md`.

4.  **Do not implement** until Plan Status is Accepted. Use `/spec-from-track` then `/plan-from-spec`.

## 2. Update Track Progress

**Trigger:** "Update track [ID] with [Progress]"

1.  **Locate File:** Find `conductor/tracks/TRACK-<ID>-*.md`.
2.  **Append Log:** Add a new entry to the `## Progress Log` section:
    - `- **YYYY-MM-DD**: <Progress Details>`
3.  **Update Metadata:** Update the `**Last Updated**` field in the header table and the `**Updated:**` field in the body.

## 3. Close Track

**Trigger:** "Close track [ID]" or "Mark track [ID] as complete"

1.  **Locate File:** Find `conductor/tracks/TRACK-<ID>-*.md`.
2.  **Update Status:** Change `**Status**` to `Completed` in both the metadata table and the body.
3.  **Finalize Log:** Add a final log entry:
    - `- **YYYY-MM-DD**: Track completed.`
4.  **Update Registry:** Update the status in `conductor/tracks.md` to `Completed`.
