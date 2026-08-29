#!/usr/bin/env python3
"""Protect Flyway migrations at edit time. Exit 2 denies the tool."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _payload import deny, file_path, is_mutating, load_payload  # noqa: E402

_MIGRATIONS = re.compile(r"(^|/)database/migrations/([^/]+)$")
_VERSIONED = re.compile(r"^V\d{2,}__.+\.sql$", re.IGNORECASE)
_REPEATABLE = re.compile(r"^R__.+\.sql$", re.IGNORECASE)


def _repo_root() -> Path:
    start = Path(__file__).resolve()
    for parent in start.parents:
        if (parent / ".git").exists() or (parent / "CLAUDE.md").exists():
            return parent
    return Path.cwd()


def _tracked(root: Path, rel: str) -> bool:
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
    payload = load_payload()
    if not is_mutating(payload):
        return 0

    path = file_path(payload)
    if not path:
        return 0

    normalized = path.replace("\\", "/")
    match = _MIGRATIONS.search(normalized)
    if not match:
        return 0

    name = match.group(2)
    rel = f"database/migrations/{name}"
    root = _repo_root()

    if name.lower().endswith((".xml", ".yml", ".yaml")):
        return deny(
            "ERROR: Liquibase/changelog files are not allowed. "
            "Flyway only: database/migrations/VNN__description.sql or R__*.sql "
            "(ADR-013). Use /new-migration.",
            payload,
        )

    if _REPEATABLE.match(name):
        return 0

    if _VERSIONED.match(name):
        if _tracked(root, rel):
            return deny(
                f"ERROR: Committed versioned migration {rel} is immutable. "
                "Add a new VNN__*.sql (next free number) via /new-migration. "
                "Do not edit applied Flyway files.",
                payload,
            )
        return 0

    return deny(
        f"ERROR: {rel} is not a Flyway name. Use VNN__snake_or-kebab-description.sql "
        "or R__seed_NN_description.sql. See the postgres-flyway-db skill.",
        payload,
    )


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001
        print(f"WARN: protect-migrations hook error ({exc}); allowing", file=sys.stderr)
        sys.exit(0)
