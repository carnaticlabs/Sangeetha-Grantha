#!/usr/bin/env python3
"""Validate and generate an offline musicological concordance and approval manifest."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from src.musicological_classification import load_master


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--master", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    master = load_master(args.master)
    master.verify_source(Path(__file__).resolve().parents[3])
    outputs = [(args.output, master.render())]
    if args.manifest:
        outputs.append((args.manifest, json.dumps(master.application_manifest(), indent=2, ensure_ascii=False) + "\n"))
    for path, content in outputs:
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                raise SystemExit(f"generated output differs: {path}")
        else:
            path.write_text(content, encoding="utf-8")
    print(f"Validated {len(master.rows)} rows, {len(master.compositions)} identities, {len(master.claims)} claims")


if __name__ == "__main__":
    main()
