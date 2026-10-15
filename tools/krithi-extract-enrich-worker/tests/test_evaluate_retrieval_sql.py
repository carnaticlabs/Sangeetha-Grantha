"""Regression: literal % in a parameterised psycopg query is read as a placeholder."""

import inspect
import re

from scripts.evaluate_retrieval import search_macro_documents


def test_macro_search_sql_escapes_literal_percent() -> None:
    source = inspect.getsource(search_macro_documents)
    stray = re.findall(r"%", re.sub(r"%%|%s", "", source))
    assert stray == [], "literal % must be written %% in parameterised psycopg SQL"
