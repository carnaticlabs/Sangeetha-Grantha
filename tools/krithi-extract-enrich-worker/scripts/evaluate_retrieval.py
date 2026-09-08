#!/usr/bin/env python3
"""Evaluation runner for semantic and hybrid retrieval in Sangita Grantha.

Runs benchmark queries against the PostgreSQL pgvector store, measuring Recall@1,
Recall@5, and Mean Reciprocal Rank (MRR) for dense semantic search, hybrid RRF search,
or both.

Usage:
  uv run python scripts/evaluate_retrieval.py
  uv run python scripts/evaluate_retrieval.py --mode hybrid
  uv run python scripts/evaluate_retrieval.py --mode semantic
  uv run python scripts/evaluate_retrieval.py --mode all
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import re
import sys
from pathlib import Path
from typing import Any

import psycopg
from psycopg.rows import dict_row

# Add src to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.embeddings.context_formatter import strip_diacritics
from src.embeddings.gemini_embedder import GeminiEmbedder

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("evaluate_retrieval")

DEFAULT_DB_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql://postgres:postgres@localhost:5432/sangita_grantha",
)
BENCHMARK_PATH = Path(__file__).parent.parent / "evals" / "retrieval_benchmarks.json"


def normalize_text(text: str | None) -> str:
    """Normalizes text by removing diacritics, whitespace, punctuation, and v/w transliteration."""
    if not text:
        return ""
    text = strip_diacritics(text).lower()
    text = text.replace("w", "v")
    return re.sub(r"[^a-z0-9]", "", text)


def load_benchmarks() -> list[dict[str, Any]]:
    if not BENCHMARK_PATH.exists():
        raise FileNotFoundError(f"Benchmark file not found at {BENCHMARK_PATH}")
    with open(BENCHMARK_PATH, encoding="utf-8") as f:
        return json.load(f)


def search_similar(
    conn: psycopg.Connection,
    query_vector: list[float],
    top_k: int = 5,
) -> list[dict[str, Any]]:
    """Runs cosine similarity ANN search against document_embeddings, deduplicating by krithi."""
    query = """
        WITH candidates AS (
            SELECT 
                k.id,
                k.title,
                c.name AS composer,
                r.name AS raga,
                d.document_kind,
                d.indexed_content,
                1 - (e.embedding <=> %s::vector(768)) AS similarity
            FROM document_embeddings e
            JOIN search_documents d ON e.document_id = d.id
            JOIN krithis k ON d.krithi_id = k.id
            JOIN composers c ON k.composer_id = c.id
            LEFT JOIN ragas r ON k.primary_raga_id = r.id
            JOIN embedding_profiles p ON e.profile_id = p.id
            WHERE p.is_active = true AND d.is_published = true
            ORDER BY e.embedding <=> %s::vector(768) ASC
            LIMIT %s
        ),
        ranked AS (
            SELECT 
                *, 
                ROW_NUMBER() OVER (PARTITION BY id ORDER BY similarity DESC) AS krithi_rank
            FROM candidates
        )
        SELECT id, title, composer, raga, document_kind, indexed_content, similarity
        FROM ranked
        WHERE krithi_rank = 1
        ORDER BY similarity DESC
        LIMIT %s
    """
    vec_str = "[" + ",".join(str(v) for v in query_vector) + "]"
    candidate_limit = max(top_k * 25, 100)
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(query, (vec_str, vec_str, candidate_limit, top_k))
        return cur.fetchall()


def search_hybrid(
    conn: psycopg.Connection,
    query_text: str,
    query_vector: list[float],
    top_k: int = 5,
) -> list[dict[str, Any]]:
    """Runs hybrid search combining dense vector cosine similarity and pg_trgm lexical similarity via RRF."""
    vec_str = "[" + ",".join(str(v) for v in query_vector) + "]"
    candidate_limit = max(top_k * 25, 100)
    tokens = [t for t in re.split(r"\s+", query_text.strip()) if len(t) >= 2]
    token_filter = " OR (" + " AND ".join(["d.indexed_content ILIKE %s"] * len(tokens)) + ")" if len(tokens) > 1 else ""
    token_params = [f"%{t}%" for t in tokens] if len(tokens) > 1 else []

    sql = f"""
        WITH dense_docs AS (
            SELECT
                d.krithi_id,
                d.document_kind,
                d.indexed_content,
                1 - (e.embedding <=> %s::vector(768)) AS sim_score,
                ROW_NUMBER() OVER (ORDER BY e.embedding <=> %s::vector(768) ASC) AS dense_rank
            FROM document_embeddings e
            JOIN search_documents d ON e.document_id = d.id
            JOIN embedding_profiles p ON e.profile_id = p.id
            WHERE p.is_active = true AND d.is_published = true
            ORDER BY e.embedding <=> %s::vector(768) ASC
            LIMIT %s
        ),
        dense_candidates AS (
            SELECT
                krithi_id,
                MAX(sim_score) AS max_sim,
                MIN(dense_rank) AS dense_rank,
                (ARRAY_AGG(indexed_content ORDER BY sim_score DESC))[1] AS snippet,
                (ARRAY_AGG(document_kind ORDER BY sim_score DESC))[1] AS kind
            FROM dense_docs
            GROUP BY krithi_id
        ),
        lexical_docs AS (
            SELECT
                d.krithi_id,
                d.document_kind,
                d.indexed_content,
                word_similarity(%s, d.indexed_content) AS lex_score,
                ROW_NUMBER() OVER (ORDER BY word_similarity(%s, d.indexed_content) DESC) AS lex_rank
            FROM search_documents d
            WHERE d.is_published = true
              AND (
                  word_similarity(%s, d.indexed_content) >= 0.3
                  OR d.indexed_content ILIKE '%%' || %s || '%%'
                  {token_filter}
              )
            ORDER BY word_similarity(%s, d.indexed_content) DESC
            LIMIT %s
        ),
        lexical_candidates AS (
            SELECT
                krithi_id,
                MAX(lex_score) AS max_lex,
                MIN(lex_rank) AS lex_rank,
                (ARRAY_AGG(indexed_content ORDER BY lex_score DESC))[1] AS snippet,
                (ARRAY_AGG(document_kind ORDER BY lex_score DESC))[1] AS kind
            FROM lexical_docs
            GROUP BY krithi_id
        ),
        fused AS (
            SELECT
                COALESCE(d.krithi_id, l.krithi_id) AS krithi_id,
                COALESCE(d.max_sim, 0.0) AS similarity,
                COALESCE(l.max_lex, 0.0) AS lexical_score,
                (
                    COALESCE(1.0 / (60.0 + d.dense_rank), 0.0) +
                    COALESCE(1.0 / (60.0 + l.lex_rank), 0.0)
                ) AS rrf_score,
                COALESCE(d.snippet, l.snippet) AS indexed_content,
                COALESCE(d.kind, l.kind) AS document_kind
            FROM dense_candidates d
            FULL OUTER JOIN lexical_candidates l ON d.krithi_id = l.krithi_id
        )
        SELECT
            k.id,
            k.title,
            c.name AS composer,
            r.name AS raga,
            f.document_kind,
            f.indexed_content,
            f.similarity,
            f.lexical_score,
            f.rrf_score
        FROM fused f
        JOIN krithis k ON f.krithi_id = k.id
        JOIN composers c ON k.composer_id = c.id
        LEFT JOIN ragas r ON k.primary_raga_id = r.id
        ORDER BY f.rrf_score DESC
        LIMIT %s
    """
    params = (
        [
            vec_str,
            vec_str,
            vec_str,
            candidate_limit,
            query_text,
            query_text,
            query_text,
            query_text,
        ]
        + token_params
        + [
            query_text,
            candidate_limit,
            top_k,
        ]
    )
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(sql, params)
        return cur.fetchall()


def evaluate_item(
    item: dict[str, Any],
    results: list[dict[str, Any]],
) -> tuple[bool, float]:
    """Returns (hit_at_k, reciprocal_rank) matching normalized criteria."""
    unexpected_composer = item.get("unexpected_composer")
    if unexpected_composer:
        norm_unexp = normalize_text(unexpected_composer)
        for res in results:
            if norm_unexp and norm_unexp in normalize_text(res.get("composer")):
                return False, 0.0
        return True, 1.0

    expected_title = normalize_text(item.get("expected_title_contains"))
    expected_composer = normalize_text(item.get("expected_composer"))
    expected_raga = normalize_text(item.get("expected_raga"))

    for rank, res in enumerate(results, start=1):
        res_title = normalize_text(res.get("title"))
        res_composer = normalize_text(res.get("composer"))
        res_raga = normalize_text(res.get("raga"))

        match = True
        if expected_title and expected_title not in res_title:
            match = False
        if expected_composer and expected_composer not in res_composer:
            match = False
        if expected_raga and expected_raga not in res_raga:
            match = False

        if match:
            return True, 1.0 / rank

    return False, 0.0


def run_benchmark_suite(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder,
    benchmarks: list[dict[str, Any]],
    mode: str,
    top_k: int,
) -> tuple[float, float, float]:
    hits_at_1 = 0
    hits_at_k = 0
    rr_sum = 0.0

    print("\n" + "=" * 80)
    print(f"EVALUATION: MODE={mode.upper()} (Top-{top_k} Cutoff)")
    print("=" * 80)

    for item in benchmarks:
        qid = item["id"]
        query = item["query"]
        print(f'\nQuery [{qid}]: "{query}"')

        query_vec = embedder.embed_query(query)
        if mode == "hybrid":
            results = search_hybrid(conn, query, query_vec, top_k=top_k)
        else:
            results = search_similar(conn, query_vec, top_k=top_k)

        hit, rr = evaluate_item(item, results)
        if hit:
            hits_at_k += 1
            if rr == 1.0:
                hits_at_1 += 1
        rr_sum += rr

        status = "HIT" if hit else "MISS"
        print(f"-> Status: {status} (RR: {rr:.2f})")
        for r_idx, res in enumerate(results, start=1):
            if "rrf_score" in res and res["rrf_score"] is not None:
                print(
                    f"   #{r_idx}: {res['title']} | {res['composer']} | {res['raga']} "
                    f"(RRF: {res['rrf_score']:.4f} | Dense: {float(res['similarity']):.3f} | "
                    f"Lex: {float(res['lexical_score']):.3f})"
                )
            else:
                sim = float(res["similarity"])
                print(f"   #{r_idx}: {res['title']} | {res['composer']} | {res['raga']} (Sim: {sim:.3f})")

    total = len(benchmarks)
    recall_1 = (hits_at_1 / total) * 100.0
    recall_k = (hits_at_k / total) * 100.0
    mrr = rr_sum / total

    print("\n" + "-" * 80)
    print(f"METRICS FOR {mode.upper()} RETRIEVAL")
    print(f"Total Queries Evaluated: {total}")
    print(f"Recall@1:                {recall_1:.1f}% ({hits_at_1}/{total})")
    print(f"Recall@{top_k}:                {recall_k:.1f}% ({hits_at_k}/{total})")
    print(f"Mean Reciprocal Rank:    {mrr:.3f}")
    print("-" * 80)

    return recall_1, recall_k, mrr


def main():
    parser = argparse.ArgumentParser(description="Evaluate semantic and hybrid retrieval accuracy")
    parser.add_argument("--db-url", type=str, default=DEFAULT_DB_URL, help="PostgreSQL connection string")
    parser.add_argument("--k", type=int, default=5, help="Top-K evaluation cutoff")
    parser.add_argument(
        "--mode", type=str, choices=["hybrid", "semantic", "all"], default="hybrid", help="Search mode to evaluate"
    )
    parser.add_argument("--model", type=str, default="gemini-embedding-2", help="Embedding model name")
    parser.add_argument("--dims", type=int, default=768, help="Output dimensions")
    args = parser.parse_args()

    benchmarks = load_benchmarks()
    logger.info("Loaded %d benchmark queries from %s", len(benchmarks), BENCHMARK_PATH)

    with psycopg.connect(args.db_url) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT count(*) FROM document_embeddings")
            row = cur.fetchone()
            count = row[0] if row else 0
            logger.info("Current total embeddings in local database: %d", count)

        if count == 0:
            logger.warning(
                "No embeddings found in document_embeddings table! "
                "Run `scripts/embed_catalogue.py --limit N` first to backfill data."
            )
            sys.exit(0)

        embedder = GeminiEmbedder(model=args.model, dimensions=args.dims)

        if args.mode in ("hybrid", "all"):
            run_benchmark_suite(conn, embedder, benchmarks, mode="hybrid", top_k=args.k)

        if args.mode in ("semantic", "all"):
            run_benchmark_suite(conn, embedder, benchmarks, mode="semantic", top_k=args.k)


if __name__ == "__main__":
    main()
