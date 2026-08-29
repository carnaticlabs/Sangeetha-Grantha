| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.1 |
| **Last Updated** | 2026-08-29 |
| **Author** | Sangeetha Grantha Team |

# AI-native SDLC artifacts (TRACK-134)

Close the gaps around agent Build speed: committed Intent / Spec / Plan on conductor tracks, review policy, session verification, edit-time hooks, and deterministic evals of the files that steer agents.

Conductor remains the source of truth. There is no parallel `intent/` directory.

## Artifacts

| Artifact | Location |
|:---|:---|
| Intent / Spec / Plan template | `.agents/workflows/conductor-track-manager.md` |
| `/spec-from-track` | `.claude/commands/spec-from-track.md` (Cursor symlink under `.cursor/commands/`) |
| `/plan-from-spec` | `.claude/commands/plan-from-spec.md` |
| Review policy | `REVIEW.md` |
| Session verification | `CLAUDE.md` → Verifying your work |
| Edit-time hooks | `.claude/hooks/*.py` + `.claude/settings.json` (Claude Code) and `.cursor/hooks.json` (Cursor). Same scripts; Cursor also gets JSON `{ "permission": "deny" }`. |
| Agent-config evals | `evals/check.py`, `evals/cases/`, `make agent-evals`, CI job `agent-evals` |

## Gates

1. Intent Status **Accepted** (human) before `/spec-from-track`.
2. Spec Status **Accepted** (human) before `/plan-from-spec`. Import/seed/lyric specs also get a `carnatic-musicologist` report.
3. Plan Status **Accepted** (human) before product-code edits.
4. PR review uses `REVIEW.md`. Findings inform; a human still approves.
5. Committed `database/migrations/V*.sql` files are immutable at edit time (Write/Edit/StrReplace/Delete); new `VNN__*.sql` and `R__*.sql` are allowed. Reads of `V__` files stay allowed. `.env` reads and shell dumps that actually target a secret file (`cat`/`head`/`source`/`< .env` redirection, `open('.env')`, `grep PATTERN .env`, …) are denied; the path merely appearing as text (commit messages, PR bodies) or as a search *pattern* (`grep 'Read(.env' file`) is allowed. Committed env templates (`.env.example`, `*.env.example`, `.env.*.example` such as `config/.env.auto-approval.example`) stay readable.
6. Committed *test* files (`*Test.kt`, `*.test.ts`, `*.spec.ts`, `test_*.py`) are immutable at edit time. Fixtures, Vitest `setup.ts`, and test helpers are not tests. `SANGITA_ALLOW_TEST_EDITS=1` (or `true`/`yes`) overrides; `0`/`false` do not.

## Evals

`evals/check.py` is deterministic (no model API). It asserts required policy strings still exist in `CLAUDE.md`, skills, commands, `REVIEW.md`, `.cursor/hooks.json`, and `.gitignore`, and smokes the edit-time hooks (including Cursor payload shapes and secret-file shell bypasses). LLM-in-CI evals from the playbook stay deferred until TRACK-109 budget/secrets work.

## Proof

```bash
python3 evals/check.py
python3 conductor/check-registry-sync.py
```

Ref: [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md), [Domain Model §6](../01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana).
