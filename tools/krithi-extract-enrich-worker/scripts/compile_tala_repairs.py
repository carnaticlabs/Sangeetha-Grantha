#!/usr/bin/env python3
"""Compile a reviewed manifest to a NEW SQL file; never execute SQL or overwrite a migration."""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from src.tala_repair import TalaRepairManifest, compile_migration


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    manifest = TalaRepairManifest.model_validate_json(args.manifest.read_text())
    with args.output.open("x") as output:
        output.write(compile_migration(manifest, args.manifest.name))
    print(f"Wrote {len(manifest.rows)} reviewed repairs to {args.output}")


if __name__ == "__main__":
    main()
