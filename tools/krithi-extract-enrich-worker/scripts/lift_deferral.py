#!/usr/bin/env python3
"""Lift `application: deferred` on approved Syama Sastri claims once their application path exists.

    uv run python scripts/lift_deferral.py --reason "TRACK-148 D1 path built" SS-0032:mudra:1 SS-0034:mudra:1

Only the application mode changes; the reviewer, date and note stay. Regenerate with
generate_classification_master.py afterwards.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.lead_decisions import lift_deferral_raw_syama

MASTER = Path(__file__).resolve().parents[3] / "database" / "data" / "track146-syama-sastri-master.json"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reason", required=True)
    parser.add_argument("claims", nargs="+")
    args = parser.parse_args()
    raw = json.loads(MASTER.read_text(encoding="utf-8"))
    n = lift_deferral_raw_syama(raw, args.claims, args.reason)
    MASTER.write_text(json.dumps(raw, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Lifted the deferral on {n} claims in {MASTER.name}")


if __name__ == "__main__":
    main()
