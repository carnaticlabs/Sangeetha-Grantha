#!/usr/bin/env python3
"""Block Read/Write/Edit of secret env files. Exit 2 denies the tool."""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _payload import bash_command, file_path, load_payload  # noqa: E402

_ENV_FILE = re.compile(
    r"(^|/)("
    r"\.env([^/]*)"
    r"|config/[^/]*\.env"
    r"|config/local\.env"
    r"|config/development\.env"
    r"|config/postgres-local\.env"
    r")$",
    re.IGNORECASE,
)
_EXAMPLE_OK = re.compile(r"\.env\.example$", re.IGNORECASE)
_BASH_SECRET = re.compile(
    r"(^|[;&|]\s*)(cat|less|more|head|tail|bat|sed|awk)\s+[^\n]*(\.env\b|config/[^ \n]*\.env)",
    re.IGNORECASE,
)


def _is_secret(path: str) -> bool:
    normalized = path.replace("\\", "/").strip()
    if not normalized or _EXAMPLE_OK.search(normalized):
        return False
    return bool(_ENV_FILE.search(normalized))


def main() -> int:
    payload = load_payload()
    path = file_path(payload)
    if _is_secret(path):
        print(
            "ERROR: Refusing to read or write env/secret files "
            f"({path}). Use gitignored config/local.env outside the agent session.",
            file=sys.stderr,
        )
        return 2

    cmd = bash_command(payload)
    if cmd and _BASH_SECRET.search(cmd) and ".env.example" not in cmd:
        print(
            "ERROR: Refusing a shell command that would dump an env/secret file.",
            file=sys.stderr,
        )
        return 2

    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001 — hooks must never crash the session
        print(f"WARN: deny-secrets hook error ({exc}); allowing", file=sys.stderr)
        sys.exit(0)
