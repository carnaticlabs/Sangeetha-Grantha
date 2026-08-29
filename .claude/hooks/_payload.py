#!/usr/bin/env python3
"""Parse PreToolUse hook JSON from Claude Code or Cursor."""

from __future__ import annotations

import json
import sys
from typing import Any

_MUTATING_TOOLS = frozenset(
    {
        "write",
        "edit",
        "strreplace",
        "delete",
        "editnotebook",
        "tabwrite",
    }
)


def load_payload() -> dict[str, Any]:
    raw = sys.stdin.read()
    if not raw.strip():
        return {}
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return {}
    return data if isinstance(data, dict) else {}


def _walk(obj: Any, *keys: str) -> Any:
    cur: Any = obj
    for key in keys:
        if not isinstance(cur, dict) or key not in cur:
            return None
        cur = cur[key]
    return cur


def is_cursor_payload(payload: dict[str, Any]) -> bool:
    if payload.get("hook_event_name") or payload.get("conversation_id"):
        return True
    if payload.get("cursor_version") or payload.get("workspace_roots"):
        return True
    return False


def tool_name(payload: dict[str, Any]) -> str:
    for path in (("tool_name",), ("toolName",), ("tool", "name")):
        value = _walk(payload, *path)
        if isinstance(value, str) and value:
            return value
    return ""


def tool_input(payload: dict[str, Any]) -> dict[str, Any]:
    for path in (("tool_input",), ("toolInput",), ("tool", "input"), ("input",), ("arguments",)):
        value = _walk(payload, *path)
        if isinstance(value, dict):
            return value
    return {}


def file_path(payload: dict[str, Any]) -> str:
    inp = tool_input(payload)
    for key in ("file_path", "filePath", "path", "target_notebook"):
        value = inp.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    for key in ("file_path", "filePath", "path", "target_notebook"):
        value = payload.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def bash_command(payload: dict[str, Any]) -> str:
    inp = tool_input(payload)
    value = inp.get("command")
    if isinstance(value, str) and value.strip():
        return value.strip()
    top = payload.get("command")
    return top.strip() if isinstance(top, str) else ""


def is_mutating(payload: dict[str, Any]) -> bool:
    """True for Write/Edit/StrReplace/Delete. Reads and shell dumps are not mutations."""
    event = str(payload.get("hook_event_name") or payload.get("event") or "")
    if event in {"beforeReadFile", "beforeTabFileRead", "beforeShellExecution"}:
        return False
    name = tool_name(payload).lower()
    if name in _MUTATING_TOOLS:
        return True
    if name in {"read", "grep", "glob", "semanticsearch", "tabread", "shell", "bash"}:
        return False
    # Unknown preToolUse with a path: treat as a write so StrReplace aliases cannot skip.
    if event.lower() == "pretooluse" and file_path(payload):
        return True
    return bool(name) and bool(file_path(payload))


def deny(message: str, payload: dict[str, Any] | None = None) -> int:
    """Claude Code: stderr + exit 2. Cursor: JSON permission deny + exit 2."""
    print(message, file=sys.stderr)
    if payload is not None and is_cursor_payload(payload):
        sys.stdout.write(
            json.dumps(
                {
                    "permission": "deny",
                    "agent_message": message,
                    "user_message": message,
                }
            )
            + "\n"
        )
    return 2
