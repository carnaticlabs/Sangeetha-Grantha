#!/usr/bin/env python3
"""Validate the Tyagaraja classification master and render its concordance.

The command does not open a database connection and does not write claims back
to PostgreSQL.

Usage:
  uv run python scripts/generate_tyagaraja_master.py \
    --master ../../database/data/track147-tyagaraja-master.json \
    --output ../../application_documentation/01-requirements/features/tyagaraja-classification-master.md \
    --check
"""

from __future__ import annotations

import argparse
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from pathlib import Path

from src.tyagaraja_classification import assert_track147_census, load_master, render_markdown, verify_source


def main() -> None:
    parser = argparse.ArgumentParser(description="Render the TRACK-147 Tyagaraja classification concordance.")
    parser.add_argument("--master", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--check", action="store_true", help="Fail if the rendered markdown differs from --output.")
    args = parser.parse_args()

    master = load_master(args.master)
    assert_track147_census(master)
    verify_source(master, Path(__file__).resolve().parents[3])
    rendered = render_markdown(master)
    if args.check:
        existing = args.output.read_text(encoding="utf-8")
        if existing != rendered:
            raise SystemExit("generated markdown differs from the master")
        return
    args.output.write_text(rendered, encoding="utf-8")


if __name__ == "__main__":
    main()
