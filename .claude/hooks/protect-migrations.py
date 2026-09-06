#!/usr/bin/env python3
"""Protect Flyway migrations at edit time. Exit 2 denies the tool."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _payload import deny, file_path, is_mutating, load_payload, tool_input, tool_name  # noqa: E402

_MIGRATIONS = re.compile(r"(^|/)database/migrations/([^/]+)$")
_VERSIONED = re.compile(r"^V\d{2,}__.+\.sql$", re.IGNORECASE)
_REPEATABLE = re.compile(r"^R__.+\.sql$", re.IGNORECASE)
_RETIRABLE_DATA_FIX = re.compile(
    r"^V(38|45|46|47|58|59|60|61|62)__.+\.sql$",
    re.IGNORECASE,
)
_CORPUS_TABLES = re.compile(
    r"\b(krithis|krithi_sections|krithi_lyric_variants|krithi_lyric_sections|"
    r"krithi_ragas|krithi_revisions)\b",
    re.IGNORECASE,
)
_DML = re.compile(r"\b(INSERT|UPDATE|DELETE)\b", re.IGNORECASE)
_DDL = re.compile(
    r"\b(CREATE|ALTER)\s+(OR\s+REPLACE\s+)?(UNIQUE\s+)?"
    r"(TABLE|INDEX|TYPE|FUNCTION|SCHEMA|SEQUENCE|VIEW)\b"
    r"|\bDROP\s+(TABLE|INDEX|TYPE|FUNCTION|SCHEMA|SEQUENCE|VIEW)\b",
    re.IGNORECASE,
)
_ALLOW = re.compile(r"--\s*corpus-data-fix:\s*allow\b", re.IGNORECASE)


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


def _sql_from_payload_or_disk(root: Path, rel: str, payload: dict) -> str:
    inp = tool_input(payload)
    for key in ("contents", "new_string", "newString"):
        value = inp.get(key)
        if isinstance(value, str) and value.strip():
            return value
    path = root / rel
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def _has_corpus_dml(sql: str) -> bool:
    return bool(_DML.search(sql) and _CORPUS_TABLES.search(sql))


def _is_corpus_only_data_fix(sql: str) -> bool:
    stripped = re.sub(r"CREATE\s+TEMP(ORARY)?\s+TABLE\b", "", sql, flags=re.IGNORECASE)
    return _has_corpus_dml(stripped) and not _DDL.search(stripped)


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
        sql = _sql_from_payload_or_disk(root, rel, payload)
        mutating_tool = tool_name(payload).lower()
        if _tracked(root, rel):
            # TRACK-139: retiring a corpus-only data-fix is allowed (delete + flyway repair).
            # Editing a committed versioned file is never allowed.
            if mutating_tool == "delete" and (
                _is_corpus_only_data_fix(sql) or _RETIRABLE_DATA_FIX.match(name)
            ):
                return 0
            return deny(
                f"ERROR: Committed versioned migration {rel} is immutable. "
                "Add a new VNN__*.sql (next free number) via /new-migration. "
                "Do not edit applied Flyway files. Corpus-only data-fix files may be "
                "deleted (TRACK-139) and then flyway-repaired.",
                payload,
            )
        if _has_corpus_dml(sql) and not _ALLOW.search(sql):
            return deny(
                f"ERROR: {rel} mutates corpus tables (krithi_*). Corpus corrections "
                "belong in parser/import/curation (ADR-012/013), not Flyway V__. "
                "Add `-- corpus-data-fix: allow` only for a rare justified exception.",
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
