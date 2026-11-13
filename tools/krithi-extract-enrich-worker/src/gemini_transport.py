"""google-genai 3.x client options that keep this worker on its 2.x transport.

3.0 makes httpx2 the default HTTP client and ``v1`` the default Gemini
Developer API version. Respx stubs the SDK on httpx, and the enricher and
embedder call the Developer API routes that 2.x used (``v1beta``). Vertex
clients keep the SDK default (``v1beta1``).
"""

from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from google.genai.types import HttpOptions

DEVELOPER_API_VERSION = "v1beta"


def genai_http_options(*, timeout_ms: int | None = None, api_version: str | None = None) -> HttpOptions:
    """Build client HTTP options on the httpx transport respx can intercept."""
    import httpx
    from google.genai.types import HttpOptions

    kwargs: dict[str, object] = {
        "httpx_client": httpx.Client(),
        "httpx_async_client": httpx.AsyncClient(),
    }
    if timeout_ms is not None:
        kwargs["timeout"] = timeout_ms
    if api_version is not None:
        kwargs["api_version"] = api_version
    return HttpOptions(**kwargs)


def developer_api_http_options(*, timeout_ms: int | None = None) -> HttpOptions:
    """Developer API client options: httpx transport and the pre-3.0 API version."""
    return genai_http_options(timeout_ms=timeout_ms, api_version=DEVELOPER_API_VERSION)


def vertex_http_options(*, timeout_ms: int | None = None) -> HttpOptions:
    """Vertex client options: httpx transport, SDK default API version."""
    return genai_http_options(timeout_ms=timeout_ms)
