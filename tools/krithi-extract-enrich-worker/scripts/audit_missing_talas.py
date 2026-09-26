#!/usr/bin/env python3
"""Fetch evidence and write a review manifest; no database connection or mutation.

Run from the worker: python scripts/audit_missing_talas.py --inventory INPUT --output OUTPUT --cache CACHE
Reruns use cached pages. --offline fails individual missing pages without network calls.
"""

from __future__ import annotations

import argparse
import asyncio
import hashlib
import json
import sys
from collections import Counter
from pathlib import Path
from urllib.parse import urlsplit

import httpx

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from src.tala_audit import Inventory, InventoryRow, TalaAuditor, TalaCandidate

ALLOWED_HOSTS = {
    "thyagaraja-vaibhavam.blogspot.com",
    "guru-guha.blogspot.com",
    "syamakrishnavaibhavam.blogspot.com",
}


class SourceCache:
    def __init__(self, directory: Path, offline: bool) -> None:
        self.directory = directory
        self.directory.mkdir(parents=True, exist_ok=True)
        self.offline = offline
        self.semaphore = asyncio.Semaphore(2)

    async def read(self, client: httpx.AsyncClient, source_url: str) -> str:
        url = source_url.replace("http://", "https://", 1)
        parsed = urlsplit(url)
        if parsed.scheme != "https" or parsed.hostname not in ALLOWED_HOSTS or parsed.username or parsed.port:
            raise ValueError("Source URL is outside the reviewed blog hosts")
        key = hashlib.sha256(url.encode()).hexdigest()
        path = self.directory / f"{key}.html"
        if path.exists():
            return path.read_text()
        if self.offline:
            raise FileNotFoundError("Source is not cached")
        async with self.semaphore:
            # No redirects to unreviewed hosts; one bounded failure is visible in the manifest.
            response = await client.get(url, follow_redirects=False)
            response.raise_for_status()
            if len(response.content) > 2_000_000 or "html" not in response.headers.get("content-type", ""):
                raise ValueError("Source is not a bounded HTML document")
            html = response.text
            path.write_text(html)
            await asyncio.sleep(0.25)
            return html


async def run(inventory: Inventory, output: Path, cache: SourceCache) -> list[TalaCandidate]:
    auditor = TalaAuditor()
    results: list[TalaCandidate] = []
    output.parent.mkdir(parents=True, exist_ok=True)
    async with httpx.AsyncClient(timeout=20, headers={"User-Agent": "SangitaGrantha-source-audit/1.0"}) as client:

        async def examine(row: InventoryRow, url: str) -> TalaCandidate:
            if not url:
                return TalaCandidate(
                    krithi_id=row.id,
                    title=row.title,
                    composer=row.composer,
                    catalogue_raga=row.raga,
                    source_url="",
                    status="source_missing",
                    notes=["No evidence URL; an independently identified source is required."],
                )
            try:
                return auditor.examine(row, url, await cache.read(client, url))
            except Exception as exc:
                return TalaCandidate(
                    krithi_id=row.id,
                    title=row.title,
                    composer=row.composer,
                    catalogue_raga=row.raga,
                    source_url=url,
                    status="fetch_failed",
                    notes=[f"{type(exc).__name__}: {exc}"],
                )

        tasks = [
            examine(row, url) for row in inventory.rows for url in sorted({e.source_url for e in row.evidence} or {""})
        ]
        if not tasks:
            output.write_text('{"complete": true, "source_count": 0, "results": []}\n')
        for task in asyncio.as_completed(tasks):
            results.append(await task)
            # Durable partial progress, including failures, after every page.
            temporary = output.with_suffix(output.suffix + ".tmp")
            temporary.write_text(
                json.dumps(
                    {
                        "complete": len(results) == len(tasks),
                        "source_count": len(tasks),
                        "results": [
                            r.model_dump(mode="json")
                            for r in sorted(results, key=lambda r: (str(r.krithi_id), r.source_url))
                        ],
                    },
                    ensure_ascii=False,
                    indent=2,
                )
                + "\n"
            )
            temporary.replace(output)
            if len(results) % 25 == 0:
                print(f"Audited {len(results)}/{len(tasks)} sources", flush=True)
    return results


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    inventory = Inventory.model_validate_json(args.inventory.read_text())
    results = asyncio.run(run(inventory, args.output, SourceCache(args.cache, args.offline)))
    print(json.dumps(Counter(r.status for r in results), sort_keys=True))
    if any(r.status == "fetch_failed" for r in results):
        sys.exit(1)


if __name__ == "__main__":
    main()
