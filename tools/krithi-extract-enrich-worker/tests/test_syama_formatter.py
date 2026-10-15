"""TRACK-146: Syama Sastri header axes render only from approved claims."""

from src.embeddings.context_formatter import MAX_HEADER_WORDS, _lakshana_headers

APPROVED = {
    "provenance_owner": "TRACK-146",
    "review_state": "reviewed",
    "claim_id": "SS-0001:group:1",
    "approval": {"decision": "approve", "reviewer": "Seshadri", "accepted_at": "2026-10-06", "note": "Approved"},
    "laya": "Misra Chapu",
    "structure": "8 numbered swara-sahitya units",
    "mudra": "Syama Krishna",
}


def cycle(attrs: dict) -> dict:
    return {
        "slug": "syama-sastri-swarajathi-ratnatrayam",
        "display_name": "Swarajathi Ratnatrayam",
        "role": "CORE",
        "attrs": attrs,
    }


def header(attrs: dict, composer: str = "Syama Sastri") -> str:
    return " ".join(
        _lakshana_headers("Kamakshi anudinamu", composer, raga="Bhairavi", tala="Capu", cycle_info=cycle(attrs))
    )


def test_approved_axes_render_within_budget() -> None:
    text = header(APPROVED)
    for token in (
        "Laya: Misra Chapu",
        "Structure: 8 numbered swara-sahitya units",
        "Mudra: Syama Krishna",
    ):
        assert token in text
    assert "Apex Sovereign" not in text
    assert "Bhava:" not in text
    assert len(text.split()) <= MAX_HEADER_WORDS


def test_unapproved_axes_are_withheld() -> None:
    attrs = {**APPROVED, "review_state": "unresolved"}
    text = header(attrs)
    assert "Swarajathi Ratnatrayam" not in text and "Misra Chapu" not in text


def test_other_composer_never_gets_syama_axes() -> None:
    assert "Misra Chapu" not in header(APPROVED, composer="Muthuswami Dikshitar")


def test_composition_overview_renders_axes_only_from_approved_membership() -> None:
    from src.embeddings.context_formatter import format_composition_overview

    def first_line(attrs: dict, composer: str = "Syama Sastri") -> str:
        return format_composition_overview(
            title="Kamakshi anudinamu",
            composer=composer,
            raga="Bhairavi",
            tala="Capu",
            musical_form="SWARAJATHI",
            primary_lyrics="Kamakshi anudinamu",
            cycle_info=cycle(attrs),
        ).splitlines()[0]

    approved = first_line(APPROVED)
    for token in (
        "Laya: Misra Chapu",
        "Structure: 8 numbered swara-sahitya units",
        "Mudra: Syama Krishna",
    ):
        assert token in approved
    assert "Apex Sovereign" not in approved
    assert len(approved.split()) <= MAX_HEADER_WORDS

    withheld = first_line({**APPROVED, "approval": None})
    assert not any(t in withheld for t in ("Laya:", "Bhava:", "Mudra:", "Ratnatrayam"))
    assert "Laya:" not in first_line(APPROVED, composer="Muthuswami Dikshitar")


def test_direct_axis_arguments_are_no_longer_accepted() -> None:
    """Unapproved values must have no path into a header except an approved membership."""
    import pytest

    from src.embeddings.context_formatter import format_composition_overview

    with pytest.raises(TypeError):
        format_composition_overview(title="Mari vere gati", composer="Syama Sastri", laya="Viloma Chapu (4+3)")  # type: ignore[call-arg]


def test_cycle_overview_renders_only_approved_member_attributes() -> None:
    from src.embeddings.context_formatter import format_cycle_overview

    members = [
        {
            "sequence_order": 1,
            "title": "Kamakshi anudinamu",
            "raga": "Bhairavi",
            "role": "CORE",
            "discriminative_attributes": {
                **APPROVED,
                "laya": "Misra Chapu",
                "structure": "8 numbered swara-sahitya units",
            },
        },
        {
            "sequence_order": 2,
            "title": "Rave hima giri kumari",
            "raga": "Hanumatodi",
            "role": "CORE",
            "discriminative_attributes": {"laya": "Adi (2 kalai)", "structure": "Swarasahitya (6 charanams)"},
        },
    ]
    doc = format_cycle_overview(
        cycle_name="Swarajathi Ratnatrayam",
        composer="Syama Sastri",
        description="The three swarajathis of Syama Sastri.",
        members=members,
    )
    assert "[Cycle: Swarajathi Ratnatrayam] [Composer: Syama Sastri]" in doc
    assert "Laya: Misra Chapu" in doc
    assert "Structure: 8 numbered swara-sahitya units" in doc
    assert "Adi (2 kalai)" not in doc and "6 charanams" not in doc


def test_kshetra_overview_dynamic_composer() -> None:
    from src.embeddings.context_formatter import format_kshetra_overview

    sastri_gap = format_kshetra_overview(
        temple_name="Kanyakumari",
        city="Kanyakumari",
        state="Tamil Nadu",
        is_gap=True,
        composer="Syama Sastri",
    )
    assert "No Syama Sastri compositions are attested" in sastri_gap

    dikshitar_default = format_kshetra_overview(
        temple_name="Kanyakumari",
        city="Kanyakumari",
        state="Tamil Nadu",
        is_gap=True,
    )
    assert "No Muthuswami Dikshitar compositions are attested" in dikshitar_default
