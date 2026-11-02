"""Regression: literal % in a parameterised psycopg query is read as a placeholder."""

import inspect
import re
from typing import Any

from scripts.evaluate_retrieval import search_macro_documents


def test_macro_search_sql_escapes_literal_percent() -> None:
    source = inspect.getsource(search_macro_documents)
    stray = re.findall(r"%", re.sub(r"%%|%s", "", source))
    assert stray == [], "literal % must be written %% in parameterised psycopg SQL"


def test_fetch_macro_scores_sql_escapes_literal_percent() -> None:
    from scripts.evaluate_retrieval import fetch_macro_scores

    source = inspect.getsource(fetch_macro_scores)
    stray = re.findall(r"%", re.sub(r"%%|%s", "", source))
    assert stray == []


def test_build_macro_scores_document_embeds_each_distinct_query_once() -> None:
    from scripts.evaluate_retrieval import build_macro_scores_document

    calls: list[str] = []

    def embed(text: str) -> list[float]:
        calls.append(text)
        return [0.0]

    rows = [
        {
            "document_kind": "CYCLE_OVERVIEW",
            "anchor_id": "t1",
            "title": "Cycle",
            "source_chunk_index": 0,
            "similarity": 0.8412345678,
            "lexical": 0.1,
        },
    ]
    benchmarks: list[dict[str, Any]] = [
        {"id": "A", "query": "same", "is_macro": True},
        {"id": "B", "query": "same"},
        {"id": "C", "query": "other"},
    ]
    doc = build_macro_scores_document(
        benchmarks, embed, lambda q, v: rows, {"id": "p", "model_name": "m", "dimensions": 768}, "2026-11-02"
    )
    assert calls == ["same", "other"]
    assert list(doc["probes"]) == ["A", "B", "C"]
    assert doc["probes"]["A"]["is_macro"] is True and doc["probes"]["B"]["is_macro"] is False
    assert doc["probes"]["A"]["candidates"][0]["similarity"] == 0.841235
    assert doc["profile"]["dimensions"] == 768 and doc["schema"] == 1
