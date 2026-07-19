#!/usr/bin/env python
"""Diagnose krithi section-structure issues by re-extracting from source.

Reusable triage tool for the "Section Issues" queue (see
application_documentation/07-quality/results/section-issues-cleanup/flagged-krithis-for-review.md). For each krithi it:

  1. reads the canonical template + stored lyric variants from the DB (read-only),
  2. re-fetches the source page (cached on disk),
  3. re-runs the CURRENT extraction pipeline (HtmlTextExtractor + normalize +
     StructureParser), and
  4. compares the freshly-parsed section set against the template, classifying
     the gap so a human/automation can pick the right remedy.

It never writes to the DB. Data repair goes through the audit-logged
variant-sections API (see section_repair.py).

Usage:
    python scripts/section_triage.py --krithi <uuid> [--krithi <uuid> ...]
    python scripts/section_triage.py --krithi <uuid> --json
    python scripts/section_triage.py --krithi <uuid> --show-source   # dump parsed sections

Env:
    DATABASE_URL   default postgresql://postgres:postgres@localhost:5432/sangita_grantha
    TRIAGE_CACHE   default <repo>/.triage-cache   (downloaded source HTML)
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from dataclasses import dataclass, field
from hashlib import sha256
from pathlib import Path

import httpx
import psycopg
from psycopg.rows import dict_row

# Make `src` importable when run from the worker dir or the repo root.
_WORKER_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(_WORKER_ROOT))

from src.diacritic_normalizer import normalize_garbled_diacritics  # noqa: E402
from src.html_extractor import HtmlTextExtractor  # noqa: E402
from src.structure_parser import StructureParser  # noqa: E402

DB_URL = os.environ.get("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/sangita_grantha")
CACHE_DIR = Path(os.environ.get("TRIAGE_CACHE", _WORKER_ROOT.parents[1] / ".triage-cache"))

INDIC_LANGS = ("sa", "ta", "te", "kn", "ml")


@dataclass
class VariantParse:
    language: str
    n_sections: int
    types: list[str]


@dataclass
class TriageResult:
    krithi_id: str
    title: str
    source_url: str | None
    template_types: list[str]
    stored_counts: dict[str, int]
    parsed: dict[str, VariantParse] = field(default_factory=dict)
    classification: str = ""
    note: str = ""

    @property
    def template_n(self) -> int:
        return len(self.template_types)


def _source_url(conn, krithi_id: str) -> str | None:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            "SELECT source_url FROM krithi_source_evidence WHERE krithi_id=%s AND source_url IS NOT NULL LIMIT 1",
            (krithi_id,),
        )
        row = cur.fetchone()
        if row and row["source_url"]:
            return row["source_url"]
        cur.execute(
            "SELECT source_reference FROM krithi_lyric_variants "
            "WHERE krithi_id=%s AND source_reference LIKE 'http%%' LIMIT 1",
            (krithi_id,),
        )
        row = cur.fetchone()
        if row and row["source_reference"]:
            return row["source_reference"]
        # Fall back to the "Created from import: <url>" note.
        cur.execute("SELECT notes FROM krithis WHERE id=%s", (krithi_id,))
        row = cur.fetchone()
        if row and row["notes"] and "http" in (row["notes"] or ""):
            return row["notes"].split("http", 1)[1].join(["http", ""]).strip()
    return None


def _load_from_db(conn, krithi_id: str) -> TriageResult:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute("SELECT title FROM krithis WHERE id=%s", (krithi_id,))
        row = cur.fetchone()
        title = row["title"] if row else "?"

        cur.execute(
            "SELECT section_type::text AS t FROM krithi_sections WHERE krithi_id=%s ORDER BY order_index",
            (krithi_id,),
        )
        template_types = [r["t"] for r in cur.fetchall()]

        cur.execute(
            "SELECT v.language, "
            "  (SELECT count(*) FROM krithi_lyric_sections ls WHERE ls.lyric_variant_id=v.id) AS n "
            "FROM krithi_lyric_variants v WHERE v.krithi_id=%s ORDER BY v.language",
            (krithi_id,),
        )
        stored = {r["language"]: int(r["n"]) for r in cur.fetchall()}

    return TriageResult(
        krithi_id=krithi_id,
        title=title,
        source_url=_source_url(conn, krithi_id),
        template_types=template_types,
        stored_counts=stored,
    )


def _fetch(url: str) -> str:
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    cached = CACHE_DIR / f"{sha256(url.encode()).hexdigest()[:16]}.html"
    if cached.exists():
        return cached.read_text("utf-8")
    resp = httpx.get(url, timeout=60, follow_redirects=True)
    resp.raise_for_status()
    cached.write_text(resp.text, "utf-8")
    return resp.text


def _classify(res: TriageResult) -> None:
    """Compare freshly-parsed variants to the template and label the gap."""
    if not res.parsed:
        res.classification = "no-parse"
        res.note = "source did not yield any lyric variants"
        return
    parsed_counts = {lang: p.n_sections for lang, p in res.parsed.items()}
    tmpl = res.template_n
    all_match_tmpl = all(n == tmpl for n in parsed_counts.values()) and len(parsed_counts) > 0
    all_agree = len(set(parsed_counts.values())) == 1
    parsed_n = next(iter(parsed_counts.values())) if all_agree else None

    if all_match_tmpl:
        # Parser reproduces the template for every variant -> siblings just need
        # a re-split write; template is correct.
        res.classification = "resplit-to-template"
        res.note = "parser matches template for all variants; re-split siblings via API"
    elif all_agree and parsed_n is not None and parsed_n > tmpl:
        res.classification = "template-undercount"
        res.note = (
            f"parser finds {parsed_n} sections for all variants; template has {tmpl} (fix template, then re-split)"
        )
    elif all_agree and parsed_n is not None and parsed_n < tmpl:
        res.classification = "parser-undercount"
        res.note = f"parser finds {parsed_n} < template {tmpl}; parser still misses markers (needs code fix)"
    else:
        res.classification = "mixed"
        res.note = f"variant counts disagree: {parsed_counts} vs template {tmpl}"


def triage(conn, krithi_id: str, extractor, parser) -> TriageResult:
    res = _load_from_db(conn, krithi_id)
    if not res.source_url:
        res.classification = "no-source"
        res.note = "no source URL on record"
        return res
    try:
        html = _fetch(res.source_url)
    except Exception as exc:  # noqa: BLE001
        res.classification = "fetch-failed"
        res.note = f"{type(exc).__name__}: {exc}"
        return res
    parse = parser.parse(normalize_garbled_diacritics(extractor.extract(html, base_url=res.source_url).text))
    for v in parse.lyric_variants:
        res.parsed[v.language] = VariantParse(
            language=v.language,
            n_sections=len(v.sections),
            types=[s.section_type.value for s in v.sections],
        )
    _classify(res)
    return res


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--krithi", action="append", default=[], help="krithi UUID (repeatable)")
    ap.add_argument("--json", action="store_true", help="emit JSON")
    ap.add_argument("--show-source", action="store_true", help="print parsed section types per variant")
    args = ap.parse_args()
    if not args.krithi:
        ap.error("provide at least one --krithi <uuid>")

    extractor = HtmlTextExtractor()
    parser = StructureParser()
    out = []
    with psycopg.connect(DB_URL) as conn:
        for kid in args.krithi:
            res = triage(conn, kid, extractor, parser)
            out.append(res)

    if args.json:
        print(
            json.dumps(
                [
                    {
                        "krithi_id": r.krithi_id,
                        "title": r.title,
                        "classification": r.classification,
                        "template_n": r.template_n,
                        "template_types": r.template_types,
                        "stored_counts": r.stored_counts,
                        "parsed": {lang: {"n": p.n_sections, "types": p.types} for lang, p in r.parsed.items()},
                        "source_url": r.source_url,
                        "note": r.note,
                    }
                    for r in out
                ],
                ensure_ascii=False,
                indent=2,
            )
        )
        return 0

    for r in out:
        print(f"\n=== {r.title}  [{r.krithi_id}]")
        print(f"    template: {r.template_n} {r.template_types}")
        print(f"    stored  : {r.stored_counts}")
        print("    parsed  : " + ", ".join(f"{lang}={p.n_sections}" for lang, p in r.parsed.items()))
        print(f"    -> {r.classification.upper()}: {r.note}")
        if args.show_source:
            for lang, p in r.parsed.items():
                print(f"       {lang}: {p.types}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
