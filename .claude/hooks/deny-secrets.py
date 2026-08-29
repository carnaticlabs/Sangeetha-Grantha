#!/usr/bin/env python3
"""Block Read/Write/Edit of secret env files. Exit 2 denies the tool."""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _payload import bash_command, deny, file_path, load_payload  # noqa: E402

_ENV_FILE = re.compile(
    r"(^|/)("
    r"\.env([^/]*)"
    r"|config/[^/]*\.env"
    r")$",
    re.IGNORECASE,
)
# Tracked templates: .env.example, config/.env.auto-approval.example, *.env.example
_EXAMPLE_OK = re.compile(
    r"\.env(?:\.[A-Za-z0-9_-]+)*\.example\b",
    re.IGNORECASE,
)
# After stripping example templates, any remaining .env or config/*.env path is a dump.
_SECRET_IN_CMD = re.compile(
    r"(?:^|[\s\"'`=<(])("
    r"\.env(?:\.[A-Za-z0-9_-]+)?"
    r"|config/[\w.-]*\.env[\w.-]*"
    r")",
    re.IGNORECASE,
)


def _is_example_template(path: str) -> bool:
    name = path.replace("\\", "/").rstrip("/").rsplit("/", 1)[-1]
    return name.lower().endswith(".example") and bool(_EXAMPLE_OK.search(name))


def _is_secret(path: str) -> bool:
    normalized = path.replace("\\", "/").strip()
    if not normalized or _is_example_template(normalized):
        return False
    return bool(_ENV_FILE.search(normalized))


def _command_touches_secret(cmd: str) -> bool:
    if not cmd:
        return False
    stripped = _EXAMPLE_OK.sub("", cmd)
    return bool(_SECRET_IN_CMD.search(stripped))


def main() -> int:
    payload = load_payload()
    path = file_path(payload)
    if _is_secret(path):
        return deny(
            "ERROR: Refusing to read or write env/secret files "
            f"({path}). Use committed *.env.example / .env.*.example "
            "templates and VITE_API_BASE_URL; never Read gitignored "
            ".env files.",
            payload,
        )

    cmd = bash_command(payload)
    if _command_touches_secret(cmd):
        return deny(
            "ERROR: Refusing a shell command that would dump an env/secret file.",
            payload,
        )

    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001 — hooks must never crash the session
        print(f"WARN: deny-secrets hook error ({exc}); allowing", file=sys.stderr)
        sys.exit(0)
