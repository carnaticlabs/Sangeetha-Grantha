| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-30 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Design reference |

# Agent Workflows

---

> [!NOTE]
> Design/reference material: this page may include proposals or earlier implementation assumptions. Use [current operations guides](./README.md) for implemented behavior and current operating steps.

These procedures now live as skills under `.agents/skills/`. Each skill is a markdown file that defines a step-by-step procedure for a common development task, so an assistant can follow it the same way on each run.

## Available skills

| Skill | File | Purpose |
|----------|------|---------|
| Bulk Import Testing | `.agents/skills/bulk-import-testing/SKILL.md` | End-to-end testing of bulk import functionality |
| Conductor Track Manager | `.agents/skills/conductor-track-manager/SKILL.md` | Create and manage conductor tracks |
| E2E Test Runner | `.agents/skills/e2e-test-runner/SKILL.md` | Run and debug Playwright E2E tests |
| Pre-commit Validation | `.agents/skills/pre-commit-validation/SKILL.md` | Validate changes before committing |
| Scaffold Service | `.agents/skills/scaffold-service/SKILL.md` | Generate new service boilerplate |
| Test Troubleshooter | `.agents/skills/test-troubleshooter/SKILL.md` | Debug failing tests |

## Skill structure

Each skill follows a standard structure:

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

Skills can be invoked by:
1. Referencing the skill file path in conversation
2. Asking the assistant to follow `.agents/skills/<name>/SKILL.md`
3. Using trigger phrases defined in the skill's description

## Related Documentation

- [Commit Policy](../../.agents/skills/commit-policy/SKILL.md)
- [Change Mapper](../../.agents/skills/change-mapper/SKILL.md)
- [CLI Reference](./cli-docs-command.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
