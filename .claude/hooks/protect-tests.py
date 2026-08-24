#!/usr/bin/env python3
"""Protect committed test files at edit time (AI-native SDLC Stage 4).

Playbook rule: while fixing a bug, the agent must not edit tests to make them
pass — it reproduces the failure, then fixes the code. Creating a *new* test
(the test-first step) stays allowed; editing an already-committed test is
blocked. Set SANGITA_ALLOW_TEST_EDITS=1 to override for a deliberate test change.

Exit 2 denies the tool.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _payload import file_path, load_payload  # noqa: E402

# A path is a test if it sits in a test source root or matches a test filename.
_TEST_DIR = re.compile(r"(^|/)(src/test/|tests?/|__tests__/)", re.IGNORECASE)
_TEST_FILE = re.compile(
    r"("
    r".*Test\.kt"          # Kotlin: FooTest.kt
    r"|.*\.test\.[tj]sx?"   # TS/JS: foo.test.ts / .tsx / .js
    r"|.*\.spec\.[tj]sx?"   # TS/JS: foo.spec.ts
    r"|test_.*\.py"         # Python: test_foo.py
    r"|.*_test\.py"         # Python: foo_test.py
    r")$",
    re.IGNORECASE,
)


def _is_test(path: str) -> bool:
    normalized = path.replace("\\", "/")
    name = normalized.rsplit("/", 1)[-1]
    if _TEST_FILE.match(name):
        return True
    return bool(_TEST_DIR.search(normalized) and name.lower().endswith(
        (".kt", ".kts", ".ts", ".tsx", ".js", ".jsx", ".py")
    ))


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
    if os.environ.get("SANGITA_ALLOW_TEST_EDITS"):
        return 0

    payload = load_payload()
    path = file_path(payload)
    if not path or not _is_test(path):
        return 0

    root = _repo_root()
    if _tracked(root, path):
        print(
            f"ERROR: {path} is a committed test file. During a bug fix, reproduce "
            "the failure and fix the code — do not edit the test to make it pass "
            "(CLAUDE.md: fix the code, not the test). To change a test on purpose, "
            "rerun with SANGITA_ALLOW_TEST_EDITS=1.",
            file=sys.stderr,
        )
        return 2

    # New, untracked test file — the test-first step is allowed.
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001 — hooks must never crash the session
        print(f"WARN: protect-tests hook error ({exc}); allowing", file=sys.stderr)
        sys.exit(0)
