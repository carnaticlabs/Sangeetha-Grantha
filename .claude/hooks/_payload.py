#!/usr/bin/env python3
"""Parse PreToolUse hook JSON from Claude Code or Cursor."""

from __future__ import annotations

import json
import sys
from typing import Any


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


def tool_name(payload: dict[str, Any]) -> str:
    for path in (("tool_name",), ("toolName",), ("tool", "name"), ("hook_event_name",)):
        value = _walk(payload, *path)
        if isinstance(value, str) and value:
            return value
    return ""


def tool_input(payload: dict[str, Any]) -> dict[str, Any]:
    for path in (("tool_input",), ("toolInput",), ("tool", "input"), ("input",)):
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
    return ""


def bash_command(payload: dict[str, Any]) -> str:
    inp = tool_input(payload)
    value = inp.get("command")
    return value.strip() if isinstance(value, str) else ""
