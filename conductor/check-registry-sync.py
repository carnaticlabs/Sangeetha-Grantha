#!/usr/bin/env python3
"""Conductor Registry ↔ Track File sync checker.

Verifies three-way truth between conductor/tracks.md registry rows,
individual TRACK-XXX-*.md file statuses, and file existence.

Exit codes:
  0 — all checks pass
  1 — drift detected (details printed to stderr)

Usage:
  python3 conductor/check-registry-sync.py
"""
import re
import os
import sys

TRACKS_DIR = os.path.join(os.path.dirname(__file__), "tracks")
REGISTRY_FILE = os.path.join(os.path.dirname(__file__), "tracks.md")

STATUS_ALIASES = {
    "Complete": "Completed",
    "Done": "Completed",
}


def parse_registry():
    registry = {}
    deprecated = set()
    in_deprecated = False
    with open(REGISTRY_FILE) as f:
        for line in f:
            if "Deprecated" in line and "##" in line:
                in_deprecated = True
            m = re.match(r"\|\s*\[TRACK-(\d+)\].*?\|\s*(.*?)\s*\|\s*(.*?)\s*\|", line)
            if m:
                tid = int(m.group(1))
                registry[tid] = m.group(3).strip()
                if in_deprecated:
                    deprecated.add(tid)
            if in_deprecated:
                dm = re.search(r"TRACK-(\d+)", line)
                if dm:
                    deprecated.add(int(dm.group(1)))
    return registry, deprecated


def parse_file_status(path):
    with open(path) as f:
        content = f.read(2000)
    m = re.search(r"\*\*Status\*\*\s*\|\s*\**(.*?)\**\s*\|", content)
    if m:
        return m.group(1).strip()
    m = re.search(r"\*\*Status:\*\*\s*(.*)", content)
    if m:
        return m.group(1).strip()
    m = re.search(r"##\s*Status:\s*(.*)", content)
    if m:
        return m.group(1).strip()
    return None


def normalise(status):
    if status is None:
        return None
    s = status.strip().rstrip(".")
    return STATUS_ALIASES.get(s, s)


def main():
    registry, deprecated = parse_registry()
    errors = []

    file_ids = set()
    for fn in os.listdir(TRACKS_DIR):
        m = re.match(r"TRACK-(\d+)", fn)
        if not m:
            continue
        tid = int(m.group(1))
        file_ids.add(tid)
        path = os.path.join(TRACKS_DIR, fn)
        file_status = normalise(parse_file_status(path))
        reg_status = normalise(registry.get(tid))

        if tid not in registry and tid not in deprecated:
            errors.append(f"TRACK-{tid:03d}: file exists but NO registry row (orphan file)")
        elif reg_status and file_status and reg_status != file_status:
            errors.append(
                f"TRACK-{tid:03d}: registry=\"{registry[tid]}\" vs file=\"{file_status}\""
            )

    for tid in sorted(registry.keys()):
        if tid not in file_ids and tid not in deprecated:
            errors.append(f"TRACK-{tid:03d}: registry row exists but NO file (orphan row)")

    if errors:
        print("Registry/file sync errors:", file=sys.stderr)
        for e in errors:
            print(f"  {e}", file=sys.stderr)
        return 1

    print(f"OK: {len(file_ids)} track files, {len(registry)} registry rows — all in sync.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
