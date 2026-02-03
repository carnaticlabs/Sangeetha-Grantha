| Metadata | Value |
|:---|:---|
| **Status** | Archived |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-03 |
| **Author** | Sangeetha Grantha Team |

# Koog Technical Integration Proposal


---


**Date:** 2026-01-16
**Status:** DRAFT
**Context:** Technical evaluation of [Koog](https://docs.koog.ai/) for Sangeetha-Grantha.

## 1. Executive Summary

Koog is a **Kotlin-first framework** by JetBrains designed for building AI agents. It emphasizes type safety, modularity, and seamless integration with the JVM ecosystem.

**Verdict:** **High Potential for Backend Integration.**
Sangeetha-Grantha's backend is built on **Kotlin + Ktor**, which is the exact "sweet spot" for Koog. While the frontend (React/TypeScript) cannot directly use Koog's Kotlin DSL (unless migrated to Kotlin Multiplatform), the backend can leverage Koog to orchestrate complex AI workflows, exposing them as APIs to the frontend.

## 2. Project Context & Compatibility

| Component | Sangeetha-Grantha Technology | Koog Compatibility |
| :--- | :--- | :--- |
| **Backend** | **Kotlin / Ktor** | **Excellent.** Koog has native Ktor support, Kotlin DSL, and JVM optimization. |
| **Frontend** | **React / TypeScript** | **Low (Direct).** Koog's client agents run on JVM/Kotlin. React would interact via API. |
| **AI Goals** | Intelligent Content Ingestion | **High.** Koog's "Graph Workflows" and "Agent" models fit complex ingestion pipelines well. |

### Current Stack Highlights
- **Backend:** `libs.ktor.server` suggests a lightweight, async backend. Koog fits this model better than heavier frameworks like LangChain4j (Java-centric).
- **Frontend:** `sangita-admin-web` uses `@google/genai` directly. Moving AI logic to a backend Koog agent would centralize control, secrets (API keys), and prompt versioning.

## 3. Key Koog Capabilities for Sangeetha-Grantha

Based on the [feature set](https://docs.koog.ai/key-features/), these are relevant:

1.  **Type-Safe DSL:** Define agent prompts and tools using Kotlin code, keeping prompt engineering close to business logic and refactor-safe.
2.  **Ktor Integration:** Can be embedded directly into the existing `modules/backend/api` service without a separate deployment.
3.  **Graph Workflows:** Perfect for the "Intelligent Content Ingestion" feature which likely involves sequential steps (Search -> Scrape -> Validate -> Structure).
4.  **Intelligent History Compression:** Useful if implementing a "Chat with Grantha" feature where context length is a concern.
5.  **LLM Switching:** Abstract functionality from specific providers (e.g., switch between Gemini Pro and Flash or OpenAI without code changes).

## 4. Integration Options

### Option A: Embedded Backend Agent (Recommended)
Embed Koog directly into `modules/backend/api`.
*   **Architecture:** The Ktor server hosts Koog agents. Frontend sends simple REST/WebSocket requests; Koog handles the LLM orchestration.
*   **Pros:**
    *   No new infrastructure (runs in existing JVM).
    *   Access to `modules/backend/dal` and domain logic directly (for "Tools").
    *   Type-safe tooling.
*   **Cons:** Increases backend complexity slightly.

### Option B: Standalone Agent Service
Create a new module `modules/services/agent` dedicated to Koog.
*   **Pros:** Decouples AI logic from the main API. Scaled independently.
*   **Cons:** Overengineering for the current stage. Adds deployment operational overhead.

## 5. Proposed "Leap" Feature: Intelligent Ingestion Pipeline

We can replace/augment the current `WebScrapingService` with a **Koog Graph Agent**.

**Workflow:**
1.  **Input:** User provides a URL or Song Name.
2.  **Koog Agent Steps:**
    *   *Node 1 (Browser Tool):* Fetch page content.
    *   *Node 2 (Parser Agent):* Extract structured data (lyrics, raga, tala).
    *   *Node 3 (Critic Agent):* Validate against internal DB (check if Raga exists in `dal`).
    *   *Node 4 (Formatter):* Return JSON to frontend.
3.  **Output:** Pre-filled `Krithi` object for the editor.

## 6. Implementation Plan (Draft)

1.  **Dependency:** Add Koog dependencies to `modules/backend/api/build.gradle.kts`.
    ```kotlin
    implementation("org.jetbrains.koog:koog-core:x.y.z")
    implementation("org.jetbrains.koog:koog-ktor-server:x.y.z")
    ```
2.  **Configuration:** Configure LLM providers (Gemini) in `application.conf` / `config`.
3.  **Development:**
    *   Create `modules/backend/api/src/main/kotlin/.../agents/IngestionAgent.kt`.
    *   Define tools (wrapping `WebScrapingService`).
4.  **API Exposure:** Create a Ktor route `/api/v1/agent/ingest` that triggers the Koog workflow.

## 7. Recommendation

**Adopt Option A.**
Start by adding Koog to the backend to power the **Content Ingestion** feature. It aligns perfectly with the stack and offers "guardrails" for AI interactions that raw API calls lack.
