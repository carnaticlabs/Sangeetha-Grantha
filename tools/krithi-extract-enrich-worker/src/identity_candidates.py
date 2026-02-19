"""Identity candidate discovery using RapidFuzz.

This module proposes composer/raga candidates from reference data for
downstream resolution review. It is deterministic and safe for production
worker usage (no external network dependencies).
"""

from __future__ import annotations

import re
import unicodedata
from collections.abc import Iterable
from dataclasses import dataclass
from difflib import SequenceMatcher

try:
    from rapidfuzz import fuzz
except Exception:  # pragma: no cover - defensive fallback
    fuzz = None

from .schema import CanonicalIdentityCandidate, CanonicalIdentityCandidates


@dataclass(frozen=True)
class ReferenceEntity:
    """Reference entity used for fuzzy identity scoring."""

    entity_id: str
    name: str
    aliases: tuple[str, ...] = ()


def _strip_diacritics(value: str) -> str:
    decomposed = unicodedata.normalize("NFD", value)
    return "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn")


def normalize_identity_text(value: str) -> str:
    """Normalize free text into a stable fuzzy-matching key."""
    cleaned = _strip_diacritics(value).lower().strip()
    cleaned = re.sub(r"[^a-z0-9\s]", " ", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()

    # Transliteration-aware collapses (mirrors Kotlin normalization intent).
    replacements = (
        ("ksh", "ks"),
        ("sh", "s"),
        ("th", "t"),
        ("dh", "d"),
        ("bh", "b"),
        ("gh", "g"),
        ("ph", "p"),
        ("kh", "k"),
        ("jh", "j"),
        ("ch", "c"),
        ("aa", "a"),
        ("ee", "i"),
        ("oo", "o"),
        ("uu", "u"),
    )
    for src, dst in replacements:
        cleaned = cleaned.replace(src, dst)
    return re.sub(r"\s+", " ", cleaned).strip()


def normalize_raga_text(value: str) -> str:
    """Raga-specific normalization: collapse spacing variants."""
    return normalize_identity_text(value).replace(" ", "")


class IdentityCandidateDiscovery:
    """Discovers top-N composer and raga candidates via RapidFuzz scoring."""

    def __init__(
        self,
        composers: Iterable[ReferenceEntity],
        ragas: Iterable[ReferenceEntity],
        *,
        min_score: int = 60,
        max_candidates: int = 5,
    ) -> None:
        self._composers = list(composers)
        self._ragas = list(ragas)
        self._min_score = max(0, min(100, min_score))
        self._max_candidates = max(1, max_candidates)

    def discover(
        self,
        composer: str | None,
        ragas: list[str] | None,
    ) -> CanonicalIdentityCandidates:
        composer_candidates = self._score_candidates(
            query_values=[composer] if composer else [],
            entities=self._composers,
            normalizer=normalize_identity_text,
        )
        raga_candidates = self._score_candidates(
            query_values=ragas or [],
            entities=self._ragas,
            normalizer=normalize_raga_text,
        )
        return CanonicalIdentityCandidates(
            composers=composer_candidates,
            ragas=raga_candidates,
        )

    def _score_candidates(
        self,
        query_values: list[str],
        entities: list[ReferenceEntity],
        normalizer,
    ) -> list[CanonicalIdentityCandidate]:
        normalized_queries = [normalizer(value) for value in query_values if value and value.strip()]
        normalized_queries = [value for value in normalized_queries if value]
        if not normalized_queries:
            return []

        best_by_entity: dict[str, CanonicalIdentityCandidate] = {}
        for entity in entities:
            best_score = -1
            matched_on: str | None = None
            canonical_norm = normalizer(entity.name)
            alias_norms = [normalizer(alias) for alias in entity.aliases if alias]
            compare_tokens = [("canonical", canonical_norm)] + [("alias", alias) for alias in alias_norms]

            for query in normalized_queries:
                for token_name, token_value in compare_tokens:
                    if not token_value:
                        continue
                    score = self._ratio(query, token_value)
                    if score > best_score:
                        best_score = score
                        matched_on = token_name

            if best_score < self._min_score:
                continue

            confidence = "LOW"
            if best_score >= 90:
                confidence = "HIGH"
            elif best_score >= 70:
                confidence = "MEDIUM"

            candidate = CanonicalIdentityCandidate(
                entityId=entity.entity_id,
                name=entity.name,
                score=best_score,
                confidence=confidence,
                matchedOn=matched_on,
            )
            existing = best_by_entity.get(entity.entity_id)
            if existing is None or candidate.score > existing.score:
                best_by_entity[entity.entity_id] = candidate

        ranked = sorted(
            best_by_entity.values(),
            key=lambda item: (-item.score, item.name.lower()),
        )
        return ranked[: self._max_candidates]

    def _ratio(self, left: str, right: str) -> int:
        if left == right:
            return 100
        if fuzz is not None:
            return int(fuzz.WRatio(left, right))
        # Fallback only if RapidFuzz import fails.
        return int(SequenceMatcher(None, left, right).ratio() * 100)
