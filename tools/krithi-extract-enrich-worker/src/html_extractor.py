"""HTML text extraction utilities for canonical extraction.

This mirrors the Kotlin HtmlTextExtractor behavior:
1. Remove boilerplate nodes (script/style/nav/footer/etc.)
2. Prefer known content selectors (post-body/article/post-content)
3. Preserve lightweight structure (paragraph breaks + links)
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from urllib.parse import urljoin

from bs4 import BeautifulSoup, NavigableString, Tag


@dataclass
class ExtractedHtmlContent:
    """Structured text extracted from an HTML page."""

    text: str
    title: str | None = None


class HtmlTextExtractor:
    """Extract readable, structure-preserving text from HTML."""

    def __init__(
        self,
        max_chars: int = 120_000,
        selectors: list[str] | None = None,
    ) -> None:
        self.max_chars = max_chars
        self.selectors = selectors or [
            "div.post-body",
            "div.post",
            "article",
            "div.post-content",
        ]
        self._block_tags = {
            "p",
            "li",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "pre",
            "blockquote",
        }

    def extract(self, html: str, base_url: str | None = None) -> ExtractedHtmlContent:
        """Extract content text and title from an HTML document."""
        soup = BeautifulSoup(html, "html.parser")

        for node in soup.select("script, style, nav, footer, header, form, aside, noscript"):
            node.decompose()

        main_element: Tag = soup.body or soup
        for selector in self.selectors:
            candidate = soup.select_one(selector)
            if candidate and candidate.get_text(strip=True):
                main_element = candidate
                break

        title = None
        if soup.title:
            title_text = soup.title.get_text(strip=True)
            if title_text:
                title = title_text

        raw_text = self._extract_structured_text(main_element, base_url=base_url)
        cleaned = self._normalize_text(raw_text)
        trimmed = cleaned[: self.max_chars] if len(cleaned) > self.max_chars else cleaned

        return ExtractedHtmlContent(text=trimmed, title=title)

    def _extract_structured_text(self, root: Tag, base_url: str | None = None) -> str:
        chunks: list[str] = []

        def last_char() -> str | None:
            if not chunks:
                return None
            return chunks[-1][-1] if chunks[-1] else None

        def append_newline() -> None:
            if chunks and last_char() != "\n":
                chunks.append("\n")

        def append_text(text: str) -> None:
            normalized = text.strip()
            if not normalized:
                return
            if chunks and last_char() not in {"\n", " "}:
                chunks.append(" ")
            chunks.append(normalized)

        def walk(node: Tag | NavigableString) -> None:
            if isinstance(node, NavigableString):
                append_text(str(node))
                return

            if not isinstance(node, Tag):
                return

            tag = node.name.lower() if node.name else ""
            is_block = tag in self._block_tags
            if is_block:
                append_newline()

            if tag == "br":
                append_newline()
            elif tag == "a":
                link_text = node.get_text(" ", strip=True)
                href = (node.get("href") or "").strip()
                absolute = urljoin(base_url, href) if href and base_url else href
                if link_text:
                    append_text(f"{link_text} ({absolute})" if absolute else link_text)
            else:
                for child in node.children:
                    walk(child)

            if is_block:
                append_newline()

        walk(root)
        return "".join(chunks)

    def _normalize_text(self, raw: str) -> str:
        return (
            re.sub(r"\n\s*\n+", "\n\n", re.sub(r" +", " ", re.sub(r"[\t\u000B\u000C\r]+", " ", raw)))
            .strip()
        )
