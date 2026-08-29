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

# A secret env path as a STANDALONE token: not glued to a preceding word char or
# an opening paren (so `"Read(.env"` in a grep pattern is not a file arg) and not
# continued by another word char. Quotes, slashes, whitespace, `<`, `=` are fine
# as delimiters. This is what separates a real file argument from the path merely
# appearing inside a search pattern or a larger identifier.
_SECRET = (
    r"(?<![\w(])(?:\.env(?:\.[A-Za-z0-9_-]+)?|config/[\w.-]*\.env[\w.-]*)(?![\w])"
)
# Verbs that print/read/copy a file's contents to somewhere observable.
_READ_VERB = r"(?:cat|bat|tac|less|more|head|tail|nl|xxd|od|strings|tee|dd|source)"
# Search/stream verbs take a PATTERN then FILE(s); only the file position leaks.
_SEARCH_VERB = r"(?:grep|egrep|fgrep|rg|ag|ack|sed|awk|gawk)"

# A command "touches a secret" only when a read/dump/source verb, a redirection,
# or an open()-style call actually targets the secret path — not when the path
# merely appears as text (commit messages, PR bodies) or as a search PATTERN
# (`grep '.env' file`). Each pattern runs per shell segment so a pipe/;/&& to an
# unrelated command cannot smuggle the verb and the path together.
_DUMP_PATTERNS = [
    re.compile(r"<\s*" + _SECRET, re.IGNORECASE),                         # < .env
    re.compile(r"\b" + _READ_VERB + r"\b[^|;&\n]*" + _SECRET, re.IGNORECASE),  # cat .env
    re.compile(r"(?:^|[;&|]\s*)\.\s+" + _SECRET, re.IGNORECASE),          # . .env  (dot-source)
    re.compile(                                                          # open('.env'), load_dotenv('.env')
        r"(?:open|read_file|read_text|readFileSync|load_dotenv|dotenv|File\.read|Path)\s*\(\s*['\"][^'\"]*"
        + _SECRET,
        re.IGNORECASE,
    ),
    re.compile(                                                          # grep PATTERN .env (secret in file position)
        r"\b" + _SEARCH_VERB + r"\b\s+(?:-\S+\s+)*\S+\s+[^|;&\n]*" + _SECRET,
        re.IGNORECASE,
    ),
]


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
    return any(pattern.search(stripped) for pattern in _DUMP_PATTERNS)


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
