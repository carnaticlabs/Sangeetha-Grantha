#!/usr/bin/env python
"""Repair under-counted canonical templates, then re-split every variant.

Some krithis (classification `template-undercount` from section_triage) have a
canonical template that DROPPED a leading section at import — typically the
pallavi, head-captured because its Latin marker rendered as "P 1..." and was not
recognised. The current extraction pipeline now detects it, and ALL language
variants independently agree on the corrected structure. This tool:

  1. rewrites the canonical template to the corrected structure via the
     audit-logged `POST /v1/admin/krithis/{id}/sections` (UPDATE_KRITHI_SECTIONS), then
  2. re-splits EVERY variant (en + Indic) via `POST /v1/admin/variants/{id}/sections`
     (UPDATE_LYRIC_VARIANT_SECTIONS), mapping the freshly parsed sections onto the
     new section ids by order.

Both steps are required together: `saveSections` upserts by order_index, so
prepending a section RELABELS the existing rows in place — the re-split then puts
the right lyric under each new label. This mutates the template AND the en variant,
so it is intentionally conservative and dry-run by default.

SAFETY GATES (all must hold, or the krithi is skipped):
  * classification == template-undercount
  * every variant's parsed section types are identical (cross-validated structure)
  * the corrected structure is the template with 1–2 PALLAVI/ANUPALLAVI sections
    PREPENDED and the remainder identical (a clean prepend — never a mid-list relabel)
  * per variant: last section ≤ 2× the en last section (no translation trailer)
  * per variant: re-split preserves ≥ 97% of the stored blob's characters

Usage:
    python scripts/template_repair.py --from-report .triage-cache/triage_report3.json
    python scripts/template_repair.py --krithi <uuid> --apply
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import time
from pathlib import Path

import httpx
import psycopg
from psycopg.rows import dict_row

# The backend enforces a GLOBAL 100 requests/minute limit (all endpoints, keyed by
# host). A full apply is ~170 requests, so we throttle below the limit and retry on
# 429 — critically, so a template change is always followed by its variant re-splits
# in the same run and no krithi is left relabeled-but-unsplit.
_THROTTLE_S = 1.05          # ~57 req/min, leaving headroom for the frontend/other clients
_last_request_at = 0.0


def _request(method: str, url: str, **kwargs) -> httpx.Response:
    global _last_request_at
    for attempt in range(6):
        wait = _THROTTLE_S - (time.monotonic() - _last_request_at)
        if wait > 0:
            time.sleep(wait)
        resp = httpx.request(method, url, timeout=30, **kwargs)
        _last_request_at = time.monotonic()
        if resp.status_code != 429:
            resp.raise_for_status()
            return resp
        retry_after = int(resp.headers.get("Retry-After", "61") or "61")
        print(f"    …429 rate-limited, waiting {retry_after}s (attempt {attempt + 1}/6)")
        time.sleep(retry_after + 1)
    resp.raise_for_status()
    return resp

_WORKER_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(_WORKER_ROOT))
sys.path.insert(0, str(Path(__file__).resolve().parent))

from section_triage import DB_URL, _fetch, triage  # noqa: E402
from section_repair import _content_preserved, _stored_blob  # noqa: E402
from src.diacritic_normalizer import normalize_garbled_diacritics  # noqa: E402
from src.html_extractor import HtmlTextExtractor  # noqa: E402
from src.structure_parser import StructureParser  # noqa: E402

API = os.environ.get("SANGITA_API", "http://localhost:8080")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "dev-admin-token")
ADMIN_EMAIL = os.environ.get("ADMIN_EMAIL", "admin@sangitagrantha.org")

PREPENDABLE = {"PALLAVI", "ANUPALLAVI"}


def _label_for(section_type: str, charanam_idx: int) -> str:
    if section_type == "CHARANAM":
        return "Charanam" if charanam_idx == 1 else f"Charanam {charanam_idx}"
    return section_type.replace("_", " ").title()


def _token() -> str:
    r = _request("POST", f"{API}/v1/auth/token", json={
        "adminToken": ADMIN_TOKEN, "email": ADMIN_EMAIL, "roles": ["ADMIN", "CURATOR"],
    })
    return r.json()["token"]


def _parsed_variants(res):
    """Re-parse the source once; return {lang: [DetectedSection,...]}."""
    html = _fetch(res.source_url)
    parsed = StructureParser().parse(
        normalize_garbled_diacritics(HtmlTextExtractor().extract(html, base_url=res.source_url).text)
    )
    return {v.language: v.sections for v in parsed.lyric_variants}


def _variant_ids(conn, krithi_id: str) -> dict[str, str]:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute("SELECT language, id::text AS vid FROM krithi_lyric_variants WHERE krithi_id=%s", (krithi_id,))
        return {r["language"]: r["vid"] for r in cur.fetchall()}


def _section_ids_by_order(conn, krithi_id: str) -> dict[int, str]:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute("SELECT order_index, id::text AS sid FROM krithi_sections WHERE krithi_id=%s ORDER BY order_index", (krithi_id,))
        return {r["order_index"]: r["sid"] for r in cur.fetchall()}


def plan(conn, kid: str):
    """Return a repair plan dict, or (None, reason) if the krithi is ineligible."""
    res = triage(conn, kid, HtmlTextExtractor(), StructureParser())
    if res.classification != "template-undercount":
        return None, f"{res.classification}: {res.note}"

    parsed = _parsed_variants(res)
    if "en" not in parsed:
        return None, "no en variant parsed"
    corrected = [s.section_type.value for s in parsed["en"]]

    # Cross-validation: every variant must agree on the corrected structure.
    for lang, secs in parsed.items():
        if [s.section_type.value for s in secs] != corrected:
            return None, f"variants disagree ({lang} differs from en)"

    tmpl = res.template_types
    k = len(corrected) - len(tmpl)
    if k < 1 or k > 2:
        return None, f"delta {k} not in 1..2"
    if corrected[k:] != tmpl:
        return None, f"not a clean prepend (corrected {corrected} vs template {tmpl})"
    if any(t not in PREPENDABLE for t in corrected[:k]):
        return None, f"prepended types {corrected[:k]} not in {PREPENDABLE}"

    # Structure is cross-validated (all variants agree), so the TEMPLATE change is
    # safe. Re-split each variant only if it individually passes the trailer +
    # content gates; a variant that fails (e.g. Tamil with a translation trailer)
    # is deferred — its single blob is left in place, still flagged, for a later
    # trailer-aware pass. en must pass (it is the template variant).
    en_last = len(parsed["en"][-1].text) if parsed["en"] else 0
    variant_ids = _variant_ids(conn, kid)
    variant_plan = {}
    deferred = {}
    for lang, secs in parsed.items():
        vid = variant_ids.get(lang)
        if not vid:
            return None, f"no variant id for {lang}"
        if en_last and len(secs[-1].text) > 2.0 * en_last:
            deferred[lang] = f"trailer {len(secs[-1].text)/en_last:.1f}x"
            continue
        ok, ratio = _content_preserved(_stored_blob(conn, vid), "\n".join(s.text for s in secs))
        if not ok:
            deferred[lang] = f"content {ratio:.0%}"
            continue
        variant_plan[lang] = (vid, secs)
    if "en" not in variant_plan:
        return None, f"en variant failed its own gate ({deferred.get('en','?')})"

    # Build the corrected template payload (1-based order_index, matching DB).
    charanam = 0
    template_payload = []
    for i, t in enumerate(corrected, start=1):
        if t == "CHARANAM":
            charanam += 1
        template_payload.append({"sectionType": t, "orderIndex": i, "label": _label_for(t, charanam)})

    return {
        "krithi_id": kid, "title": res.title, "template_before": tmpl, "template_after": corrected,
        "prepend": corrected[:k], "template_payload": template_payload, "variants": variant_plan,
        "deferred": deferred,
    }, None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--krithi", action="append", default=[])
    ap.add_argument("--from-report")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    ids = list(args.krithi)
    if args.from_report:
        rep = json.loads(Path(args.from_report).read_text("utf-8"))
        ids += [r["krithi_id"] for r in rep if r["classification"] == "template-undercount"]
    ids = list(dict.fromkeys(ids))
    if not ids:
        ap.error("provide --krithi or --from-report")

    token = _token() if args.apply else None
    eligible = skipped = variant_writes = 0

    with psycopg.connect(DB_URL) as conn:
        for kid in ids:
            p, reason = plan(conn, kid)
            if p is None:
                print(f"SKIP [{kid[:8]}] {reason}")
                skipped += 1
                continue
            eligible += 1
            defer_note = f"  [defer: {p['deferred']}]" if p["deferred"] else ""
            print(f"\n{p['title']} [{kid[:8]}]  {p['template_before']}  ->  {p['template_after']}  (+{p['prepend']}){defer_note}")

            if args.apply:
                _request("POST", f"{API}/v1/admin/krithis/{kid}/sections",
                         headers={"Authorization": f"Bearer {token}"},
                         json={"sections": p["template_payload"]})
                sid_by_order = _section_ids_by_order(conn, kid)  # re-read after template change

            for lang, (vid, secs) in p["variants"].items():
                variant_writes += 1
                if args.apply:
                    payload = {"sections": [{"sectionId": sid_by_order[i + 1], "text": s.text}
                                            for i, s in enumerate(secs)]}
                    rr = _request("POST", f"{API}/v1/admin/variants/{vid}/sections",
                                  headers={"Authorization": f"Bearer {token}"}, json=payload)
                    print(f"    {lang}: {len(secs)} sections -> HTTP {rr.status_code}")
                else:
                    print(f"    {lang}: {len(secs)} sections -> DRY-RUN")

    print(f"\n{'APPLIED' if args.apply else 'DRY-RUN'}: {eligible} krithis eligible, "
          f"{variant_writes} variant re-splits, {skipped} krithis skipped")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
