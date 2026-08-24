| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-08-24 |
| **Author** | Sangeetha Grantha Team |

# Review instructions

Agent review informs merge; it does not approve. Branch protection (when enabled) still requires a human code owner. Findings do not merge on their own.

## Passes

Run three passes and tag each finding with its pass:

- **Bugs:** logic errors, broken edge cases, silent data loss, regressions in neighboring flows
- **Security:** missing auth/`requireRole`, audit gaps on mutations, PII or secrets in logs, `.env` or credentials in the diff
- **Compliance:** the change matches the track **Spec** and **Plan** (and `CLAUDE.md`); Flyway-only migrations (ADR-013); `DatabaseFactory.dbQuery` + DTOs (no Exposed entities); junction tables populated; lakshana in [Domain Model §6](application_documentation/01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana) for composition data

## What Important means here

Reserve Important for findings that would break behavior, leak data, breach a policy (Flyway, audit, auth), or publish musicologically wrong catalog data. Style and naming are nits.

## Cap the nits

Report at most five nits per review; summarize the rest as a count.

## Do not report

Generated build output, lockfile-only noise, and anything CI already enforces (`make test`, Flyway validate, ruff/mypy, `make agent-evals`) unless the diff disables those checks.

## Feedback into CLAUDE.md

If a review flags the same agent mistake twice, add it to **Things agents get wrong** in `CLAUDE.md` as part of the review fix — not as a separate cleanup later.
