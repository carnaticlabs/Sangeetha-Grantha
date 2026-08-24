#!/usr/bin/env python3
"""Deterministic agent-config evals (no model calls).

Cases live in evals/cases/*.json. Each case names a file and strings that
must (or must not) remain in agent configuration. Hook smoke tests live here
so a CLAUDE.md/skill edit cannot drop Flyway, audit, or secret-file guards
without CI noticing.

Exit 0 if all cases and hook smokes pass; 1 otherwise.
"""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = Path(__file__).resolve().parent / "cases"
HOOKS = ROOT / ".claude" / "hooks"


def _fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)


def run_cases() -> int:
    failures = 0
    files = sorted(CASES.glob("*.json"))
    if not files:
        _fail(f"no eval cases in {CASES}")
        return 1

    for case_path in files:
        case = json.loads(case_path.read_text(encoding="utf-8"))
        case_id = case.get("id", case_path.stem)
        rel = case["file"]
        target = ROOT / rel
        if not target.is_file():
            _fail(f"{case_id}: missing file {rel}")
            failures += 1
            continue
        text = target.read_text(encoding="utf-8")
        case_failed = False
        for needle in case.get("must_contain", []):
            if needle not in text:
                _fail(f"{case_id}: {rel} missing required text {needle!r}")
                failures += 1
                case_failed = True
        for needle in case.get("must_not_contain", []):
            if needle in text:
                _fail(f"{case_id}: {rel} contains forbidden text {needle!r}")
                failures += 1
                case_failed = True
        if not case_failed:
            print(f"ok  {case_id}")
    return failures


def _hook(script: str, payload: dict) -> int:
    proc = subprocess.run(
        [sys.executable, str(HOOKS / script)],
        input=json.dumps(payload),
        capture_output=True,
        text=True,
        cwd=ROOT,
        check=False,
    )
    return proc.returncode


def run_hook_smokes() -> int:
    failures = 0
    smokes = [
        (
            "deny-secrets.env-read",
            "deny-secrets.py",
            {"tool_name": "Read", "tool_input": {"file_path": ".env"}},
            2,
        ),
        (
            "deny-secrets.claude-ok",
            "deny-secrets.py",
            {"tool_name": "Read", "tool_input": {"file_path": "CLAUDE.md"}},
            0,
        ),
        (
            "deny-secrets.bash-cat",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "cat config/local.env"}},
            2,
        ),
        (
            "protect-migrations.committed-v",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "database/migrations/V01__baseline-schema-and-types.sql"
                },
            },
            2,
        ),
        (
            "protect-migrations.repeatable",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "database/migrations/R__seed_04_raga_reference.sql"
                },
            },
            0,
        ),
        (
            "protect-migrations.liquibase",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {"file_path": "database/migrations/changelog.xml"},
            },
            2,
        ),
        (
            "protect-migrations.unrelated",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {"file_path": "modules/backend/api/src/Foo.kt"},
            },
            0,
        ),
        (
            "protect-migrations.cursor-shape",
            "protect-migrations.py",
            {
                "tool": {
                    "name": "Write",
                    "input": {
                        "path": "database/migrations/V01__baseline-schema-and-types.sql"
                    },
                }
            },
            2,
        ),
        (
            "protect-tests.committed-kt",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {
                    "file_path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/KrithiServiceTest.kt"
                },
            },
            2,
        ),
        (
            "protect-tests.new-test-ok",
            "protect-tests.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/BrandNewFeatureTest.kt"
                },
            },
            0,
        ),
        (
            "protect-tests.non-test-ok",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {"file_path": "modules/backend/api/src/Foo.kt"},
            },
            0,
        ),
        (
            "protect-tests.cursor-shape",
            "protect-tests.py",
            {
                "tool": {
                    "name": "Edit",
                    "input": {
                        "path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/KrithiServiceTest.kt"
                    },
                }
            },
            2,
        ),
    ]
    for name, script, payload, expected in smokes:
        code = _hook(script, payload)
        if code != expected:
            _fail(f"hook {name}: expected exit {expected}, got {code}")
            failures += 1
        else:
            print(f"ok  hook:{name}")
    return failures


def main() -> int:
    failures = run_cases() + run_hook_smokes()
    if failures:
        print(f"{failures} agent-eval failure(s)", file=sys.stderr)
        return 1
    print("agent-evals passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
