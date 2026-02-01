| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-01 |
| **Author** | Sangita Grantha Team |

# Scraping Robustness and Refactor Implementation (TRACK-032)

## 1. Purpose
Enhance `WebScrapingService` robustness by introducing architectural improvements such as caching, rate limiting, and structured text extraction, to support reliable multi-language lyric extraction and reduce Gemini API errors.

## 2. Changes

### 2.1 Scraping Infrastructure
- **`ScrapeCache`**: In-memory cache for scrape results to avoid redundant API calls during development and retries.
- **`HtmlTextExtractor`**: Refactored HTML extraction logic (likely using JSoup or similar) for cleaner text input to the LLM.
- **`ScrapeJsonSanitizer`**: dedicated utility for cleaning JSON responses from Gemini.
- **`GeminiRateLimiter`**: Token bucket or similar mechanism to respect Gemini API limits.

### 2.2 Service Updates
- **`WebScrapingService`**: Updated to use the new components (`HtmlTextExtractor`, `ScrapeCache`, `GeminiRateLimiter`).
- **`GeminiApiClient`**: Enhanced error handling and rate limit integration.
- **`ApiEnvironment`**: Configuration for cache TTL, rate limits.
- **`AppModule`**: DI wiring for new components.

### 2.3 Dependencies
- Added dependencies for caching/scraping in `libs.versions.toml` and `build.gradle.kts`.

## 3. Commit Reference
This document serves as the `Ref` for the commit.

## 4. Progress
- [x] Refactor scraping logic into dedicated components.
- [x] Implement caching and rate limiting.
- [x] Update `WebScrapingService` to utilize new infrastructure.
