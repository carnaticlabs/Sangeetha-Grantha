#!/usr/bin/env python3
"""Block git commits that add a Cursor Co-authored-by trailer.

Cursor auto-commits inject `--trailer "Co-authored-by: Cursor <cursoragent@cursor.com>"`
when Agent attribution is on. This repo's commits stay under the author's name only.

Exit 2 denies the tool.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _payload import bash_command, deny, load_payload  # noqa: E402

_GIT_COMMIT = re.compile(r"\bgit\s+commit\b", re.IGNORECASE)
_TRAILER_ARG = re.compile(
    r"""--trailer\s+(?:"[^"]*"|'[^']*')""",
    re.IGNORECASE,
)
_CURSOR_IN_MESSAGE = re.compile(
    r"Co-authored-by:\s*Cursor\b"
    r"|Made-with:\s*Cursor\b"
    r"|cursoragent@cursor\.com",
    re.IGNORECASE,
)


def _is_git_commit(cmd: str) -> bool:
    return bool(_GIT_COMMIT.search(cmd))


def _message_has_cursor_trailer(cmd: str) -> bool:
    """True when the agent put a Cursor trailer in -m/HEREDOC, not via --trailer."""
    without_flags = _TRAILER_ARG.sub("", cmd)
    return bool(_CURSOR_IN_MESSAGE.search(without_flags))


def main() -> int:
    payload = load_payload()
    cmd = bash_command(payload)
    if not cmd or not _is_git_commit(cmd):
        return 0
    if _message_has_cursor_trailer(cmd):
        return deny(
            "ERROR: Do not add Co-authored-by: Cursor to the commit message. "
            "Commits stay under the author's name only.",
            payload,
        )
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001 — hooks must never crash the session
        print(f"WARN: deny-cursor-coauthor hook error ({exc}); allowing", file=sys.stderr)
        sys.exit(0)
