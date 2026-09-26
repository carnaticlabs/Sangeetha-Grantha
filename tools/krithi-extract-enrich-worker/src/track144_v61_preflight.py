"""Preflight for V61's unscoped Marugelaraa pallavi write.

V61 inserts one Latin pallavi into every lyric variant of a krithi titled
Marugelaraa and prepends that text to every variant's lyrics. It does not
filter by composer or script. Applied migrations stay immutable. This module
decides whether a populated database can migrate, and it captures non-Latin
rows before that write so an operator can confirm V65 restored them.
"""

from __future__ import annotations

import json
from pathlib import Path
from uuid import UUID

from pydantic import BaseModel, Field

LATIN_PALLAVI = "marug(E)lar(A) O rAghav(A)"
LYRICS_PREFIX = LATIN_PALLAVI + "\n\n"
REPAIRED_TITLES = ("Marugelaraa", "Sri Narada Nada", "dIna janAvana")
MARUGELARA_TITLE = "Marugelaraa"
EXPECTED_COMPOSER = "Tyagaraja"


class KrithiIdentity(BaseModel):
    krithi_id: UUID
    title: str = Field(min_length=1)
    composer: str = Field(min_length=1)


class LyricVariantView(BaseModel):
    variant_id: UUID
    krithi_id: UUID
    title: str
    composer: str
    script: str
    language: str
    lyrics: str
    pallavi_text: str | None = None


class PreflightInput(BaseModel):
    v61_applied: bool
    v65_applied: bool
    krithis: list[KrithiIdentity]
    variants: list[LyricVariantView]
    snapshot_acknowledged: bool = False


class PreflightDecision(BaseModel):
    blocks_migration: bool
    needs_recovery: bool
    reasons: list[str]


class VariantSnapshot(BaseModel):
    """Captured non-Latin Marugelaraa rows, taken before V61 runs."""

    track: str = "TRACK-144"
    variants: list[LyricVariantView]


def is_contaminated(variant: LyricVariantView) -> bool:
    """True when a non-Latin variant still holds the Latin text V61 inserts."""
    if variant.script == "latin":
        return False
    if variant.lyrics.startswith(LYRICS_PREFIX):
        return True
    return variant.pallavi_text == LATIN_PALLAVI


def assess(inventory: PreflightInput) -> PreflightDecision:
    reasons: list[str] = []
    blocks = False
    needs_recovery = False

    by_title: dict[str, list[KrithiIdentity]] = {}
    for krithi in inventory.krithis:
        if krithi.title in REPAIRED_TITLES:
            by_title.setdefault(krithi.title, []).append(krithi)

    for title, rows in sorted(by_title.items()):
        if len(rows) > 1:
            blocks = True
            reasons.append(f"{title} matches {len(rows)} krithis. V61 selects that title with a scalar subquery.")

    marugelara = by_title.get(MARUGELARA_TITLE, [])
    if len(marugelara) == 1 and marugelara[0].composer != EXPECTED_COMPOSER and not inventory.v61_applied:
        blocks = True
        reasons.append(
            f"The only {MARUGELARA_TITLE} is by {marugelara[0].composer}. V61 would update it by title alone."
        )

    non_latin = [variant for variant in inventory.variants if variant.script != "latin"]
    contaminated = [variant for variant in non_latin if is_contaminated(variant)]

    if non_latin and not inventory.v61_applied and not inventory.snapshot_acknowledged:
        blocks = True
        reasons.append(
            f"{len(non_latin)} non-Latin {MARUGELARA_TITLE} variant(s) would receive the Latin pallavi. "
            "Write a snapshot, then migrate. V65 removes that inserted text."
        )
    elif non_latin and not inventory.v61_applied and inventory.snapshot_acknowledged:
        reasons.append(
            "Snapshot acknowledged. V61 will still insert the Latin pallavi; V65 removes it from non-Latin variants."
        )

    if contaminated and not inventory.v65_applied:
        needs_recovery = True
        reasons.append(
            f"{len(contaminated)} non-Latin variant(s) still contain the Latin pallavi V61 inserted. "
            "Apply V65. It restores those rows from the text it reads, without reconstructing an unseen original."
        )
    elif contaminated and inventory.v65_applied:
        needs_recovery = True
        blocks = True
        reasons.append("V65 is applied but non-Latin Marugelaraa variants still contain the inserted Latin pallavi.")

    if not reasons:
        if inventory.v61_applied:
            reasons.append(
                "V61 is applied. Marugelaraa has no non-Latin variant holding the inserted Latin pallavi. "
                "V61 itself is not restricted to Tyagaraja or Latin script."
            )
        else:
            reasons.append(
                "V61 is not applied. The repaired titles are unambiguous and Marugelaraa has no non-Latin variant."
            )

    return PreflightDecision(blocks_migration=blocks, needs_recovery=needs_recovery, reasons=reasons)


def write_snapshot(path: Path, variants: list[LyricVariantView]) -> VariantSnapshot:
    snapshot = VariantSnapshot(variants=[variant for variant in variants if variant.script != "latin"])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(snapshot.model_dump_json(indent=2) + "\n", encoding="utf-8")
    return snapshot


def load_snapshot(path: Path) -> VariantSnapshot:
    return VariantSnapshot.model_validate(json.loads(path.read_text(encoding="utf-8")))


def snapshot_still_matches(snapshot: VariantSnapshot, current: list[LyricVariantView]) -> list[str]:
    """Report snapshot rows whose non-Latin text was not restored."""
    current_by_id = {variant.variant_id: variant for variant in current}
    problems: list[str] = []
    for saved in snapshot.variants:
        live = current_by_id.get(saved.variant_id)
        if live is None:
            problems.append(f"{saved.variant_id} is no longer present")
            continue
        if live.script != saved.script:
            problems.append(f"{saved.variant_id} script changed from {saved.script} to {live.script}")
        if is_contaminated(live):
            problems.append(f"{saved.variant_id} still contains the inserted Latin pallavi")
        if live.lyrics != saved.lyrics:
            problems.append(f"{saved.variant_id} lyrics differ from the pre-V61 snapshot")
    return problems
