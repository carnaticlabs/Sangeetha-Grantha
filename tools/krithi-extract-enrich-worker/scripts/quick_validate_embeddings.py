#!/usr/bin/env python3
"""Quick validation of the 17,891 live embeddings generated so far."""

# ruff: noqa
import os
import sys
from pathlib import Path
import psycopg
from psycopg.rows import dict_row

WORKER_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(WORKER_ROOT))

from src.embeddings.gemini_embedder import GeminiEmbedder

test_queries = [
    {
        "query": "Chidambaram cosmic dance of Shiva in Kedaram raga",
        "expected": "Ananda naTana prakASaM",
        "theme": "Thematic Kshetra & Dance",
    },
    {
        "query": "Thyagaraja Pancharatna in Sri ragam praising all the great noble souls",
        "expected": "Endaro Mahaanubhaavulu",
        "theme": "Direct Concept & Pancharatnam",
    },
    {
        "query": "First Pancharatna krithi in Nattai raga praising Lord Rama",
        "expected": "jagadAnanda kAraka",
        "theme": "Raga & Form Matching",
    },
    {
        "query": "Dikshitar krithi that invoked rain in Amritavarshini raga",
        "expected": "AnandAmRtAkarshiNi",
        "theme": "Musicological Lore & Raga",
    },
    {
        "query": "Kamalamba Navavaranam in Kalyani raga",
        "expected": "kamalAmbAM bhajarE",
        "theme": "Vibhakti & Navavarnam Cycle",
    },
    {
        "query": "Thyagaraja asking Rama why he will not show his smiling face in Abheri",
        "expected": "Nagu Momu Kana Leni",
        "theme": "Bhavana / Meaning Search",
    },
    {
        "query": "Is there salvation without knowledge of music and devotion in Saramati raga",
        "expected": "Mokshamu Galadaa",
        "theme": "Philosophical / Meaning Search",
    },
]


def main():
    embedder = GeminiEmbedder()
    conn = psycopg.connect("postgresql://postgres:postgres@localhost:5432/sangita_grantha")

    print(f"\n{'=' * 75}")
    print(f"LIVE VALIDATION OF EMBEDDINGS ({len(test_queries)} Test Queries)")
    print(f"{'=' * 75}\n")

    correct_top1 = 0
    correct_top3 = 0

    for i, item in enumerate(test_queries, 1):
        q = item["query"]
        expected = item["expected"]
        theme = item["theme"]

        print(f'[{i}/{len(test_queries)}] Query: "{q}"')
        print(f"    Target Expectation: {expected} ({theme})")

        q_vec = embedder.embed_query(q)
        vec_str = "[" + ",".join(str(v) for v in q_vec) + "]"

        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(
                f"""
                SELECT 
                    k.title,
                    c.name AS composer,
                    r.name AS raga,
                    d.document_kind,
                    LEFT(d.indexed_content, 110) AS snippet,
                    1 - (e.embedding <=> '{vec_str}'::vector(768)) AS similarity
                FROM document_embeddings e
                JOIN search_documents d ON e.document_id = d.id
                JOIN krithis k ON d.krithi_id = k.id
                JOIN composers c ON k.composer_id = c.id
                LEFT JOIN ragas r ON k.primary_raga_id = r.id
                ORDER BY e.embedding <=> '{vec_str}'::vector(768) ASC
                LIMIT 3;
                """
            )
            hits = cur.fetchall()

        hit_titles = [h["title"] for h in hits]
        is_top1 = expected.lower() in hits[0]["title"].lower() if hits else False
        is_top3 = any(expected.lower() in t.lower() for t in hit_titles)

        if is_top1:
            correct_top1 += 1
            status = "✅ EXACT #1 HIT"
        elif is_top3:
            correct_top3 += 1
            status = "🟡 TOP 3 HIT"
        else:
            status = "❌ NOT IN TOP 3"

        print(f"    Result: {status}")
        for rank, h in enumerate(hits, 1):
            print(
                f"       #{rank} ({h['similarity']:.4f}) | {h['title']} | {h['composer']} | {h.get('raga') or 'N/A'} [{h['document_kind']}]"
            )
            print(f"          Snippet: {h['snippet']}...")
        print("-" * 75)

    print(f"\nSummary:")
    print(f"Top-1 Accuracy: {correct_top1}/{len(test_queries)} ({correct_top1 / len(test_queries) * 100:.1f}%)")
    print(
        f"Top-3 Accuracy: {(correct_top1 + correct_top3)}/{len(test_queries)} ({(correct_top1 + correct_top3) / len(test_queries) * 100:.1f}%)"
    )
    print(f"{'=' * 75}\n")


if __name__ == "__main__":
    main()
