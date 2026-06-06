---
description: Rewrites an informal user request into a structured, tool-friendly prompt (goal, scope, paths, steps, deliverables, verification). Use when preparing work for agentic models (Cursor Composer, Claude, GPT, etc.) or before large multi-step tasks.
---

# Agentic Prompt Optimizer (workflow)

**Purpose:** Convert a rough sentence into one message an agent can run with tools (read/search/edit/terminal) instead of open-ended chat.

**Canonical instructions:** [.cursor/skills/agentic-prompt-optimizer/SKILL.md](../../.cursor/skills/agentic-prompt-optimizer/SKILL.md)

## Quick trigger phrases

- "Optimize this prompt for an agent"
- "Turn this into an agentic task"
- "Rewrite for Composer / Claude / GPT with tools"

## Minimal process

1. State **goal** and **definition of done** in one line each.
2. List **inputs** (paths) and **out of scope**.
3. Order **steps** with checkable outcomes; note what can run **in parallel**.
4. Specify **deliverables** (files, formats) and **verification** (commands/tests).

If the full skill is available, follow its template and examples; otherwise apply the minimal process above.
