| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Koog Integration Analysis and Options

| Metadata | Value |
|:---|:---|
| **Status** | Analysis |
| **Version** | 1.1 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |

> - [Intelligent Content Ingestion](../../intelligent-content-ingestion.md)
> - [Gemini Integration ADR](../../../../02-architecture/decisions/ADR-006-gemini-integration.md)
> - [AI Integration Opportunities](../../../../09-ai/integration-opportunities.md)

---

## 1. Executive Summary

Koog is a JetBrains open source framework for building AI agents in Kotlin. It provides a Kotlin DSL, multiplatform support, orchestration features (graph workflows, tool calling, streaming), and enterprise integration points (Ktor, Spring Boot, OpenTelemetry). It also supports multiple LLM providers, including Google (Gemini), which aligns with the current Sangeetha Grantha stack.

This document maps Koog capabilities to Sangeetha Grantha needs and presents integration options. The highest near-term value is in AI workflow orchestration for content ingestion and validation, plus observability and reliability improvements. A phased approach is recommended: validate Koog in a POC for ingestion and validation workflows, then adopt selectively for production pipelines if benefits are confirmed.

---

## 2. Koog Capabilities (from official docs)

Reference: https://docs.koog.ai/key-features/

### 2.1 Core Features

* **Multiplatform development**: JVM, JS, WasmJS, Android, iOS targets via Kotlin Multiplatform.
* **Reliability and fault tolerance**: retries and agent persistence to restore execution state.
* **Intelligent history compression**: token optimization for long-running conversations.
* **Enterprise-ready integrations**: Spring Boot and Ktor integration.
* **Observability**: OpenTelemetry exporters and support for W&B Weave, Langfuse.
* **LLM switching**: change models/providers without losing conversation history.
* **Kotlin-first DSL**: type-safe agent definition for JVM and Kotlin apps.
* **Model Context Protocol (MCP)**: use MCP tools in agents.
* **Knowledge retrieval and memory**: vector embeddings, ranked document storage, shared memory.
* **Streaming API**: real-time response processing with parallel tool calls.
* **Modular features**: composable capabilities per agent.
* **Graph workflows**: define complex agent behavior in graphs.
* **Custom tools**: integrate external systems and APIs as tools.
* **Comprehensive tracing**: detailed, configurable agent tracing.

### 2.2 Supported LLM Providers

Reference: https://docs.koog.ai/llm-providers/

Koog supports multiple providers, including:
* OpenAI (including Azure OpenAI)
* Anthropic
* Google (Gemini)
* DeepSeek
* OpenRouter
* Amazon Bedrock
* Mistral
* Alibaba (DashScope)
* Ollama (local models)

---

## 3. Current Project State

### 3.1 Existing AI Integration

* **LLM Provider**: Google Gemini (2.0 Flash, 1.5 Pro).
* **Integration Pattern**: direct API calls via `GeminiApiClient`.
* **Key Services**:
  * `TransliterationService`
  * `WebScrapingService`
* **Runtime Stack**: Ktor-based backend.

### 3.2 Gaps and Opportunities

* **Workflow orchestration**: current flows are mostly linear, with limited retry and stateful orchestration.
* **Observability**: no standard tracing for AI workflows beyond application logs.
* **RAG and shared memory**: no shared, reusable knowledge layer for ragas, talas, composers, or citations.
* **Provider flexibility**: direct Gemini integration limits model switching and experimentation.
* **Complex pipelines**: ingestion and validation could benefit from explicit graph workflows and tool calls.

---

## 4. Where Koog Fits Best

### 4.1 Ingestion and Validation Pipelines

Koog graph workflows and tool calling are a strong fit for multi-step pipelines:

* Scrape HTML -> extract structured JSON -> validate against reference data -> persist.
* Retry and recovery with persistence when upstream sources fail.
* Streaming responses for long extraction tasks.

### 4.2 Knowledge Retrieval and Memory

Koog memory features align with domain-specific needs:

* Store ragas, talas, composers, and canonical metadata as embeddings.
* Use ranked retrieval for validation or annotation tasks.
* Preserve shared memory across agent runs to reduce repeated prompts.

### 4.3 Observability and Reliability

* OpenTelemetry support enables consistent tracing of AI steps.
* Tracing and retries can reduce operational risk in ingestion workflows.

### 4.4 Multi-Provider Flexibility

* Keep Gemini as primary provider while enabling selective routing to other models for evaluation or cost optimization.
* Allows safe experimentation without changing core service interfaces.

---

## 5. Integration Options

### Option A: Koog for AI Pipelines (Targeted Runtime Integration)

**Scope**: Use Koog for ingestion and validation workflows only. Keep existing transliteration and standard endpoints on `GeminiApiClient` initially.

**Use Cases**:
* Web scraping and extraction pipeline.
* Metadata normalization and validation workflows.

**Pros**:
* High value with contained scope.
* Minimal disruption to existing API endpoints.
* Clear ROI from reliability and observability improvements.

**Cons**:
* Two AI integration paths in the codebase.
* Some duplication of prompt logic until refactored.

**Effort**: Medium

---

### Option B: Koog for Batch and Offline Tools (Non-Production First)

**Scope**: Use Koog for internal tooling and offline processing (imports, migrations, validation runs). Leave runtime services unchanged.

**Use Cases**:
* Bulk ingestion of legacy content.
* Batch validation and consistency checks.
* One-off data migrations or remediation.

**Pros**:
* Lowest production risk.
* Fast learning and experimentation.
* Allows gradual evolution of prompts and tool APIs.

**Cons**:
* Runtime services do not benefit immediately.
* Requires separate operational pipeline.

**Effort**: Low to Medium

---

### Option C: Koog as Primary AI Abstraction (Full Integration)

**Scope**: Replace `GeminiApiClient` with Koog in all AI services.

**Pros**:
* Unified AI interface.
* Centralized observability and retries.
* Provider flexibility across the entire platform.

**Cons**:
* Larger refactor.
* Must validate performance and costs carefully.
* Higher change risk.

**Effort**: High

---

### Option D: POC First, Then Decide

**Scope**: Build a small POC around one workflow (scraping + validation) and evaluate.

**POC Goals**:
* Confirm Koog integration with Ktor in this codebase.
* Validate graph workflows and tool calling with real data.
* Measure improvements in error handling and traceability.

**Effort**: Low to Medium

---

## 6. Recommended Path

**Recommended**: Option D -> Option A

1. Build a narrow POC for ingestion and validation.
2. If outcomes are positive, use Koog for AI pipeline orchestration in production while keeping transliteration on Gemini direct until ready to consolidate.
3. Evaluate full abstraction (Option C) after 1 to 2 iterations.

---

## 7. Implementation Outline (Option A)

### 7.1 Architecture Sketch

```
Ktor API
  |-- GeminiApiClient (existing)
  |-- KoogAgentPipeline (new)
         |-- ScrapeTool
         |-- ExtractTool
         |-- ValidationTool
         |-- ReferenceDataRetriever
```

### 7.2 Key Tasks

* Create a Koog module for agent pipelines (separate package or module).
* Implement tools for scrape, extract, validate, persist.
* Add OpenTelemetry tracing for pipeline stages.
* Integrate Koog provider config to keep Gemini as default.

---

## 8. Risks and Mitigations

* **API churn**: Pin Koog versions, validate changelogs before upgrades.
* **Operational complexity**: Start with one pipeline to limit blast radius.
* **Prompt divergence**: Create shared prompt templates for Koog and direct Gemini clients.
* **Latency variance**: Benchmark ingestion workflows before enabling in production.

---

## 9. Decision Criteria

* **Value**: Does Koog reduce ingestion failures and manual rework?
* **Reliability**: Are retries and state persistence effective with real data?
* **Observability**: Are traces usable for debugging and audits?
* **Cost**: Is multi-provider routing needed beyond Gemini?
* **Team Fit**: Is the Kotlin DSL understandable and maintainable for the team?

---

## 10. Open Questions

* Which ingestion workflow is the best first POC candidate?
* Do we want Koog memory to be backed by an existing vector store, or should we add a new one?
* What level of observability is required for AI workflows (logs only vs. full tracing)?
* Is multi-provider support a must-have or a nice-to-have this year?

---

## 11. References

* Koog Key Features: https://docs.koog.ai/key-features/
* Koog LLM Providers: https://docs.koog.ai/llm-providers/
