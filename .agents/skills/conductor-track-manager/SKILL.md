---
name: conductor-track-manager
description: Manages the full lifecycle of Conductor tracks (create, update progress, close) ensuring strict phase-gated execution (Intent -> Spec -> Plan -> Implementation) and registry synchronization in conductor/tracks.md. Use when starting a new track, logging progress, or completing a feature track.
---

# Conductor Track Manager

This skill governs the lifecycle of Conductor feature tracks in Sangita Grantha, enforcing strict traceability and phase-gating.

## Strict Human-Gated Workflow

Do not skip these mandatory human gates:

1. **Write Intent** (in originator language). Stop until **Intent Status is Accepted**.
2. **Run `/spec-from-track`**. Stop until **Spec Status is Accepted**.
3. **Run `/plan-from-spec`**. Stop until **Plan Status is Accepted**.
4. **Implement**: Do not implement until Plan Status is Accepted. If implementation diverges, update the **Plan** in the same change.

> [!IMPORTANT]
> Do not create a separate `intent/` folder. The track file (`conductor/tracks/TRACK-<ID>-<slug>.md`) is the canonical single source of truth.

---

## 1. Create a New Track

**Trigger phrases**: "Start a new track for [Feature]", "Create TRACK-XXX", "Scaffold track for [Task]"

### Procedure:
1. **Determine Track ID**:
   - Inspect `conductor/tracks.md` to find the highest allocated ID.
   - Increment the number (e.g. if highest is `TRACK-140`, next is `TRACK-141`).
2. **Scaffold Track File**:
   - Create `conductor/tracks/TRACK-<ID>-<slug>.md`.
   - Use the standard template below.
3. **Update Registry**:
   - Append a row to the table in `conductor/tracks.md`.

### Track Template:

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
<One sentence description of the goal>

## Context
- **Reference:** <Links to docs, architecture, or related tracks>

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
<Non-negotiables: Flyway, audit log, lakshana, no secrets.>

### Open questions
<Unresolved questions. Carry into Spec if still open.>

## Spec
**Status:** Draft
**Accepted by:** —
**Accepted at:** —

### Requirements
<Populated by /spec-from-track after Intent is Accepted.>

### Design
<How it fits into the existing codebase architecture.>

### Flagged concerns
<Policy conflicts: Flyway, audit/auth, lakshana, security.>

### Open questions carried forward
<From Intent, answered or still open.>

## Plan
**Status:** Draft
**Accepted by:** —
**Accepted at:** —

### Files that change
<Populated by /plan-from-spec after Spec is Accepted.>

### Order of work
1. <Step 1>

### Risks
<What the change could break.>

### Proof
<Commands and tests that prove completion.>

## Implementation Plan
- [ ] <Task 1>
- [ ] <Task 2>

## Progress Log
- **YYYY-MM-DD**: Track created.
```

---

## 2. Update Track Progress

**Trigger phrases**: "Update track [ID] with [Progress]", "Log progress on TRACK-XXX"

### Procedure:
1. Locate `conductor/tracks/TRACK-<ID>-*.md`.
2. Append a bullet to the `## Progress Log` section:
   `- **YYYY-MM-DD**: <Summary of progress and completed tasks>`
3. Update `**Last Updated**` in the top metadata table and `**Updated:**` in the body.

---

## 3. Close Track

**Trigger phrases**: "Close track [ID]", "Mark track [ID] as complete"

### Procedure:
1. Locate `conductor/tracks/TRACK-<ID>-*.md`.
2. Set `**Status**` to `Completed` in both the metadata table and header.
3. Add final entry to `## Progress Log`:
   `- **YYYY-MM-DD**: Track completed.`
4. Update the corresponding row in `conductor/tracks.md` to `Completed`.
