from scripts.embed_catalogue import document_needs_refresh


def test_matching_vector_hash_does_not_hide_mutated_document_text():
    assert document_needs_refresh(("doc", "vector", "expected", "expected", "changed", "lyrics"), "expected", "lyrics")


def test_matching_text_does_not_hide_stale_document_hash():
    assert document_needs_refresh(("doc", "vector", "expected", "old", "expected", "lyrics"), "expected", "lyrics")


def test_overview_full_source_change_beyond_excerpt_is_refreshed():
    assert document_needs_refresh(
        ("doc", "vector", "expected", "expected", "expected", "old lyrics"), "expected", "new lyrics"
    )


def test_unchanged_document_and_vector_are_not_sent_again():
    assert not document_needs_refresh(
        ("doc", "vector", "expected", "expected", "expected", "lyrics"), "expected", "lyrics"
    )


def test_missing_vector_is_regenerated():
    assert document_needs_refresh(("doc", None, None, "expected", "expected", "lyrics"), "expected", "lyrics")
