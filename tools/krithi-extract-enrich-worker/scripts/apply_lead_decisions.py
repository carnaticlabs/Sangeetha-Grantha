#!/usr/bin/env python3
"""Record a named reviewer's decisions on TRACK-148 / TRACK-149 leads.

Turns a proposal file into a decision file made by --reviewer. Claims named with --except get no decision and
stay unreviewed. For Syama Sastri the master JSON is updated in place; for Tyagaraja the decision file is read
by populate_tyagaraja_master.py, so run that next.

    uv run python scripts/apply_lead_decisions.py --composer syama --reviewer "Name" --accepted-at 2026-11-04
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.lead_decisions import apply_to_raw_syama, decisions_from_proposals

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "database" / "data"
PATHS = {
    "syama": (
        DATA / "track148-lead-proposals.json",
        DATA / "track148-lead-decisions.json",
        DATA / "track146-syama-sastri-master.json",
    ),
    "tyagaraja": (DATA / "track149-lead-proposals.json", DATA / "track149-lead-decisions.json", None),
}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--composer", choices=sorted(PATHS), required=True)
    parser.add_argument("--reviewer", required=True, help="Name of the human who made the decisions")
    parser.add_argument("--accepted-at", required=True)
    parser.add_argument("--except", dest="excluded", nargs="*", default=[], help="Claim ids left unreviewed")
    args = parser.parse_args()

    proposals_path, decisions_path, master_path = PATHS[args.composer]
    proposals = json.loads(proposals_path.read_text(encoding="utf-8"))
    decisions = decisions_from_proposals(proposals, args.reviewer, args.accepted_at, set(args.excluded))
    decisions_path.write_text(decisions.model_dump_json(indent=2, exclude_none=True) + "\n", encoding="utf-8")
    print(f"Wrote {len(decisions.decisions)} decisions to {decisions_path.relative_to(ROOT)}")
    if master_path is not None:
        raw = json.loads(master_path.read_text(encoding="utf-8"))
        apply_to_raw_syama(raw, decisions)
        master_path.write_text(json.dumps(raw, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Updated {master_path.relative_to(ROOT)}; now run generate_classification_master.py")


if __name__ == "__main__":
    main()
