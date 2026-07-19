---
name: python-extraction-worker
description: Guidelines for the Python extraction and enrichment worker (tools/krithi-extract-enrich-worker).
---

# Python Extraction & Enrichment Worker Guidelines

This skill defines python development standards and guidelines for the document parser and metadata enricher located in `tools/krithi-extract-enrich-worker/`.

## 1. Programming Paradigm: Pydantic & Class-Based "Gold-Standard"
To prevent code drift and untyped dictionary parsing bugs, Python code in this module must adhere to the following rules:
- **Class-Based Design**: Encapsulate logic in domain-specific class structures rather than writing procedural scripts. Avoid parsing data as unstructured `dict` objects.
- **Pydantic Validation**: All data flow schemas must extend `pydantic.BaseModel`. Declare strict typing, defaults, and optional fields. Leverage Pydantic's built-in validation features.
- **Per-Item Failure Isolation**: One unparseable PDF, page, or krithi is caught, logged with enough context to locate the source, and recorded as a failed unit — it must never abort the batch or silently drop the items that parsed cleanly. A batch result reports what succeeded *and* what failed; a partial run that looks like a clean run is the worst outcome.
- **Example of Gold-Standard Python**:
  ```python
  from typing import List, Optional
  from uuid import UUID
  from pydantic import BaseModel, Field

  class SectionExtraction(BaseModel):
      label: str = Field(..., description="E.g., Pallavi, Anupallavi, Charanam")
      text: str = Field(..., min_length=5, description="The lyric text of the section")
      is_madhyama_kala: bool = False

  class KrithiExtraction(BaseModel):
      title: str
      raga: Optional[str] = None
      composer: Optional[str] = None
      sections: List[SectionExtraction] = Field(default_factory=list)

  class TextBlocker:
      """Class-based extractor encapsulating regex segmenting heuristics."""
      def __init__(self, raw_text: str):
          self.raw_text = raw_text

      def parse_blocks(self) -> List[SectionExtraction]:
          # Perform heuristic splitting, returning validated Pydantic instances
          pass
  ```

## 2. Environment & Dependency Management
- **Mise & UV**: Tooling is controlled via `mise` and **`uv`**. The worker requires **Python 3.14+** (`requires-python = ">=3.14"` in `pyproject.toml`; mypy is pinned to `python_version = "3.14"`).
- Dependency additions must go through `pyproject.toml` and be locked using `uv.lock`.
- To install/update local environments, run `uv sync` or `uv pip install`.

## 3. Core Worker Modules & Logic
- **`structure_parser.py`**: Key parsing logic and section classification heuristics (like `TextBlocker`). Ensure parsing patterns support transliteration collapses (e.g. `c` or `ch` for Charanam/caraNam).
- **`velthuis_decoder.py`**: Handles Velthuis font mapping for Sanskrit text.
- **`diacritic_normalizer.py`**: Performs English/Indic script normalization.
- **`gemini_enricher.py`**: Handles LLM interactions using the `google-genai` 2.x SDK, using structured outputs mapping to Pydantic schemas.

## 4. Testing & Linting
- **Unit & Integration Tests**: Run using `pytest` inside the tool folder.
  - Mock third-party APIs (like Gemini) using `respx`.
  - Use `testcontainers` for running integration tests against a real Postgres container.
- **Formatting & Linting**: Ruff is the standard tool. Run `ruff check . --fix` and `ruff format .`.
- **Static Typing**: Run `mypy .` to enforce strict type definitions.
