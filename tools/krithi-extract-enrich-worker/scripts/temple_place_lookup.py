"""TRACK-154 CLI: Google place-id lookup, triage and seed generation for temples.

Run from the worker root (see scripts/_common.py):

    export GOOGLE_PLACES_API_KEY=...        # never committed, never logged
    PYTHONPATH=. uv run python scripts/temple_place_lookup.py export-targets storage/track154/targets.json
    PYTHONPATH=. uv run python scripts/temple_place_lookup.py lookup storage/track154/targets.json \
        storage/track154/candidates.json --pass 1
    PYTHONPATH=. uv run python scripts/temple_place_lookup.py triage storage/track154/targets.json \
        storage/track154/candidates.json storage/track154/review.json
    # curator edits review.json, fills signoff, then:
    PYTHONPATH=. uv run python scripts/temple_place_lookup.py build storage/track154/review.json

Only `lookup` touches the network. Candidate and review files hold Google names and addresses:
keep them under the gitignored `storage/` and delete them after the review.
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import sys
from pathlib import Path

from scripts._common import database_url, setup_logging
from src import temple_place_lookup as tpl

REPO_ROOT = Path(__file__).resolve().parents[3]
DATA_JSON = REPO_ROOT / "database/data/track154-temple-place-review.json"
SEED_SQL = REPO_ROOT / "database/migrations" / tpl.SEED_FILE_NAME

logger = logging.getLogger("temple_place_lookup")


def cmd_export(args: argparse.Namespace) -> int:
    import psycopg

    out = Path(args.out)
    tpl.assert_untracked_location(out)
    with psycopg.connect(database_url()) as conn:
        targets = tpl.export_targets(conn)
    out.write_text(json.dumps([t.model_dump() for t in targets], indent=2, ensure_ascii=False), "utf-8")
    kinds = {k: sum(t.place_kind == k for t in targets) for k in sorted({t.place_kind for t in targets})}
    print(f"{len(targets)} temples exported {kinds}; lookup targets: {sum(t.is_lookup_target for t in targets)}")
    return 0


def cmd_lookup(args: argparse.Namespace) -> int:
    key = os.environ.get(tpl.API_KEY_ENV, "")
    if not key:
        print(f"{tpl.API_KEY_ENV} is not set", file=sys.stderr)
        return 2
    store = Path(args.candidates)
    tpl.assert_untracked_location(store)
    targets = tpl.load_targets(Path(args.targets))
    client = tpl.PlacesClient(key, max_calls=tpl.max_calls_from_env())
    summary = tpl.LookupRunner(client, store).run(targets, pass_no=args.pass_no)
    print(
        f"calls this run: {summary.calls_this_run}; calls total (reconcile with the Cloud console): "
        f"{summary.calls_total} of ceiling {client.max_calls}; answered {summary.answered}; "
        f"resumed {summary.skipped_resumed}; failed {len(summary.failed)}"
    )
    if summary.stopped_reason:
        print(f"stopped early: {summary.stopped_reason}", file=sys.stderr)
        return 3
    return 1 if summary.failed else 0


def cmd_triage(args: argparse.Namespace) -> int:
    out = Path(args.review)
    tpl.assert_untracked_location(out)
    targets = tpl.load_targets(Path(args.targets))
    review = tpl.triage(targets, tpl.load_candidates(Path(args.candidates)))
    out.write_text(review.model_dump_json(indent=2), "utf-8")
    report = tpl.coverage_report(review)
    print(f"targets {report.total}: {report.counts}")
    for e in review.entries:
        if e.decision is None:
            print(f"  review [{e.triage}] {e.name} ({e.city}, {e.state})")
    auto = tpl.auto_confirmed_lines(review)
    print(
        f"\nauto-confirmed ({len(auto)}) - check each address is the temple, not a nearby shrine;"
        " your sign-off covers these:"
    )
    for line in auto:
        print(f"  {line}")
    return 0


def cmd_build(args: argparse.Namespace) -> int:
    review = tpl.ReviewFile.model_validate_json(Path(args.review).read_text("utf-8"))
    try:
        data = tpl.build_place_data(review)
    except tpl.BuildError as exc:
        print(f"build refused: {exc}", file=sys.stderr)
        return 1
    DATA_JSON.write_text(tpl.canonical_json(data), "utf-8")
    SEED_SQL.write_text(tpl.render_seed_sql(data), "utf-8")
    print(f"wrote {DATA_JSON.relative_to(REPO_ROOT)} and {SEED_SQL.relative_to(REPO_ROOT)}")
    print(f"coverage: {tpl.coverage_report(review)}")
    return 0


def main(argv: list[str] | None = None) -> int:
    setup_logging()
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("export-targets", help="read-only export of temples from the dev database")
    p.add_argument("out")
    p.set_defaults(func=cmd_export)
    p = sub.add_parser("lookup", help="Places Text Search (network); resumable; hard call ceiling")
    p.add_argument("targets")
    p.add_argument("candidates")
    p.add_argument("--pass", dest="pass_no", type=int, choices=(1, 2), default=1)
    p.set_defaults(func=cmd_lookup)
    p = sub.add_parser("triage", help="auto-confirm rule, review list, coverage report")
    p.add_argument("targets")
    p.add_argument("candidates")
    p.add_argument("review")
    p.set_defaults(func=cmd_triage)
    p = sub.add_parser("build", help="curator-edited review -> committed JSON + R__seed_12 SQL")
    p.add_argument("review")
    p.set_defaults(func=cmd_build)
    args = parser.parse_args(argv)
    return int(args.func(args))


if __name__ == "__main__":
    raise SystemExit(main())
