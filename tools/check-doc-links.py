#!/usr/bin/env python3
"""Report Markdown relative links whose target does not exist.

Documentation rots quietly: a file moves, the links pointing at it keep rendering
fine on GitHub (they just 404 on click), and nobody notices until someone follows
one. This walks every tracked `.md` file and resolves each relative link against
the filesystem.

Usage:
    python3 tools/check-doc-links.py          # report, exit 1 if any are broken
    python3 tools/check-doc-links.py --quiet  # exit code only

What is deliberately NOT checked:
  * external URLs (http/https) — needs network, and flaky third parties would make
    this useless as a gate
  * bare/absolute paths — the repo uses relative links by convention
  * anything inside a fenced block or an inline code span — those render as literal
    text, so they are examples, not references. TRACK-002 documents a header format
    containing a link placeholder; writing about link syntax must stay possible.

Line suffixes (`file.kt:120-140`) and anchors (`doc.md#section`) are stripped
before resolving; the file must exist, the line range and anchor are not verified.
"""
from __future__ import annotations

import os
import re
import sys

SKIP_DIRS = {
    ".git", ".gradle", ".venv", "node_modules", "build", "dist",
    "ds-bundle", ".pytest_cache", ".triage-cache", ".design-sync",
}

LINK_RE = re.compile(r"\]\((\.\.?/[^)\s]+)\)")
FENCE_RE = re.compile(r"^```.*?^```", re.S | re.M)
# Inline code spans: `like this` and ``with `backticks` inside``.
INLINE_CODE_RE = re.compile(r"(?<!`)(`+)(?!`).+?(?<!`)\1(?!`)", re.S)
SUFFIX_RE = re.compile(r":[0-9]+(?:-[0-9]+)?$")


def literal_spans(text: str) -> list[tuple[int, int]]:
    """Char ranges rendered as literal text, where a link is an example not a reference.

    Covers fenced blocks and inline code spans. A markdown link inside backticks renders
    as characters, never as something clickable, so it cannot be broken — flagging it
    would force docs to avoid writing about link syntax at all.
    """
    return [(m.start(), m.end()) for m in FENCE_RE.finditer(text)] + [
        (m.start(), m.end()) for m in INLINE_CODE_RE.finditer(text)
    ]


def broken_links(root_dir: str = ".") -> list[tuple[str, str]]:
    broken: list[tuple[str, str]] = []
    for root, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for name in files:
            if not name.endswith(".md"):
                continue
            path = os.path.normpath(os.path.join(root, name))
            try:
                text = open(path, encoding="utf-8", errors="ignore").read()
            except OSError:
                continue
            spans = literal_spans(text)
            for m in LINK_RE.finditer(text):
                if any(a <= m.start() < b for a, b in spans):
                    continue  # inside code — an example or template, not a reference
                link = m.group(1)
                target = SUFFIX_RE.sub("", link.split("#")[0]).replace("%20", " ")
                if not os.path.exists(os.path.normpath(os.path.join(root, target))):
                    broken.append((path, link))
    return broken


def main() -> int:
    quiet = "--quiet" in sys.argv
    broken = broken_links()
    if not broken:
        if not quiet:
            print("doc links OK — every relative Markdown link resolves")
        return 0
    if not quiet:
        print(f"{len(broken)} broken relative link(s):\n")
        for path, link in sorted(broken):
            print(f"  {path}\n      -> {link}")
        print(
            "\nEach link points at a path that does not exist. Usually the target moved: "
            "find where it lives now and update the link, or drop it if the file is gone."
            "\n\nIf you are deliberately referring to a file that does not exist yet, do not "
            "link it — write it as inline code (`path/to/file.kt`) and turn it into a link in "
            "the change that creates the file. A link is a promise that clicking it works."
        )
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
