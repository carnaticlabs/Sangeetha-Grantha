"""The authoritative transliteration-collapse table for matching keys.

Romanised Carnatic names arrive spelled a dozen ways — *Dikshitar* / *Diksitar*
/ *Dikshithar* — so matching keys collapse aspirated digraphs to their bare
consonant before comparison. This table decides whether two spellings are
"the same name", which makes it matching-critical: a change here silently
re-partitions identity resolution.

TRACK-130 consolidated two divergent copies of this table (`normalizer.py` and
`identity_candidates.py`) into this module. The two consumers genuinely need
different rule sets, so those differences are now **named** rather than
accidental:

* `CORE_COLLAPSE_RULES` — the consonant collapses both consumers share.
* `MATCHING_COLLAPSE_RULES` — core plus `chh` (which must fire before `sh`/`ch`)
  and the `kh` aspirate. Used by `normalizer.normalize_for_matching`.
* `IDENTITY_COLLAPSE_RULES` — core plus `kh` plus the long-vowel collapses.
  Used by `identity_candidates.normalize_identity_text`.

Order is significant: longer patterns must precede their prefixes (`ksh` before
`sh`, `chh` before `ch`), which is why these are ordered tuples and not dicts.

**Kotlin counterpart.** There is deliberately no mirror table on the Kotlin side
any more. `NameNormalizationService` delegated consonant collapse to this Python
implementation in its Phase 3 "Simplify and Ship" pass — see the comments there:
"transliteration collapse is now handled by Python normalizer". Kotlin still
applies the long-vowel collapses itself for ragas
(`aa→a`, `ee→i`, `oo→o`, `uu→u`, then space removal), which is the counterpart of
`LONG_VOWEL_COLLAPSE_RULES` below and of `normalize_for_matching(..., "raga")`.
Python is therefore authoritative for the consonant table; keep the vowel rules
in step with Kotlin's raga branch.
"""

from __future__ import annotations

CollapseRules = tuple[tuple[str, str], ...]

# Shared by both consumers, in this order. `ksh` precedes `sh` deliberately.
CORE_COLLAPSE_RULES: CollapseRules = (
    ("ksh", "ks"),
    ("sh", "s"),
    ("th", "t"),
    ("dh", "d"),
    ("bh", "b"),
    ("ph", "p"),
    ("gh", "g"),
    ("jh", "j"),
    ("ch", "c"),
)

# `chh` must collapse before `sh` and `ch` get a chance to fire, otherwise
# "chh" degrades to "ch" instead of "c". Only the matching key applies it.
MATCHING_ONLY_RULES: CollapseRules = (("chh", "c"),)

# Aspirate smoothing both consumers apply, historically at different points in
# the sequence. `kh` shares no characters with any other pattern here, so its
# position cannot change the result (verified by the pinned-output fixture).
ASPIRATE_KH_RULES: CollapseRules = (("kh", "k"),)

# Long-vowel collapses. Kotlin applies the same four for ragas.
LONG_VOWEL_COLLAPSE_RULES: CollapseRules = (
    ("aa", "a"),
    ("ee", "i"),
    ("oo", "o"),
    ("uu", "u"),
)

# `chh` is inserted directly after `ksh`, matching the historical ordering.
MATCHING_COLLAPSE_RULES: CollapseRules = (
    CORE_COLLAPSE_RULES[:1] + MATCHING_ONLY_RULES + CORE_COLLAPSE_RULES[1:] + ASPIRATE_KH_RULES
)

IDENTITY_COLLAPSE_RULES: CollapseRules = CORE_COLLAPSE_RULES + ASPIRATE_KH_RULES + LONG_VOWEL_COLLAPSE_RULES


def apply_collapse(value: str, rules: CollapseRules) -> str:
    """Apply an ordered collapse table to `value`."""
    result = value
    for source, target in rules:
        result = result.replace(source, target)
    return result
