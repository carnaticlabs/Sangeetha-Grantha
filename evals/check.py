#!/usr/bin/env python3
"""Deterministic agent-config evals (no model calls).

Cases live in evals/cases/*.json. Each case names a file and strings that
must (or must not) remain in agent configuration. Hook smoke tests live here
so a CLAUDE.md/skill edit cannot drop Flyway, audit, or secret-file guards
without CI noticing.

Exit 0 if all cases and hook smokes pass; 1 otherwise.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = Path(__file__).resolve().parent / "cases"
HOOKS = ROOT / ".claude" / "hooks"


def _fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)


def run_cases() -> int:
    failures = 0
    files = sorted(CASES.glob("*.json"))
    if not files:
        _fail(f"no eval cases in {CASES}")
        return 1

    for case_path in files:
        case = json.loads(case_path.read_text(encoding="utf-8"))
        case_id = case.get("id", case_path.stem)
        rel = case["file"]
        target = ROOT / rel
        if not target.is_file():
            _fail(f"{case_id}: missing file {rel}")
            failures += 1
            continue
        text = target.read_text(encoding="utf-8")
        case_failed = False
        for needle in case.get("must_contain", []):
            if needle not in text:
                _fail(f"{case_id}: {rel} missing required text {needle!r}")
                failures += 1
                case_failed = True
        for needle in case.get("must_not_contain", []):
            if needle in text:
                _fail(f"{case_id}: {rel} contains forbidden text {needle!r}")
                failures += 1
                case_failed = True
        if not case_failed:
            print(f"ok  {case_id}")
    return failures


def _hook(
    script: str,
    payload: dict,
    env: dict[str, str] | None = None,
) -> subprocess.CompletedProcess[str]:
    run_env = os.environ.copy()
    run_env.pop("SANGITA_ALLOW_TEST_EDITS", None)
    if env:
        run_env.update(env)
    return subprocess.run(
        [sys.executable, str(HOOKS / script)],
        input=json.dumps(payload),
        capture_output=True,
        text=True,
        cwd=ROOT,
        env=run_env,
        check=False,
    )


def run_hook_smokes() -> int:
    failures = 0
    smokes = [
        (
            "deny-secrets.env-read",
            "deny-secrets.py",
            {"tool_name": "Read", "tool_input": {"file_path": ".env"}},
            2,
        ),
        (
            "deny-secrets.claude-ok",
            "deny-secrets.py",
            {"tool_name": "Read", "tool_input": {"file_path": "CLAUDE.md"}},
            0,
        ),
        (
            "deny-secrets.bash-cat",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "cat config/local.env"}},
            2,
        ),
        (
            "protect-migrations.committed-v",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "database/migrations/V01__baseline-schema-and-types.sql"
                },
            },
            2,
        ),
        (
            "protect-migrations.repeatable",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "database/migrations/R__seed_04_raga_reference.sql"
                },
            },
            0,
        ),
        (
            "protect-migrations.liquibase",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {"file_path": "database/migrations/changelog.xml"},
            },
            2,
        ),
        (
            "protect-migrations.unrelated",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {"file_path": "modules/backend/api/src/Foo.kt"},
            },
            0,
        ),
        (
            "protect-migrations.cursor-shape",
            "protect-migrations.py",
            {
                "tool": {
                    "name": "Write",
                    "input": {
                        "path": "database/migrations/V01__baseline-schema-and-types.sql"
                    },
                }
            },
            2,
        ),
        (
            "protect-tests.committed-kt",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {
                    "file_path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/KrithiServiceTest.kt"
                },
            },
            2,
        ),
        (
            "protect-tests.new-test-ok",
            "protect-tests.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/BrandNewFeatureTest.kt"
                },
            },
            0,
        ),
        (
            "protect-tests.non-test-ok",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {"file_path": "modules/backend/api/src/Foo.kt"},
            },
            0,
        ),
        (
            "protect-tests.cursor-shape",
            "protect-tests.py",
            {
                "tool": {
                    "name": "Edit",
                    "input": {
                        "path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/KrithiServiceTest.kt"
                    },
                }
            },
            2,
        ),
        (
            "deny-secrets.env-example",
            "deny-secrets.py",
            {"tool_name": "Read", "tool_input": {"file_path": ".env.example"}},
            0,
        ),
        (
            "deny-secrets.env-example-bash",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "cat .env.example"}},
            0,
        ),
        (
            "deny-secrets.env-auto-approval-example",
            "deny-secrets.py",
            {
                "tool_name": "Read",
                "tool_input": {"file_path": "config/.env.auto-approval.example"},
            },
            0,
        ),
        (
            "deny-secrets.env-auto-approval-example-bash",
            "deny-secrets.py",
            {
                "tool_name": "Bash",
                "tool_input": {
                    "command": "cat config/.env.auto-approval.example"
                },
            },
            0,
        ),
        (
            "deny-secrets.env-auto-approval-secret",
            "deny-secrets.py",
            {
                "tool_name": "Read",
                "tool_input": {"file_path": "config/.env.auto-approval"},
            },
            2,
        ),
        (
            "deny-secrets.python-open",
            "deny-secrets.py",
            {
                "tool_name": "Bash",
                "tool_input": {"command": "python3 -c \"print(open('.env').read())\""},
            },
            2,
        ),
        (
            "deny-secrets.rg",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "rg SECRET .env"}},
            2,
        ),
        (
            "deny-secrets.source",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "source .env"}},
            2,
        ),
        (
            "deny-secrets.cursor-shell",
            "deny-secrets.py",
            {
                "hook_event_name": "beforeShellExecution",
                "command": "cat .env",
                "conversation_id": "eval",
            },
            2,
            {"stdout_contains": '"permission": "deny"'},
        ),
        # Tightened detection: a secret path as TEXT or a search PATTERN is not a dump.
        (
            "deny-secrets.grep-pattern-not-file",
            "deny-secrets.py",
            {
                "tool_name": "Bash",
                "tool_input": {"command": "grep 'Read(.env.*)' .claude/settings.json"},
            },
            0,
        ),
        (
            "deny-secrets.grep-env-as-pattern",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "git diff | grep .env"}},
            0,
        ),
        (
            "deny-secrets.mentions-in-message",
            "deny-secrets.py",
            {
                "tool_name": "Bash",
                "tool_input": {"command": "git commit -m 'never read .env files'"},
            },
            0,
        ),
        (
            "deny-secrets.head-still-blocked",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "head -n 5 config/local.env"}},
            2,
        ),
        (
            "deny-secrets.redirect-blocked",
            "deny-secrets.py",
            {"tool_name": "Bash", "tool_input": {"command": "while read l; do :; done < .env"}},
            2,
        ),
        (
            "protect-migrations.delete-v",
            "protect-migrations.py",
            {
                "tool_name": "Delete",
                "tool_input": {
                    "path": "database/migrations/V01__baseline-schema-and-types.sql"
                },
            },
            2,
        ),
        (
            "protect-migrations.delete-corpus-fix",
            "protect-migrations.py",
            {
                "tool_name": "Delete",
                "tool_input": {
                    "path": "database/migrations/V45__remove_stale_anupallavi_brhannayaki.sql"
                },
            },
            0,
        ),
        (
            "protect-migrations.new-corpus-dml",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "database/migrations/V99__fix_krithi_sections.sql",
                    "contents": "DELETE FROM krithi_sections WHERE id = '00000000-0000-0000-0000-000000000000';\n",
                },
            },
            2,
        ),
        (
            "protect-migrations.new-corpus-dml-override",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "database/migrations/V99__curator_mirror.sql",
                    "contents": "-- corpus-data-fix: allow\nDELETE FROM krithi_sections WHERE id = '00000000-0000-0000-0000-000000000000';\n",
                },
            },
            0,
        ),
        (
            "protect-migrations.fk-mention-not-corpus-dml",
            "protect-migrations.py",
            {
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "database/migrations/V99__krithi_aux.sql",
                    "contents": (
                        "CREATE TABLE krithi_aux (krithi_id uuid REFERENCES krithis);\n"
                        "INSERT INTO krithi_aux (krithi_id) SELECT id FROM krithis;\n"
                    ),
                },
            },
            0,
        ),
        (
            "protect-migrations.read-ok",
            "protect-migrations.py",
            {
                "tool_name": "Read",
                "tool_input": {
                    "file_path": "database/migrations/V01__baseline-schema-and-types.sql"
                },
            },
            0,
        ),
        (
            "protect-migrations.strreplace",
            "protect-migrations.py",
            {
                "tool_name": "StrReplace",
                "tool_input": {
                    "path": "database/migrations/V01__baseline-schema-and-types.sql"
                },
            },
            2,
        ),
        (
            "protect-tests.fixtures-ok",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {
                    "file_path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/testsupport/MoneyPathFixtures.kt"
                },
            },
            0,
        ),
        (
            "protect-tests.setup-ok",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {
                    "file_path": "modules/frontend/sangita-admin-web/src/test/setup.ts"
                },
            },
            0,
        ),
        (
            "protect-tests.override-0-still-deny",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {
                    "file_path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/KrithiServiceTest.kt"
                },
            },
            2,
            {"env": {"SANGITA_ALLOW_TEST_EDITS": "0"}},
        ),
        (
            "protect-tests.override-1-allow",
            "protect-tests.py",
            {
                "tool_name": "Edit",
                "tool_input": {
                    "file_path": "modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/KrithiServiceTest.kt"
                },
            },
            0,
            {"env": {"SANGITA_ALLOW_TEST_EDITS": "1"}},
        ),
    ]
    for smoke in smokes:
        name, script, payload, expected = smoke[:4]
        extra = smoke[4] if len(smoke) > 4 else {}
        proc = _hook(script, payload, extra.get("env"))
        if proc.returncode != expected:
            _fail(
                f"hook {name}: expected exit {expected}, got {proc.returncode}"
            )
            failures += 1
            continue
        needle = extra.get("stdout_contains")
        if needle and needle not in proc.stdout:
            _fail(f"hook {name}: stdout missing {needle!r}")
            failures += 1
            continue
        print(f"ok  hook:{name}")
    return failures


_VERSIONED_FILE = re.compile(r"^V(\d+)__.+\.sql$", re.IGNORECASE)
_CORPUS_ALLOW = re.compile(r"--\s*corpus-data-fix:\s*allow\b", re.IGNORECASE)

def _mutates_corpus_tables(sql: str) -> bool:
    """True only when DML *targets* a corpus table, not when the name appears as an FK."""
    return bool(
        re.search(
            r"\b(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+(?:ONLY\s+)?(?:public\.)?"
            r"(krithis|krithi_sections|krithi_lyric_variants|krithi_lyric_sections|"
            r"krithi_ragas|krithi_revisions)\b",
            sql,
            re.IGNORECASE,
        )
    )
# Pre-TRACK-139 files may touch krithi_* (raga identity or data-fixes). New V__
# after V57 must not mutate corpus tables without an override comment.
# V58–V62 were retired; the next versioned file is V58 and is not grandfathered.
_CORPUS_DML_MAX_GRANDFATHER = 57


def run_corpus_migration_lint() -> int:
    """Fail when a non-grandfathered V__ mutates krithi_* corpus tables (TRACK-139)."""
    failures = 0
    migrations = ROOT / "database" / "migrations"
    if not migrations.is_dir():
        _fail("corpus-migration-lint: missing database/migrations")
        return 1
    for path in sorted(migrations.glob("V*.sql")):
        match = _VERSIONED_FILE.match(path.name)
        if not match:
            continue
        version = int(match.group(1))
        if version <= _CORPUS_DML_MAX_GRANDFATHER:
            continue
        text = path.read_text(encoding="utf-8")
        if _mutates_corpus_tables(text) and not _CORPUS_ALLOW.search(text):
            _fail(
                f"corpus-migration-lint: {path.name} mutates krithi_* corpus tables; "
                "use parser/import/curation (ADR-012/013) or `-- corpus-data-fix: allow`"
            )
            failures += 1
    if failures == 0:
        print("ok  corpus-migration-lint")
    return failures


def main() -> int:
    failures = run_cases() + run_hook_smokes() + run_corpus_migration_lint()
    if failures:
        print(f"{failures} agent-eval failure(s)", file=sys.stderr)
        return 1
    print("agent-evals passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
