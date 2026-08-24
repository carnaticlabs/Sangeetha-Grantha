| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-08-24 |
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
| Edit-time hooks | `.claude/hooks/deny-secrets.py`, `.claude/hooks/protect-migrations.py`, `.claude/hooks/protect-tests.py`, `.claude/settings.json` |
| Agent-config evals | `evals/check.py`, `evals/cases/`, `make agent-evals`, CI job `agent-evals` |

## Gates

1. Intent Status **Accepted** (human) before `/spec-from-track`.
2. Spec Status **Accepted** (human) before `/plan-from-spec`. Import/seed/lyric specs also get a `carnatic-musicologist` report.
3. Plan Status **Accepted** (human) before product-code edits.
4. PR review uses `REVIEW.md`. Findings inform; a human still approves.
5. Committed `database/migrations/V*.sql` files are immutable at edit time; new `VNN__*.sql` and `R__*.sql` are allowed. `.env` reads are denied.
6. Committed test files are immutable at edit time (Stage 4: reproduce and fix the code during a bug fix, don't edit the test to pass). New test files are allowed; `SANGITA_ALLOW_TEST_EDITS=1` overrides for deliberate test changes.

## Evals

`evals/check.py` is deterministic (no model API). It asserts required policy strings still exist in `CLAUDE.md`, skills, commands, and `REVIEW.md`, and smokes the two edit-time hooks. LLM-in-CI evals from the playbook stay deferred until TRACK-109 budget/secrets work.

## Proof

```bash
python3 evals/check.py
python3 conductor/check-registry-sync.py
```

Ref: [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md), [Domain Model §6](../01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana).
