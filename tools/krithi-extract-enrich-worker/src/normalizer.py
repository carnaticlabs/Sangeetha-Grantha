"""Consolidated normalization helpers for extraction-time matching keys."""

from __future__ import annotations

import re
import unicodedata

from .diacritic_normalizer import normalize_garbled_diacritics as _legacy_normalize_garbled_diacritics
from .heuristics.transliteration_collapse import MATCHING_COLLAPSE_RULES, apply_collapse

_HONORIFIC_RE = re.compile(r"\b(?:saint|sri|swami|sir|dr|prof|smt)\b", re.IGNORECASE)
_NON_ALNUM_SPACE_RE = re.compile(r"[^a-z0-9\s]")
_WS_RE = re.compile(r"\s+")

_TRAILING_ANUSVARA_RE = re.compile(r"(?<=[aeiou])m\b")
_EPENTHETIC_I_RE = re.compile(r"([kgcjtdpbnmrlvs])i(?=[kgcjtdpbnmrlvs])")
_TITLE_VOWEL_BRIDGE_RE = re.compile(r"([kgcjtdpbnmrlvs])[ae](?=[kgcjtdpbnmrlvs])")
_DOUBLE_CONSONANT_RE = re.compile(r"([bcdfghjklmnpqrstvwxyz])\1+")

_DEITY_PREFIX_RE = re.compile(r"^(?:(?:lord|goddess|sri|arulmigu)\s+)+")
_TEMPLE_PREFIX_RE = re.compile(r"^(?:(?:sri|arulmigu|tiru)\s+)+")


def _strip_diacritics(text: str) -> str:
    return "".join(char for char in unicodedata.normalize("NFD", text) if unicodedata.category(char) != "Mn")


def normalize_for_matching(text: str, entity_type: str = "title") -> str:
    """Produce a canonical matching key for deduplication and variant matching."""
    if not text:
        return ""

    # 1. NFD decomposition + strip combining marks
    result = _strip_diacritics(text)
    # 2. Lowercase
    result = result.lower()
    # 3. Strip honorific prefixes
    result = _HONORIFIC_RE.sub(" ", result)
    # 4. Remove special characters (alphanumeric + space only)
    result = _NON_ALNUM_SPACE_RE.sub(" ", result)
    # 5. Transliteration collapse (longest match first; includes the `kh`
    #    aspirate smoothing the existing matching data depends on).
    result = apply_collapse(result, MATCHING_COLLAPSE_RULES)

    # 6. Devanagari-aware rules + robustness for Sanskrit title matching.
    if entity_type == "title":
        result = _TRAILING_ANUSVARA_RE.sub("", result)
        result = _EPENTHETIC_I_RE.sub(r"\1", result)
        result = result.replace("ndar", "ndr")
        result = _TITLE_VOWEL_BRIDGE_RE.sub(r"\1", result)
        result = result.replace("ngr", "nr")
        result = _DOUBLE_CONSONANT_RE.sub(r"\1", result)

    # 7. Entity-specific rules
    if entity_type == "composer":
        if result in {"tyagaraja", "tyagarajar"}:
            result = "tyagaraja"
        elif "diksitar" in result:
            result = "muttusvami diksitar"
        elif result in {"syama sastri", "syama sastry"}:
            result = "syama sastri"
    elif entity_type == "raga":
        result = result.replace("aa", "a").replace("ee", "i").replace("oo", "o").replace("uu", "u").replace(" ", "")
    elif entity_type == "tala":
        if result.endswith("am") and len(result) > 4:
            result = f"{result[:-2]}a"
            if result.endswith("am"):
                result = result[:-1]
        if result == "desadi" or result == "madyadi":
            result = "adi"
    elif entity_type == "deity":
        result = _DEITY_PREFIX_RE.sub("", result)
    elif entity_type == "temple":
        result = _TEMPLE_PREFIX_RE.sub("", result)

    # 8. Collapse whitespace + trim
    result = _WS_RE.sub(" ", result).strip()
    return result


def normalize_garbled_diacritics(text: str) -> str:
    """Keep the existing garbled diacritic cleanup from diacritic_normalizer.py."""
    return _legacy_normalize_garbled_diacritics(text)
