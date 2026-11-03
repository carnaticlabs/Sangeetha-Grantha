#!/usr/bin/env python3
"""Fail when a change touches code that has a documented counterpart but the docs did not move.

Documentation drifts when the code that a doc describes changes and nobody opens the
doc. This maps high-signal source paths to the docs that describe them and reports
every rule where the code changed and none of its docs did. It does not judge whether
a doc edit is *sufficient*; `/retrospective-commit-and-push` has the agent make the
edit, and this gate keeps it from being skipped.

Changed files are the union of:
  * committed changes since the merge base with the base ref (default origin/main)
  * staged and unstaged edits
  * untracked files that are not gitignored

Escape hatch: a commit in the range (or a trailer line in --message-file) containing
`Docs-Impact: none` waives the check, for refactors that really have no doc surface.

Usage:
    python3 tools/check-doc-impact.py                  # vs origin/main
    python3 tools/check-doc-impact.py --base main
    python3 tools/check-doc-impact.py --json
    python3 tools/check-doc-impact.py --self-test

Exit 0 when every triggered rule is satisfied (or waived).
Exit 1 when a rule is triggered without a doc change, or --self-test fails.
"""

from __future__ import annotations

import argparse
import fnmatch
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = "application_documentation"
WAIVER = "Docs-Impact: none"


@dataclass(frozen=True)
class Rule:
    name: str
    code: tuple[str, ...]  # fnmatch globs; `*` also crosses `/`
    docs: tuple[str, ...]  # any one of these changing satisfies the rule


RULES: tuple[Rule, ...] = (
    Rule(
        "schema",
        ("database/migrations/V*.sql",),
        (f"{DOCS}/04-database/schema.md", f"{DOCS}/04-database/migrations.md"),
    ),
    Rule(
        "versions",
        (
            "gradle/libs.versions.toml",
            "modules/frontend/sangita-admin-web/package.json",
            "tools/krithi-extract-enrich-worker/pyproject.toml",
        ),
        (f"{DOCS}/00-meta/current-versions.md",),
    ),
    Rule(
        "openapi",
        ("openapi/*.yaml",),
        (f"{DOCS}/03-api/api-contract.md", f"{DOCS}/03-api/openapi-sync.md"),
    ),
    Rule(
        "backend-routes",
        ("modules/backend/api/src/main/*/routes/*",),
        (
            f"{DOCS}/03-api/api-contract.md",
            f"{DOCS}/03-api/api-examples.md",
            "openapi/sangita-grantha.openapi.yaml",
        ),
    ),
    Rule(
        "dev-workflow",
        ("Makefile", "compose.yaml"),
        (
            "CLAUDE.md",
            f"{DOCS}/00-onboarding/getting-started.md",
            f"{DOCS}/02-architecture/tech-stack.md",
        ),
    ),
)


def _git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args], cwd=ROOT, capture_output=True, text=True, check=False
    )
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def changed_files(base: str) -> set[str]:
    merge_base = _git("merge-base", base, "HEAD").strip()
    names = set(_git("diff", "--name-only", merge_base).splitlines())
    names |= set(_git("ls-files", "--others", "--exclude-standard").splitlines())
    return {n for n in names if n}


def waived(base: str, message_file: str | None) -> bool:
    texts = [_git("log", f"{base}..HEAD", "--format=%B")]
    if message_file:
        texts.append(Path(message_file).read_text(encoding="utf-8"))
    return any(WAIVER.lower() in t.lower() for t in texts)


def matches(path: str, globs: tuple[str, ...]) -> bool:
    return any(fnmatch.fnmatchcase(path, g) for g in globs)


def evaluate(changed: set[str]) -> list[dict]:
    """Return one finding per triggered rule whose docs did not change."""
    findings = []
    for rule in RULES:
        triggers = sorted(p for p in changed if matches(p, rule.code))
        if triggers and not any(d in changed for d in rule.docs):
            findings.append(
                {"rule": rule.name, "changed": triggers, "update_one_of": list(rule.docs)}
            )
    return findings


def self_test() -> int:
    cases = [
        ({"database/migrations/V99__x.sql"}, ["schema"]),
        ({"database/migrations/V99__x.sql", f"{DOCS}/04-database/schema.md"}, []),
        ({"gradle/libs.versions.toml"}, ["versions"]),
        ({"gradle/libs.versions.toml", f"{DOCS}/00-meta/current-versions.md"}, []),
        ({"modules/backend/api/src/main/kotlin/x/routes/KrithiRoutes.kt"}, ["backend-routes"]),
        ({"Makefile", "CLAUDE.md"}, []),
        ({"modules/frontend/sangita-admin-web/src/App.tsx"}, []),
    ]
    failed = 0
    for changed, expected in cases:
        got = [f["rule"] for f in evaluate(changed)]
        if got != expected:
            print(f"FAIL: {sorted(changed)} -> {got}, expected {expected}", file=sys.stderr)
            failed += 1
    print("self-test " + ("FAILED" if failed else "ok"))
    return 1 if failed else 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--base", default="origin/main")
    parser.add_argument("--message-file", help="commit message to scan for the waiver")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    try:
        changed = changed_files(args.base)
        is_waived = waived(args.base, args.message_file)
    except RuntimeError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    findings = evaluate(changed)
    if args.json:
        print(json.dumps({"waived": is_waived, "findings": findings}, indent=2))
    elif findings:
        for f in findings:
            print(f"[{f['rule']}] changed: {', '.join(f['changed'])}")
            print(f"    update one of: {', '.join(f['update_one_of'])}")
        print(f"\nAdd the doc update, or add a `{WAIVER}` line to a commit message.")
    else:
        print("doc impact: no undocumented changes")

    return 1 if findings and not is_waived else 0


if __name__ == "__main__":
    sys.exit(main())
