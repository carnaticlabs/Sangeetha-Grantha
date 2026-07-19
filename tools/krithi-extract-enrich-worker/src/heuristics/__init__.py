"""Heuristic resolvers shared by the extraction strategies."""

from .metadata_heuristics import URL_COMPOSER_MAP, infer_composer_from_url, is_valid_segment_title

__all__ = ["URL_COMPOSER_MAP", "infer_composer_from_url", "is_valid_segment_title"]
