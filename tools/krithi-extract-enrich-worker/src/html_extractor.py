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

        # TRACK-097: Strip navigation tables from Carnatic music blog pages.
        # These contain language-switch links (English | Devanagari | Tamil | ...)
        # that pollute extracted text with non-lyric content.
        self._strip_navigation_tables(soup)

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
                # TRACK-097: Skip navigation/boilerplate links that
                # produce false metadata boundaries (e.g. "Meaning of Kriti-1")
                if link_text and self._is_navigation_link(link_text):
                    pass  # silently drop
                elif link_text:
                    href = (node.get("href") or "").strip()
                    # TRACK-097: For inline links within lyrics (e.g. variation
                    # references like <a href="#V1">mAyUra nAtha</a>), output
                    # only the link text — the URL is noise in lyric content.
                    # Only include URL for truly external links.
                    is_fragment = href.startswith("#")
                    is_same_page = href.startswith(base_url or "") if base_url else False
                    if is_fragment or is_same_page:
                        append_text(link_text)
                    else:
                        absolute = urljoin(base_url, href) if href and base_url else href
                        if absolute:
                            append_text(f"{link_text} ({absolute})")
                        else:
                            append_text(link_text)
            else:
                for child in node.children:
                    walk(child)

            if is_block:
                append_newline()

        walk(root)
        return "".join(chunks)

    # ─── TRACK-097: Navigation / boilerplate link patterns ────────────────
    _NAV_LINK_PATTERN = re.compile(
        r"^(?:meaning\s+of\s+kriti|"
        r"notation|"
        r"click\s+here|"
        r"back\s+to|"
        r"home\s*page|"
        r"pronunciation\s+guide|"
        r"word\s+by\s+word\s+meaning)",
        re.IGNORECASE,
    )

    _LANGUAGE_NAV_LABELS = {
        "english", "devanagari", "tamil", "telugu", "kannada", "malayalam",
        "sanskrit", "hindi", "word division",
    }

    def _strip_navigation_tables(self, soup: BeautifulSoup) -> None:
        """Remove <table> elements that serve as language-switch navigation bars.

        Guru-guha.blogspot.com (and similar Carnatic music blogs) embed per-section
        navigation as <table> rows with links to language anchors. These produce
        lines like "English | Devanagari | Tamil | ..." that pollute the extracted
        text and can cause false metadata-boundary matches.

        A table is considered "navigation" if ≥50% of its cell text consists of
        known language labels or the table id matches a language label.
        """
        for table in soup.find_all("table"):
            # Fast path: table id matches a language label (guru-guha pattern)
            table_id = (table.get("id") or "").strip().lower()
            if table_id in self._LANGUAGE_NAV_LABELS:
                table.decompose()
                continue

            # Heuristic: if most cells are language labels, it's navigation
            cells = table.find_all("td")
            if not cells:
                continue
            nav_count = sum(
                1 for cell in cells
                if cell.get_text(strip=True).lower() in self._LANGUAGE_NAV_LABELS
            )
            if len(cells) > 0 and nav_count / len(cells) >= 0.5:
                table.decompose()

    def _is_navigation_link(self, link_text: str) -> bool:
        """Return True if a link's text is navigation boilerplate, not lyric content."""
        return bool(self._NAV_LINK_PATTERN.match(link_text.strip()))

    def _normalize_text(self, raw: str) -> str:
        return (
            re.sub(r"\n\s*\n+", "\n\n", re.sub(r" +", " ", re.sub(r"[\t\u000B\u000C\r]+", " ", raw)))
            .strip()
        )
