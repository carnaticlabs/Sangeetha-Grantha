"""TRACK-154: lookup/triage/build on SYNTHETIC fixtures only (no recorded Google content, no network)."""

import json
import logging
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

import httpx
import pytest
import respx

from src import temple_place_lookup as tpl

FIXTURES = Path(__file__).parent / "fixtures" / "temple_place"
KEY = "AIzaSyNOT-A-REAL-KEY-synthetic-0123456789"


def target(name: str, city: str = "Townville", kind: str = "COMPLEX", aliases: list[str] | None = None):
    return tpl.TempleTarget(
        name=name,
        name_normalized=name.lower(),
        city=city,
        state="Tamil Nadu",
        country="India",
        place_kind=kind,
        aliases=aliases or [],
    )


def place(pid: str, name: str, address: str) -> dict[str, Any]:
    # `location` is included on purpose: it must never survive parsing.
    return {
        "id": pid,
        "displayName": {"text": name, "languageCode": "en"},
        "formattedAddress": address,
        "location": {"latitude": 10.5, "longitude": 79.5},
    }


def reply(*places: dict[str, Any]) -> httpx.Response:
    return httpx.Response(200, json={"places": list(places)})


def client(max_calls: int = 50) -> tpl.PlacesClient:
    return tpl.PlacesClient(KEY, max_calls=max_calls, http=httpx.Client())


# ─── request shape ────────────────────────────────────────────────────────────────────────────


@respx.mock
def test_request_uses_header_key_minimal_field_mask_and_page_size():
    route = respx.post(tpl.SEARCH_URL).mock(return_value=reply())
    client().search("Alpha Temple, Townville, Tamil Nadu")
    request = route.calls.last.request
    assert request.headers["X-Goog-Api-Key"] == KEY
    assert KEY not in str(request.url)
    assert request.url.query == b""
    assert request.headers["X-Goog-FieldMask"] == "places.id,places.displayName,places.formattedAddress"
    body = json.loads(request.content)
    assert body == {"textQuery": "Alpha Temple, Townville, Tamil Nadu", "pageSize": 5}


def test_field_mask_has_no_geometry_fields():
    for token in ("location", "viewport", "geometry", "plusCode", "rating", "photos"):
        assert token.lower() not in tpl.FIELD_MASK.lower()
    assert tpl.PAGE_SIZE <= 5


@respx.mock
def test_response_geometry_is_not_kept():
    respx.post(tpl.SEARCH_URL).mock(return_value=reply(place("p1", "Alpha Temple", "Townville, Tamil Nadu")))
    found = client().search("q")
    assert found == [
        tpl.Candidate(place_id="p1", display_name="Alpha Temple", formatted_address="Townville, Tamil Nadu", rank=1)
    ]
    assert "10.5" not in found[0].model_dump_json()


def test_client_requires_a_key_and_hides_it_in_repr():
    with pytest.raises(ValueError):
        tpl.PlacesClient("", max_calls=1)
    assert KEY not in repr(client())


# ─── key redaction ────────────────────────────────────────────────────────────────────────────


@respx.mock
def test_key_never_appears_in_errors_logs_or_files(tmp_path, caplog):
    echoed = httpx.Response(403, text=f"API key {KEY} is not authorised for this API")
    respx.post(tpl.SEARCH_URL).mock(return_value=echoed)
    store = tmp_path / "c.json"
    caplog.set_level(logging.DEBUG)
    summary = tpl.LookupRunner(client(), store).run([target("Alpha Temple")])
    assert summary.stopped_reason and summary.stopped_reason.startswith("FATAL")
    assert KEY not in caplog.text
    assert KEY not in summary.model_dump_json()
    assert KEY not in store.read_text()
    assert "[REDACTED]" in store.read_text()


@respx.mock
def test_transport_error_message_is_redacted():
    respx.post(tpl.SEARCH_URL).mock(side_effect=httpx.ConnectError(f"cannot reach host with {KEY}"))
    with pytest.raises(tpl.PlacesError) as err:
        client().search("q")
    assert KEY not in str(err.value)


def test_redact_helper():
    assert tpl.redact(f"a {KEY} b", KEY) == "a [REDACTED] b"
    assert tpl.redact("plain", None) == "plain"


# ─── call ceiling and resume ──────────────────────────────────────────────────────────────────


@respx.mock
def test_call_ceiling_stops_before_exceeding(tmp_path):
    route = respx.post(tpl.SEARCH_URL).mock(return_value=reply())
    targets = [target(f"Temple {i}") for i in range(5)]
    summary = tpl.LookupRunner(client(max_calls=3), tmp_path / "c.json").run(targets)
    assert route.call_count == 3
    assert summary.calls_total == 3
    assert summary.stopped_reason and "ceiling" in summary.stopped_reason
    assert summary.answered == 3


@respx.mock
def test_ceiling_is_cumulative_across_runs(tmp_path):
    route = respx.post(tpl.SEARCH_URL).mock(return_value=reply())
    store = tmp_path / "c.json"
    targets = [target(f"Temple {i}") for i in range(4)]
    tpl.LookupRunner(client(max_calls=3), store).run(targets)
    again = tpl.LookupRunner(client(max_calls=3), store).run(targets)
    assert route.call_count == 3  # the second run was already at the ceiling
    assert again.calls_this_run == 0
    assert again.stopped_reason


@respx.mock
def test_resume_skips_answered_queries_and_retries_failures(tmp_path):
    store = tmp_path / "c.json"
    targets = [target("Alpha Temple"), target("Beta Temple")]
    respx.post(tpl.SEARCH_URL).mock(
        side_effect=[reply(), httpx.Response(500, text="boom")],
    )
    first = tpl.LookupRunner(client(), store).run(targets)
    assert first.answered == 1 and len(first.failed) == 1

    respx.reset()
    route = respx.post(tpl.SEARCH_URL).mock(return_value=reply())
    second = tpl.LookupRunner(client(), store).run(targets)
    assert route.call_count == 1  # only Beta is asked again
    assert second.skipped_resumed == 1 and second.answered == 1
    assert second.calls_total == 3
    assert tpl.load_candidates(store).calls_made == 3


@respx.mock
def test_sub_shrines_are_never_looked_up(tmp_path):
    route = respx.post(tpl.SEARCH_URL).mock(return_value=reply())
    targets = [target("Parent Temple"), target("Sannidhi", kind="SANNIDHI"), target("Mandapa", kind="MANDAPAM")]
    tpl.LookupRunner(client(), tmp_path / "c.json").run(targets)
    assert route.call_count == 1


@respx.mock
def test_pass_two_retries_only_unconfirmed_targets_with_variant_query(tmp_path):
    store = tmp_path / "c.json"
    good = target("Alpha Temple")
    bad = target("Beta Temple", aliases=["Betha Koil"])
    respx.post(tpl.SEARCH_URL).mock(
        side_effect=[reply(place("p1", "Alpha Temple", "Townville, Tamil Nadu, India")), reply()],
    )
    tpl.LookupRunner(client(), store).run([good, bad], pass_no=1)
    respx.reset()
    route = respx.post(tpl.SEARCH_URL).mock(return_value=reply())
    tpl.LookupRunner(client(), store).run([good, bad], pass_no=2)
    assert route.call_count == 1
    assert json.loads(route.calls.last.request.content)["textQuery"] == "Betha Koil, Townville, Tamil Nadu"


def test_query_variants():
    t = target("Alpha Temple")
    assert tpl.build_query(t, 1) == "Alpha Temple, Townville, Tamil Nadu"
    assert tpl.build_query(t, 2) == "Alpha Temple"
    assert tpl.build_query(target("Alpha Temple", aliases=["Alpha Koil"]), 2) == "Alpha Koil, Townville, Tamil Nadu"


def test_working_file_must_not_be_trackable(tmp_path):
    tpl.assert_untracked_location(tmp_path / "ok.json")  # outside any repository
    repo = Path(__file__).resolve().parents[3]
    with pytest.raises(ValueError):
        tpl.assert_untracked_location(repo / "database" / "data" / "candidates.json")
    tpl.assert_untracked_location(repo / "storage" / "track154" / "candidates.json")


# ─── triage ───────────────────────────────────────────────────────────────────────────────────


def record(t: tpl.TempleTarget, *candidates: tpl.Candidate, pass_no: int = 1) -> tpl.QueryRecord:
    return tpl.QueryRecord(
        key=t.key, pass_no=pass_no, query="q", status="ok", candidates=list(candidates), at="2026-10-25T00:00:00+00:00"
    )


def cand(pid: str, name: str, address: str, rank: int = 1) -> tpl.Candidate:
    return tpl.Candidate(place_id=pid, display_name=name, formatted_address=address, rank=rank)


def test_auto_confirm_requires_single_candidate_with_matching_name_and_locality():
    t = target("Śrī Alpha Temple")
    ok = cand("p1", "Sri Alpha Temple", "Main St, Townville, Tamil Nadu 600001, India")
    assert tpl.classify(t, record(t, ok)).triage == "AUTO_CONFIRM"
    assert tpl.classify(t, record(t, ok)).place_id == "p1"


def test_single_candidate_with_wrong_name_or_locality_is_reviewed():
    t = target("Alpha Temple")
    assert tpl.classify(t, record(t, cand("p", "Alpha Stores", "Townville, Tamil Nadu"))).triage == "SINGLE_MISMATCH"
    other_town = cand("p", "Alpha Temple", "Elsewhere, Tamil Nadu")
    assert tpl.classify(t, record(t, other_town)).triage == "SINGLE_MISMATCH"
    # whole-word match: "Town" must not match inside "Townville"
    short = target("Alpha Temple", city="Town")
    assert tpl.classify(short, record(short, cand("p", "Alpha Temple", "Townville"))).triage == "SINGLE_MISMATCH"


def test_multiple_candidates_never_auto_confirm_even_if_one_matches():
    t = target("Alpha Temple")
    a = cand("p1", "Alpha Temple", "Townville, Tamil Nadu", 1)
    b = cand("p2", "Alpha Temple", "Townville, Tamil Nadu", 2)
    c = cand("p3", "Other", "Townville, Tamil Nadu", 2)
    assert tpl.classify(t, record(t, a, b)).triage == "AMBIGUOUS_MATCHES"
    assert tpl.classify(t, record(t, a, c)).triage == "MULTIPLE_CANDIDATES"
    assert tpl.classify(t, record(t, c, c)).triage == "MULTIPLE_CANDIDATES"


def test_no_candidates_and_unlooked_up():
    t = target("Alpha Temple")
    assert tpl.classify(t, record(t)).triage == "NO_CANDIDATES"
    assert tpl.classify(t, None).triage == "NOT_LOOKED_UP"


def test_pass_two_candidates_never_auto_confirm():
    t = target("Alpha Temple")
    data = tpl.CandidateFile(
        records=[record(t), record(t, cand("p1", "Alpha Temple", "Townville, Tamil Nadu"), pass_no=2)]
    )
    entry = tpl.triage([t], data).entries[0]
    assert entry.decision is None and entry.triage == "NO_CANDIDATES"
    assert entry.retry_candidates[0].place_id == "p1"


def test_triage_orders_by_risk_prefills_auto_and_demotes_duplicate_places():
    a, b, c, d = target("Alpha Temple"), target("Beta Temple"), target("Gamma Temple"), target("Delta Temple")
    same = cand("dup", "x", "Townville")
    data = tpl.CandidateFile(
        records=[
            record(a, cand("pa", "Alpha Temple", "Townville, TN")),
            record(b, cand("dup", "Beta Temple", "Townville, TN")),
            record(c, cand("dup", "Gamma Temple", "Townville, TN")),
            record(d),
        ]
    )
    assert same.place_id == "dup"
    review = tpl.triage([a, b, c, d, target("Sub", kind="SANNIDHI")], data, now=datetime(2026, 10, 25, tzinfo=UTC))
    by_name = {e.name: e for e in review.entries}
    assert len(review.entries) == 4  # sub-shrine excluded
    assert by_name["Alpha Temple"].decision == "CONFIRMED"
    assert by_name["Alpha Temple"].reviewed_by == tpl.AUTO_ACTOR
    assert by_name["Alpha Temple"].reviewed_at == "2026-10-25"
    for name in ("Beta Temple", "Gamma Temple"):
        assert by_name[name].decision is None and by_name[name].triage == "DUPLICATE_PLACE_ID"
    assert [e.risk for e in review.entries] == sorted(e.risk for e in review.entries)
    assert review.entries[-1].name == "Alpha Temple"  # auto-confirmed sorts last
    report = tpl.coverage_report(review)
    assert report.counts == {"CONFIRMED": 1, "LOCALITY_ONLY": 0, "UNRESOLVED": 0, "PENDING_REVIEW": 3}


# ─── build ────────────────────────────────────────────────────────────────────────────────────


def decided(name: str, decision, pid: str | None = None, **kw) -> tpl.ReviewEntry:
    return tpl.ReviewEntry(
        name=name,
        name_normalized=name.lower(),
        city="Townville",
        state="Tamil Nadu",
        country="India",
        triage="X",
        risk=1,
        decision=decision,
        google_place_id=pid,
        **kw,
    )


def signed(*entries: tpl.ReviewEntry) -> tpl.ReviewFile:
    return tpl.ReviewFile(
        signoff=tpl.Signoff(reviewer="Seshadri", date="2026-10-26", statement="synthetic"), entries=list(entries)
    )


def test_build_strips_google_content_and_applies_signoff_defaults():
    e = decided("Alpha Temple", "CONFIRMED", "p1")
    e.candidates = [cand("p1", "Google Name", "Google Address")]
    data = tpl.build_place_data(signed(e, decided("Beta Temple", "UNRESOLVED", note="no match")))
    text = tpl.canonical_json(data)
    assert "Google Name" not in text and "Google Address" not in text
    assert "latitude" not in text and "longitude" not in text
    assert {x.reviewed_by for x in data.entries} == {"Seshadri"}
    assert all(x.reviewed_at == "2026-10-26T00:00:00Z" for x in data.entries)
    assert data.approvals[0].reviewer == "Seshadri"


@pytest.mark.parametrize(
    "entries,fragment",
    [
        ([decided("a", None)], "no decision"),
        ([decided("a", "CONFIRMED")], "needs google_place_id"),
        ([decided("a", "UNRESOLVED", "p")], "must not carry"),
        ([decided("a", "CONFIRMED", "p"), decided("b", "CONFIRMED", "p")], "already confirmed"),
        (
            [
                tpl.ReviewEntry(
                    name="s", name_normalized="s", triage="X", risk=1, place_kind="SANNIDHI", decision="UNRESOLVED"
                )
            ],
            "sub-shrines",
        ),
    ],
)
def test_build_refuses_bad_reviews(entries, fragment):
    with pytest.raises(tpl.BuildError, match=fragment):
        tpl.build_place_data(signed(*entries))


def test_build_requires_signoff():
    review = tpl.ReviewFile(entries=[decided("a", "UNRESOLVED")])
    with pytest.raises(tpl.BuildError, match="signoff"):
        tpl.build_place_data(review)


def test_locality_only_may_omit_or_keep_a_place_id():
    data = tpl.build_place_data(signed(decided("a", "LOCALITY_ONLY"), decided("b", "LOCALITY_ONLY", "town")))
    assert [x.google_place_id for x in data.entries] == [None, "town"]


# ─── seed generation and drift ────────────────────────────────────────────────────────────────


def synthetic_data() -> tpl.PlaceReviewData:
    return tpl.PlaceReviewData.model_validate_json((FIXTURES / "synthetic-review.json").read_text("utf-8"))


def test_seed_sql_embeds_the_json_exactly():
    data = synthetic_data()
    sql = tpl.render_seed_sql(data)
    assert tpl.embedded_matches_json(sql, tpl.canonical_json(data))
    drifted = tpl.canonical_json(data).replace("synthetic-place-1", "synthetic-place-X")
    assert not tpl.embedded_matches_json(sql, drifted)


def test_seed_sql_never_writes_coordinates_or_other_columns():
    sql = tpl.render_seed_sql(synthetic_data())
    update = sql[sql.index("UPDATE temples") : sql.index("WHERE id = t.id")]
    assert "latitude" not in update and "longitude" not in update
    code = "\n".join(line for line in sql.splitlines() if not line.lstrip().startswith("--"))
    assert "latitude" not in code.lower() and "longitude" not in code.lower()
    for col in ("google_place_id", "place_review_state", "place_reviewed_by", "place_reviewed_at", "updated_at"):
        assert col in update


def test_committed_fixture_seed_is_current():
    """The synthetic seed used by the Kotlin seed test must equal what the generator emits."""
    expected = tpl.render_seed_sql(synthetic_data())
    assert (FIXTURES / "synthetic-seed.sql").read_text("utf-8") == expected


def test_committed_reviewed_json_and_seed_agree_when_present():
    root = Path(__file__).resolve().parents[3]
    data_file = root / "database/data/track154-temple-place-review.json"
    seed_file = root / "database/migrations" / tpl.SEED_FILE_NAME
    if not (data_file.exists() or seed_file.exists()):
        pytest.skip("reviewed data not generated yet (needs the live lookup and curator review)")
    assert data_file.exists() and seed_file.exists()
    parsed = tpl.PlaceReviewData.model_validate_json(data_file.read_text("utf-8"))
    assert all(e.reviewed_by and e.reviewed_at for e in parsed.entries)
    assert parsed.approvals
    assert tpl.embedded_matches_json(seed_file.read_text("utf-8"), data_file.read_text("utf-8"))
    assert seed_file.read_text("utf-8") == tpl.render_seed_sql(parsed)


def test_auto_confirmed_lines_show_google_address_for_curator_check():
    a, b = target("Alpha Temple"), target("Beta Temple")
    data = tpl.CandidateFile(records=[record(a, cand("pa", "Alpha Temple", "Hill Rd, Townville, TN")), record(b)])
    lines = tpl.auto_confirmed_lines(tpl.triage([a, b], data))
    assert lines == ["Alpha Temple (Townville, Tamil Nadu) -> Alpha Temple | Hill Rd, Townville, TN | pa"]
