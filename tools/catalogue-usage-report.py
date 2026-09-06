#!/usr/bin/env python3
"""Aggregate TRACK-138 catalogue usage JSON lines into a visit report.

Input records are one CatalogueUsageEvent JSON object per line. Deduplicate by
eventId. Do not treat sessions as unique people.
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from typing import Any


def load_events(raw_lines: list[str]) -> list[dict[str, Any]]:
    seen: set[str] = set()
    events: list[dict[str, Any]] = []
    for line in raw_lines:
        text = line.strip()
        if not text:
            continue
        try:
            payload = json.loads(text)
        except json.JSONDecodeError:
            continue
        event_id = payload.get("eventId")
        if not event_id or event_id in seen:
            continue
        seen.add(event_id)
        events.append(payload)
    return events


def report(events: list[dict[str, Any]]) -> dict[str, Any]:
    attempts = len(events)
    searches = [
        e
        for e in events
        if e.get("action") == "search" and e.get("status") == 200
    ]
    views = [
        e
        for e in events
        if e.get("action") == "reader" and e.get("status") == 200
    ]
    zero_result = sum(1 for e in searches if e.get("resultCount") == 0)
    sessions = {e.get("sessionId") for e in events if e.get("sessionId")}
    actions = Counter(str(e.get("action") or "other") for e in events)
    return {
        "attempts": attempts,
        "completedSearches": len(searches),
        "zeroResultSearches": zero_result,
        "readerViews": len(views),
        "approximateSessions": len(sessions),
        "sessionsAreNotUniquePeople": True,
        "actions": dict(actions),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input",
        help="JSONL file of catalogue usage events (default: stdin)",
    )
    args = parser.parse_args(argv)
    if args.input:
        with open(args.input, encoding="utf-8") as handle:
            lines = handle.readlines()
    else:
        lines = sys.stdin.readlines()
    json.dump(report(load_events(lines)), sys.stdout, indent=2)
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
