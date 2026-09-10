| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Steel-thread verification

---

Use the steel-thread scenario to verify a representative application path across service and persistence boundaries. It supplements focused tests and a real import's source/data checks.

## Run

```bash
make steel-thread
```

The Make target invokes API tests matching `*SteelThread*`. Database-backed cases use the repository's test substrate; Docker must be available where required. Do not pre-reset the development corpus to prepare tests.

## Interpret the result

Record the command, revision and outcome. If it fails, distinguish toolchain/container/migration failures from an application assertion failure. Preserve the failing check; fix the underlying cause rather than weakening tests.

For a real import, continue with [post-import verification](../../07-quality/qa/test-plan.md): identity, raga junctions, lyric variants/sections, provenance, workflow state, and client reads. A passing isolated scenario does not certify the whole corpus.

[Integration testing](../../07-quality/integration-tests-approach.md) · [Quality gates](../../07-quality/README.md) · [Historical steel-thread report](../../07-quality/reports/steel-thread.md)

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
