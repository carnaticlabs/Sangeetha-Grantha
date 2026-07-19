"""Metadata heuristics: composer inference from URLs and segment-title validation.

These are last-resort, source-knowledge heuristics kept separate from the
extraction pipeline so they can be audited and extended in one place.
"""

from __future__ import annotations

import unicodedata

# ---------------------------------------------------------------------------
# URL-based composer inference for well-known Carnatic music blog sources.
# Used as a last-resort fallback when neither the HTML content nor the
# request payload provide a composer name.
# ---------------------------------------------------------------------------
URL_COMPOSER_MAP: list[tuple[str, str]] = [
    ("guru-guha.blogspot", "Muthuswami Dikshitar"),
    ("syamakrishnavaibhavam.blogspot", "Syama Sastri"),
    ("thyagaraja-vaibhavam.blogspot", "Tyagaraja"),
]


def infer_composer_from_url(url: str) -> str | None:
    """Infer composer name from well-known Carnatic music blog URL patterns."""
    lower = url.lower()
    for pattern, composer in URL_COMPOSER_MAP:
        if pattern in lower:
            return composer
    return None


def is_valid_segment_title(title: str) -> bool:
    """Check if title is likely a section header or metadata artifact."""
    if not title:
        return False

    normalized = title.lower().strip()
    # Remove diacritics
    normalized = "".join(c for c in unicodedata.normalize("NFD", normalized) if unicodedata.category(c) != "Mn")
    # Remove common separators or noise
    normalized = normalized.replace("-", "").replace(" ", "")

    # Blocklist of known section headers often mistaken for titles
    # Includes variations like 'madhyamakālasāhityam' -> 'madhyamakalasahityam'
    # (assuming heavy normalization happens or we check substrings)

    # Basic exact checks on normalized string
    invalid_exact = {
        "pallavi",
        "anupallavi",
        "charanam",
        "caranam",
        "samasticharanam",
        "samasticaranam",
        "madhyamakalasahityam",
        "madhyamakalasahitya",
        "cittaswara",
        "cittaswaram",
        "cittam",
        "chittaswara",
        "chittaswaram",
        "chittam",
        "muktayiswara",
        "muktayiswaram",
        "sahityam",
        "sahitya",
        "swaram",
        "swara",
        "meaningofkriti",
        "englishtranslation",
        "devanagari",
        "tamil",
        "telugu",
        "kannada",
        "malayalam",
    }

    if normalized in invalid_exact:
        return False

    # Check for specific prefixes/substrings that indicate metadata dumps
    if normalized.startswith("meaningof"):
        return False
    if "translation" in normalized:
        return False

    # Check specifically for "madhyama" + "kala" combination which is common
    if "madhyama" in normalized and "kala" in normalized:
        return False

    return True
