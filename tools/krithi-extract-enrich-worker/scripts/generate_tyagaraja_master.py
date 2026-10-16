#!/usr/bin/env python3
"""Validate the Tyagaraja classification master, render its concordance, and emit its manifest.

The command does not open a database connection and does not write claims back
to PostgreSQL.

Usage:
  uv run python scripts/generate_tyagaraja_master.py \
    --master ../../database/data/track147-tyagaraja-master.json \
    --output ../../application_documentation/01-requirements/features/tyagaraja-classification-master.md \
    --manifest ../../database/data/track147-tyagaraja-application-manifest.json \
    --check
"""

from __future__ import annotations

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from pathlib import Path

from src.tyagaraja_classification import (
    assert_track147_census,
    build_application_manifest,
    load_master,
    render_markdown,
    verify_source,
)


def main() -> None:
    parser = argparse.ArgumentParser(description="Render the TRACK-147 Tyagaraja classification concordance.")
    parser.add_argument("--master", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--manifest", type=Path, help="Optional path to application manifest JSON.")
    parser.add_argument("--check", action="store_true", help="Fail if rendered outputs differ from disk.")
    args = parser.parse_args()

    master = load_master(args.master)
    assert_track147_census(master)
    verify_source(master, Path(__file__).resolve().parents[3])
    rendered = render_markdown(master)
    outputs: list[tuple[Path, str]] = [(args.output, rendered)]
    if args.manifest is not None:
        manifest = build_application_manifest(master)
        manifest_json = json.dumps(manifest.model_dump(mode="json"), indent=2, ensure_ascii=False) + "\n"
        outputs.append((args.manifest, manifest_json))

    for path, content in outputs:
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                raise SystemExit(f"generated output differs from master: {path}")
        else:
            path.write_text(content, encoding="utf-8")


if __name__ == "__main__":
    main()
