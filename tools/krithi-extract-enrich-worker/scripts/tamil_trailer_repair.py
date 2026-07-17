#!/usr/bin/env python
"""Re-split variants whose last section carried an inline translation trailer.

Companion to `section_repair.py` for the one case its content gate cannot pass:
Govindan-blog Tamil variants append a word-by-word Tamil *meaning* after the lyric,
so the re-split is intentionally SHORTER than the (trailer-laden) stored blob and the
`len(new)/len(old) >= 0.97` gate would read the trailer removal as content loss.

Here the parser's `strip_refrain_trailer` does the cut, and we verify it directly:
parse the source twice — once normally (trailer stripped) and once with stripping
disabled — so the exact removed tail is known, and confirm it is natural-language
prose (no HK pronunciation digits), never transliterated lyric. Only then is the
stripped re-split written through the audit-logged variant-sections API.

Safety gates (all must hold, per variant):
  * krithi classification == resplit-to-template (parser reproduces the template)
  * stripped section count + types == template
  * the variant is actually broken (stored count != template)
  * a trailer WAS removed, and the removed tail is prose (≥40 chars, no pronunciation
    digits) — proof we cut a translation, not lyric
  * sanity: the re-split shares a ≥20-char run with the stored blob (same composition)

Usage:
    python scripts/tamil_trailer_repair.py --from-report .triage-cache/triage_report.json
    python scripts/tamil_trailer_repair.py --krithi <uuid> --lang ta --apply
"""
from __future__ import annotations

import argparse
import difflib
import json
import re
import sys
from pathlib import Path

import psycopg

_HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(_HERE))
sys.path.insert(0, str(_HERE.parent))

from section_repair import (  # noqa: E402  (reuse the audited helpers)
    _request, _stored_blob, _template_sections, _token, _variant_ids, API,
)
from section_triage import DB_URL, _fetch, triage  # noqa: E402
from src.diacritic_normalizer import normalize_garbled_diacritics  # noqa: E402
from src.html_extractor import HtmlTextExtractor  # noqa: E402
from src.structure_parser import StructureParser, _PRONUNCIATION_DIGIT  # noqa: E402

_MIN_TRAILER_CHARS = 40
_MIN_SHARED_RUN = 20


def _parse(url: str, *, strip: bool):
    p = StructureParser()
    p._trailer_strip_enabled = strip
    html = _fetch(url)
    return {v.language: v for v in p.parse(
        normalize_garbled_diacritics(HtmlTextExtractor().extract(html, base_url=url).text)
    ).lyric_variants}


def _longest_shared_run(a: str, b: str) -> int:
    na, nb = re.sub(r"\s+", "", a), re.sub(r"\s+", "", b)
    if not na or not nb:
        return 0
    return difflib.SequenceMatcher(None, na, nb, autojunk=False).find_longest_match(
        0, len(na), 0, len(nb)).size


def _assess(res, lang, template, stored_blob):
    """Return (sections, None) if safe to write, else (None, reason)."""
    tmpl_types = [t for _, t, _ in template]
    stripped = _parse(res.source_url, strip=True).get(lang)
    full = _parse(res.source_url, strip=False).get(lang)
    if not stripped or not full:
        return None, f"{lang} not parsed"
    secs = stripped.sections
    if len(secs) != len(template) or [s.section_type.value for s in secs] != tmpl_types:
        return None, f"{lang} parsed {[s.section_type.value for s in secs]} != template"

    # What did the strip remove from the last section?
    full_last, strip_last = full.sections[-1].text, secs[-1].text
    if full_last == strip_last:
        return None, f"{lang} no trailer removed — use section_repair"
    dropped = (full_last[len(strip_last):] if full_last.startswith(strip_last)
               else full_last[len(strip_last):]).strip()
    if len(dropped) < _MIN_TRAILER_CHARS:
        return None, f"{lang} removed tail too short ({len(dropped)} chars)"
    if _PRONUNCIATION_DIGIT.search(dropped):
        return None, f"{lang} removed tail carries lyric markers — refusing"

    # Sanity: this really is the same composition as what's stored.
    new_concat = "\n".join(s.text for s in secs)
    run = _longest_shared_run(new_concat, stored_blob)
    if run < _MIN_SHARED_RUN:
        return None, f"{lang} weak overlap with stored blob (run={run})"
    return secs, None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--krithi", action="append", default=[])
    ap.add_argument("--from-report")
    ap.add_argument("--lang", default="ta", help="variant language to repair (default: ta)")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    ids = list(args.krithi)
    if args.from_report:
        rep = json.loads(Path(args.from_report).read_text("utf-8"))
        ids += [r["krithi_id"] for r in rep if r["classification"] == "resplit-to-template"]
    ids = list(dict.fromkeys(ids))
    if not ids:
        ap.error("provide --krithi or --from-report")

    token = _token() if args.apply else None
    written = skipped = 0
    with psycopg.connect(DB_URL) as conn:
        for kid in ids:
            res = triage(conn, kid, HtmlTextExtractor(), StructureParser())
            if res.classification != "resplit-to-template":
                continue
            if res.stored_counts.get(args.lang) == 0:
                continue
            template = _template_sections(conn, kid)
            if res.stored_counts.get(args.lang) == len(template):
                continue  # already matches — nothing to do
            vid = _variant_ids(conn, kid).get(args.lang)
            if not vid:
                continue
            stored_blob = _stored_blob(conn, vid)
            secs, reason = _assess(res, args.lang, template, stored_blob)
            if secs is None:
                if reason and "no trailer removed" not in reason:
                    print(f"SKIP {res.title[:32]:32} — {reason}")
                    skipped += 1
                continue
            sid_by_order = {oi: sid for oi, _, sid in template}
            payload = {"sections": [{"sectionId": sid_by_order[i + 1], "text": s.text}
                                    for i, s in enumerate(secs)]}
            trailer_note = f"(last {len(secs[-1].text)}ch, was trailer-laden)"
            if args.apply:
                resp = _request("POST", f"{API}/v1/admin/variants/{vid}/sections",
                                headers={"Authorization": f"Bearer {token}"}, json=payload)
                print(f"{res.title[:32]:32} {args.lang}: {len(secs)} secs -> HTTP {resp.status_code} {trailer_note}")
            else:
                print(f"{res.title[:32]:32} {args.lang}: {len(secs)} secs -> DRY-RUN {trailer_note}")
            written += 1

    print(f"\n{'APPLIED' if args.apply else 'DRY-RUN'}: {written} Tamil trailer re-splits, {skipped} skipped")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
