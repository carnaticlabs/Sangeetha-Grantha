| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Agent Workflows

This document describes the automated workflows available in `.agent/workflows/` for use with Claude Code and other AI assistants.

## Overview

Agent workflows are structured markdown files that define step-by-step procedures for common development tasks. They serve as executable documentation that AI assistants can follow to perform complex, multi-step operations consistently.

## Available Workflows

| Workflow | File | Purpose |
|----------|------|---------|
| Bulk Import Testing | `bulk-import-testing.md` | End-to-end testing of bulk import functionality |
| Conductor Track Manager | `conductor-track-manager.md` | Create and manage conductor tracks |
| E2E Test Runner | `e2e-test-runner.md` | Run and debug Playwright E2E tests |
| Pre-commit Validation | `pre-commit-validation.md` | Validate changes before committing |
| Scaffold Service | `scaffold-service.md` | Generate new service boilerplate |
| Test Troubleshooter | `test-troubleshooter.md` | Debug failing tests |

## Workflow Structure

Each workflow follows a standard structure:

```markdown
---
description: Brief description of what the workflow does
---

# Workflow Name

## 1. Step One
Instructions and commands...

## 2. Step Two
Instructions and commands...
```

## Usage

Workflows can be invoked by:
1. Referencing the workflow file path in conversation
2. Asking the AI assistant to "follow the workflow in `.agent/workflows/<name>.md`"
3. Using trigger phrases defined in the workflow's description

## Related Documentation

- [Commit Policy](.agent/skills/commit-policy/SKILL.md)
- [Change Mapper](.agent/skills/change-mapper/SKILL.md)
- [CLI Reference](./cli-docs-command.md)