"""Garbled diacritic normalisation for PDF-extracted IAST text.

PDF fonts (e.g. Utopia in guruguha.org PDFs) encode IAST diacritics as
standalone combining characters rather than precomposed Unicode codepoints.
PyMuPDF extracts these as separate characters, often with intervening
whitespace.  This module converts garbled sequences back to proper IAST
Unicode.

Examples:
    r¯aga ˙m  →  rāgaṁ
    t¯al.a ˙m →  tāḷaṁ
    ¯adi       →  ādi
    mi´sra     →  miśra
    n¯ıl¯ambari → nīlāmbari
    raks.a     →  rakṣa
    n. d. e    →  ṇḍe
"""

from __future__ import annotations

import re


# ─── Normalisation rules (Section 4.1 of spec) ──────────────────────────
#
# Rules 1–7 are safe for broad application.  Rule 8 (consonant + dot) is
# handled via permissive regex in the parsers, not here.
#
# Each tuple: (compiled regex, replacement string)

_RULES: list[tuple[re.Pattern[str], str]] = [
    # 1. macron (¯) + optional space + a  →  ā
    (re.compile(r"\u00AF\s*a"), "ā"),
    # 2. macron (¯) + optional space + i or dotless-i (ı)  →  ī
    (re.compile(r"\u00AF\s*[iı]"), "ī"),
    # 3. macron (¯) + optional space + u  →  ū
    (re.compile(r"\u00AF\s*u"), "ū"),
    # 4. optional space + dot-above (˙) + optional space + m  →  ṁ
    (re.compile(r"\s*\u02D9\s*m"), "ṁ"),
    # 5. optional space + dot-above (˙) + optional space + n  →  ṅ
    (re.compile(r"\s*\u02D9\s*n"), "ṅ"),
    # 5b. optional space + dot-above (˙) + optional space + r  →  ṛ
    (re.compile(r"\s*\u02D9\s*r"), "ṛ"),
    # 6. acute (´) + optional space + s  →  ś
    (re.compile(r"\u00B4\s*s"), "ś"),
    # 7. tilde (˜) + optional space + n  →  ñ
    (re.compile(r"\u02DC\s*n"), "ñ"),
    # 8. Maltese Cross (✠) → strip (PDF artifact)
    (re.compile(r"\u2720"), ""),
    # 9. Trailing numbers at end of lines (Page/Song indices)
    (re.compile(r"\n\s*\d+\s*$"), "\n"),
    # 10. dotless-i (ı) → i (fallback)
    (re.compile(r"\u0131"), "i"),
]

# ─── Rule 8: Consonant-dot patterns (Section 4.1, previously skipped) ────
#
# In garbled Utopia PDF encoding, retroflex/special consonants are rendered
# as base consonant + dot-below (represented as period).  The dot may be
# followed by optional whitespace before the next character.
#
# We process these AFTER rules 1–7 so that combined forms like t¯al.a
# (tāḷa) have the macron resolved first, then the dot.
#
# Order matters: longer patterns first to avoid partial matches.

_CONSONANT_DOT_RULES: list[tuple[re.Pattern[str], str]] = [
    # Each pattern uses a negative lookbehind for uppercase letters to
    # prevent false positives on abbreviations like "Dr.", "Sr.", "Mr.".
    # Sanskrit/IAST text uses lowercase; abbreviations use Title/UPPER case.
    #
    # s. + optional space  →  ṣ  (retroflex sibilant)
    (re.compile(r"(?<![A-Z])s\.\s*"), "ṣ"),
    # n. + optional space  →  ṇ  (retroflex nasal)
    (re.compile(r"(?<![A-Z])n\.\s*"), "ṇ"),
    # d. + optional space  →  ḍ  (retroflex stop)
    (re.compile(r"(?<![A-Z])d\.\s*"), "ḍ"),
    # t. + optional space  →  ṭ  (retroflex stop)
    (re.compile(r"(?<![A-Z])t\.\s*"), "ṭ"),
    # l. + optional space  →  ḷ  (retroflex lateral)
    (re.compile(r"(?<![A-Z])l\.\s*"), "ḷ"),
    # r. + optional space  →  ṛ  (vocalic r)
    (re.compile(r"(?<![A-Z])r\.\s*"), "ṛ"),
    # h. + optional space  →  ḥ  (visarga)
    (re.compile(r"(?<![A-Z])h\.\s*"), "ḥ"),
]


def normalize_garbled_diacritics(text: str) -> str:
    """Convert garbled standalone-diacritic sequences to precomposed IAST.

    Applies rules 1–7 (macron, dot-above, acute, tilde combinations) and
    rule 8 (consonant-dot patterns: n. → ṇ, d. → ḍ, s. → ṣ, etc.).

    Args:
        text: Raw PDF-extracted text potentially containing garbled diacritics.

    Returns:
        Text with standalone diacritics replaced by their IAST equivalents.
    """
    result = text
    # Rules 1–7: combining diacritics (macron, dot-above, acute, tilde)
    for pattern, replacement in _RULES:
        result = pattern.sub(replacement, result)
    # Rule 8: consonant-dot patterns (must run after rules 1–7)
    for pattern, replacement in _CONSONANT_DOT_RULES:
        result = pattern.sub(replacement, result)
    return result


def cleanup_raga_tala_name(name: str) -> str:
    """Clean up a raga or tala name extracted from garbled PDF text.

    Applies three steps:
      1. Strip mēḷa number, e.g. "(28)".
      2. Normalise garbled diacritics.
      3. Title-case the result.

    Args:
        name: Raw extracted raga or tala name (may contain garbled diacritics,
              parenthesised numbers, etc.).

    Returns:
        Clean, title-cased name suitable for entity resolution.
    """
    if not name:
        return name

    # Step 1: strip mēḷa / reference numbers like (28), (15)
    name = re.sub(r"\s*\(\d+\)\s*", "", name).strip()

    # Step 2: normalise garbled diacritics
    name = normalize_garbled_diacritics(name)

    # Step 3: title-case
    name = name.strip().title()

    return name
