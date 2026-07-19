"""TRACK-130: pinned normalization outputs.

The transliteration-collapse table is matching-critical and is mirrored on the
Kotlin side. Consolidating the two divergent Python copies must not move a single
key, so every output is pinned here against a fixture generated from the
pre-consolidation code. A deliberate change to normalization should show up in
review as a diff to `pinned_outputs.json`, never as a silent shift.
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from src.identity_candidates import normalize_identity_text, normalize_raga_text
from src.normalizer import normalize_for_matching

PINS_PATH = Path(__file__).parent / "fixtures" / "normalization" / "pinned_outputs.json"
_PINS = json.loads(PINS_PATH.read_text(encoding="utf-8"))["pins"]


@pytest.mark.parametrize("name", sorted(_PINS))
def test_normalization_output_is_pinned(name: str) -> None:
    expected = _PINS[name]
    actual = {
        "matching_title": normalize_for_matching(name, "title"),
        "matching_composer": normalize_for_matching(name, "composer"),
        "matching_raga": normalize_for_matching(name, "raga"),
        "matching_tala": normalize_for_matching(name, "tala"),
        "matching_deity": normalize_for_matching(name, "deity"),
        "matching_temple": normalize_for_matching(name, "temple"),
        "identity": normalize_identity_text(name),
        "identity_raga": normalize_raga_text(name),
    }
    assert actual == expected, f"normalization drifted for {name!r}"


def test_pin_fixture_covers_a_meaningful_corpus() -> None:
    """Guard against the fixture being silently emptied or truncated."""
    assert len(_PINS) >= 50, f"expected ~50+ pinned names, got {len(_PINS)}"


# ── TRACK-130 cross-consumer consistency ────────────────────────────────────


def test_both_consumers_share_an_identical_core_rule_sequence() -> None:
    """The shared consonant collapses must not drift apart again.

    This is the regression that motivated TRACK-130: two copies of a
    matching-critical table with different contents. Filtering each consumer's
    rule list down to the core set must reproduce the core sequence exactly —
    same rules, same relative order.
    """
    from src.heuristics.transliteration_collapse import (
        CORE_COLLAPSE_RULES,
        IDENTITY_COLLAPSE_RULES,
        MATCHING_COLLAPSE_RULES,
    )

    core = set(CORE_COLLAPSE_RULES)
    matching_core = tuple(r for r in MATCHING_COLLAPSE_RULES if r in core)
    identity_core = tuple(r for r in IDENTITY_COLLAPSE_RULES if r in core)

    assert matching_core == CORE_COLLAPSE_RULES
    assert identity_core == CORE_COLLAPSE_RULES
    assert matching_core == identity_core


def test_longer_patterns_precede_their_prefixes() -> None:
    """`ksh` before `sh`, `chh` before `ch` — otherwise the collapse is wrong."""
    from src.heuristics.transliteration_collapse import (
        IDENTITY_COLLAPSE_RULES,
        MATCHING_COLLAPSE_RULES,
    )

    for rules in (MATCHING_COLLAPSE_RULES, IDENTITY_COLLAPSE_RULES):
        sources = [src for src, _ in rules]
        for i, src in enumerate(sources):
            for later in sources[i + 1 :]:
                # A shorter pattern firing first would consume the longer one's
                # text, so no earlier source may be contained in a later one.
                assert src not in later, f"{later!r} must precede {src!r} in {sources}"

    # The two orderings that actually motivated this invariant.
    matching_sources = [src for src, _ in MATCHING_COLLAPSE_RULES]
    assert matching_sources.index("ksh") < matching_sources.index("sh")
    assert matching_sources.index("chh") < matching_sources.index("ch")


def test_neither_consumer_keeps_a_private_collapse_table() -> None:
    """Both modules must consume the shared table, not re-declare one."""
    import inspect

    from src import identity_candidates, normalizer

    for module in (normalizer, identity_candidates):
        source = inspect.getsource(module)
        assert '("ksh", "ks")' not in source, (
            f"{module.__name__} re-declares the collapse table; it must import it "
            "from heuristics.transliteration_collapse"
        )
