"""Manifest-driven search metadata for approved non-cycle claims (TRACK-148 design option D1).

Approved claims that are not part of a cycle (a mudra, a laya, a raga trait, a ritual function) reach search
through the application manifests, not through a catalogue column or a new table. This module reads the two
manifests and returns, per catalogue krithi, the labelled values the context formatter may add to a header.

Only a claim that is reviewed, carries a complete named human approval and is marked `application: apply` is
returned. A claim marked `deferred` stays invisible, and a krithi with no such claim gets no clause at all.
"""

from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
MANIFESTS: tuple[Path, ...] = (
    ROOT / "database" / "data" / "track146-syama-sastri-application-manifest.json",
    ROOT / "database" / "data" / "track147-tyagaraja-application-manifest.json",
)

# Per composer: axis -> label, in the order the clauses are rendered (most discriminative first).
# Cycle membership, form, geography, deity and attribution are handled elsewhere and are not rendered here.
SYAMA_LABELS: tuple[tuple[str, str], ...] = (
    ("mudra", "Mudra"),
    ("laya", "Laya"),
    ("raga_character", "Raga Trait"),
    ("bhava", "Bhava"),
)
TYAGARAJA_LABELS: tuple[tuple[str, str], ...] = (
    ("mudra", "Mudra"),
    ("ritual", "Ritual Function"),
    ("dramatic", "Dramatic Context"),
    ("theme", "Theme"),
)
LABELS_BY_COMPOSER: dict[str, tuple[tuple[str, str], ...]] = {
    "Syama Sastri": SYAMA_LABELS,
    "Tyagaraja": TYAGARAJA_LABELS,
}
# A manifest belongs to one composer, told apart by the claim id prefix.
_COMPOSER_BY_PREFIX: tuple[tuple[str, str], ...] = (("SS-", "Syama Sastri"), ("tyagaraja:", "Tyagaraja"))

ClaimValues = dict[str, list[str]]


def _composer_of(claim_id: str) -> str | None:
    return next((composer for prefix, composer in _COMPOSER_BY_PREFIX if claim_id.startswith(prefix)), None)


def _is_applicable(change: dict[str, object]) -> bool:
    approval = change.get("approval")
    if not isinstance(approval, dict) or approval.get("decision") != "approve":
        return False
    if not all(approval.get(k) for k in ("reviewer", "accepted_at", "note")):
        return False
    return bool(approval.get("application", "apply") == "apply")


def load_approved_claims(manifests: tuple[Path, ...] = MANIFESTS) -> dict[str, dict[str, ClaimValues]]:
    """Return {canonical krithi id: {composer: {axis: [values]}}} for applicable approved non-cycle claims."""
    found: dict[str, dict[str, ClaimValues]] = defaultdict(lambda: defaultdict(lambda: defaultdict(list)))
    for path in manifests:
        if not path.exists():
            continue
        manifest = json.loads(path.read_text(encoding="utf-8"))
        for change in manifest.get("changes", []):
            claim_id = str(change.get("claim_id", ""))
            composer = _composer_of(claim_id)
            axis, value, krithi_id = change.get("axis"), change.get("value"), change.get("canonical_id")
            if composer is None or not krithi_id or not isinstance(value, str) or not value.strip():
                continue
            if axis not in dict(LABELS_BY_COMPOSER[composer]) or not _is_applicable(change):
                continue
            values = found[str(krithi_id)][composer][str(axis)]
            if value.strip() not in values:
                values.append(value.strip())
    return {k: {c: dict(a) for c, a in v.items()} for k, v in found.items()}


def _readable(value: str) -> str:
    """Turns a tag slug such as `tyagaraja-nauka-charitram` into words the embedding model can use."""
    if re.fullmatch(r"[a-z]+(?:-[a-z]+)+", value):
        words = value.split("-")
        if words[0] == "tyagaraja":
            words = words[1:]
        return " ".join(w.capitalize() for w in words)
    return value


def claim_clauses(composer: str, claims: dict[str, ClaimValues] | None) -> list[str]:
    """Labelled `[Label: value]` clauses for one krithi, abstaining when the composer has no approved claim."""
    if not claims:
        return []
    by_axis = claims.get(composer)
    if not by_axis:
        return []
    clauses = []
    for axis, label in LABELS_BY_COMPOSER.get(composer, ()):
        values = by_axis.get(axis)
        if values:
            clauses.append(f"[{label}: {'; '.join(_readable(v) for v in values)}]")
    return clauses
