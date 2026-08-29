#!/usr/bin/env python3
"""Protect committed test files at edit time (AI-native SDLC Stage 4).

Playbook rule: while fixing a bug, the agent must not edit tests to make them
pass — it reproduces the failure, then fixes the code. Creating a *new* test
(the test-first step) stays allowed; editing an already-committed *test* file
is blocked. Fixtures, Vitest setup, and test helpers are not tests.

Override: SANGITA_ALLOW_TEST_EDITS=1 (or true/yes). Any other value, including
0/false, does not opt out.

Exit 2 denies the tool.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _payload import deny, file_path, is_mutating, load_payload  # noqa: E402

# Filename patterns only — do not treat everything under src/test/ as a test
# (fixtures, setup.ts, testsupport helpers stay editable).
_TEST_FILE = re.compile(
    r"("
    r".*Test\.kt"          # Kotlin: FooTest.kt
    r"|.*\.test\.[tj]sx?"   # TS/JS: foo.test.ts / .tsx / .js
    r"|.*\.spec\.[tj]sx?"   # TS/JS + Playwright: foo.spec.ts
    r"|test_.*\.py"         # Python: test_foo.py
    r"|.*_test\.py"         # Python: foo_test.py
    r")$",
    re.IGNORECASE,
)
_ALLOW = re.compile(r"^(1|true|yes)$", re.IGNORECASE)


def _is_test(path: str) -> bool:
    name = path.replace("\\", "/").rsplit("/", 1)[-1]
    return bool(_TEST_FILE.match(name))


def _edits_allowed() -> bool:
    raw = os.environ.get("SANGITA_ALLOW_TEST_EDITS", "").strip()
    return bool(_ALLOW.match(raw))


def _repo_root() -> Path:
    start = Path(__file__).resolve()
    for parent in start.parents:
        if (parent / ".git").exists() or (parent / "CLAUDE.md").exists():
            return parent
    return Path.cwd()


def _tracked(root: Path, path: str) -> bool:
    rel = path
    try:
        abspath = Path(path)
        if abspath.is_absolute():
            rel = str(abspath.relative_to(root))
    except (ValueError, OSError):
        rel = path
    try:
        result = subprocess.run(
            ["git", "ls-files", "--error-unmatch", rel],
            cwd=root,
            capture_output=True,
            text=True,
            check=False,
        )
    except OSError:
        return False
    return result.returncode == 0


def main() -> int:
    if _edits_allowed():
        return 0

    payload = load_payload()
    if not is_mutating(payload):
        return 0

    path = file_path(payload)
    if not path or not _is_test(path):
        return 0

    root = _repo_root()
    if _tracked(root, path):
        return deny(
            f"ERROR: {path} is a committed test file. During a bug fix, reproduce "
            "the failure and fix the code — do not edit the test to make it pass "
            "(CLAUDE.md: fix the code, not the test). To change a test on purpose, "
            "rerun with SANGITA_ALLOW_TEST_EDITS=1 (true/yes also work; 0/false do not).",
            payload,
        )

    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001 — hooks must never crash the session
        print(f"WARN: protect-tests hook error ({exc}); allowing", file=sys.stderr)
        sys.exit(0)
