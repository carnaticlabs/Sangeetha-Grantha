"""Embeddings package for semantic search and retrieval in Sangita Grantha."""

from .context_formatter import format_composition_overview, format_section_passage
from .gemini_embedder import GeminiEmbedder

__all__ = [
    "format_composition_overview",
    "format_section_passage",
    "GeminiEmbedder",
]
