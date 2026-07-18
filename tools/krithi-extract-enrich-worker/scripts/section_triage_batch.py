#!/usr/bin/env python
"""Batch-triage every krithi listed in application_documentation/07-quality/results/section-issues-cleanup/flagged-krithis-for-review.md.

Parses the flagged-review markdown, deduplicates krithi IDs (recording the FIRST
category each appears under), runs section_triage.triage() on each, and prints a
classification summary — overall and per source category. Writes a JSON report to
--out for downstream repair tooling.

Usage:
    python scripts/section_triage_batch.py --flagged application_documentation/07-quality/results/section-issues-cleanup/flagged-krithis-for-review.md \
        --out .triage-cache/triage_report.json
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

import psycopg

_WORKER_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(_WORKER_ROOT))
sys.path.insert(0, str(Path(__file__).resolve().parent))

from section_triage import DB_URL, triage  # noqa: E402
from src.html_extractor import HtmlTextExtractor  # noqa: E402
from src.structure_parser import StructureParser  # noqa: E402

_HEADING = re.compile(r"^##\s+(.*)$")
_UUID = re.compile(r"`([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})`")


def parse_flagged(md_path: Path) -> dict[str, str]:
    """Return {krithi_id: first_category_heading}."""
    category = "?"
    first_cat: dict[str, str] = {}
    for line in md_path.read_text("utf-8").splitlines():
        h = _HEADING.match(line)
        if h:
            category = h.group(1).strip()
            continue
        for m in _UUID.finditer(line):
            first_cat.setdefault(m.group(1), category)
    return first_cat


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--flagged", required=True)
    ap.add_argument("--out", default=None)
    ap.add_argument("--limit", type=int, default=0, help="triage only the first N (debug)")
    args = ap.parse_args()

    cats = parse_flagged(Path(args.flagged))
    ids = list(cats)
    if args.limit:
        ids = ids[: args.limit]
    print(f"Triaging {len(ids)} unique krithis…", file=sys.stderr)

    extractor = HtmlTextExtractor()
    parser = StructureParser()
    report = []
    with psycopg.connect(DB_URL) as conn:
        for i, kid in enumerate(ids, 1):
            try:
                res = triage(conn, kid, extractor, parser)
                cls = res.classification
            except Exception as exc:  # noqa: BLE001
                cls = "error"
                res = None
                print(f"  [{i}/{len(ids)}] {kid} ERROR {exc}", file=sys.stderr)
            report.append({
                "krithi_id": kid,
                "category": cats[kid],
                "title": res.title if res else "?",
                "classification": cls,
                "template_n": res.template_n if res else None,
                "template_types": res.template_types if res else None,
                "stored_counts": res.stored_counts if res else None,
                "parsed": {l: {"n": p.n_sections, "types": p.types} for l, p in res.parsed.items()} if res else None,
                "source_url": res.source_url if res else None,
                "note": res.note if res else "",
            })
            if i % 10 == 0:
                print(f"  …{i}/{len(ids)}", file=sys.stderr)

    # Summary
    overall = Counter(r["classification"] for r in report)
    by_cat: dict[str, Counter] = defaultdict(Counter)
    for r in report:
        by_cat[r["category"]][r["classification"]] += 1

    print("\n=== Overall classification ===")
    for cls, n in overall.most_common():
        print(f"  {cls:24} {n}")
    print("\n=== By source category ===")
    for cat, counter in by_cat.items():
        print(f"\n  {cat}")
        for cls, n in counter.most_common():
            print(f"     {cls:22} {n}")

    if args.out:
        Path(args.out).parent.mkdir(parents=True, exist_ok=True)
        Path(args.out).write_text(json.dumps(report, ensure_ascii=False, indent=2), "utf-8")
        print(f"\nWrote {args.out}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
