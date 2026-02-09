| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha Documentation Index

This documentation tree is the **spec-driven source of truth** for Sangita Grantha.

The **primary PRD** lives at the repo root:
[Sangita Grantha PRD](01-requirements/product-requirements-document.md)

Historical / inherited drafts from earlier projects, and any future archival docs for Sangita Grantha, live under `./archive/`.

---

- **[00 Onboarding](./00-onboarding/README.md)**
  - [Getting Started](./00-onboarding/getting-started.md)
  - [IDE Setup](./00-onboarding/ide-setup.md)
  - [Troubleshooting](./00-onboarding/troubleshooting.md)

- **[01 Requirements](./01-requirements/README.md)**
  - [Product Requirements](./01-requirements/product-requirements-document.md)
  - [Domain Model](./01-requirements/domain-model.md)
  - [Glossary](./01-requirements/glossary.md)
  - [Admin Web PRD](./01-requirements/admin-web/prd.md)
  - [Mobile App PRD](./01-requirements/mobile/prd.md)
  - [Features](./01-requirements/features/README.md)
    - [Commit Guardrails & Workflow Enforcement](./01-requirements/features/commit-guardrails-workflow.md) ✅ *Implemented*
    - [Bulk Import](./01-requirements/features/bulk-import/README.md)
    - [Intelligent Content Ingestion](./01-requirements/features/intelligent-content-ingestion.md)
    - [Advanced Krithi Notation & Transliteration](./01-requirements/features/advanced-krithi-notation-transliteration.md)
  - [Krithi Data Sourcing & Quality](./01-requirements/krithi-data-sourcing/README.md) *(New — Feb 2026)*
    - [Quality Strategy](./01-requirements/krithi-data-sourcing/quality-strategy.md)
    - [Implementation Checklist](./01-requirements/krithi-data-sourcing/implementation-checklist.md)
  - [Tech Debt](./01-requirements/tech-debt/README.md)

- **[02 Architecture](./02-architecture/README.md)**
  - [System Design](./02-architecture/backend-system-design.md)
  - [Tech Stack](./02-architecture/tech-stack.md)
  - [Target Scale Architecture](./02-architecture/scale-target-architecture.md)
  - [Decisions (ADRs)](./02-architecture/decisions/adr-index.md)
  - [Diagrams](./02-architecture/diagrams/README.md)
    - [C4 Model](./02-architecture/diagrams/c4-model.md)
    - [ERD](./02-architecture/diagrams/erd.md)
    - [Flows](./02-architecture/diagrams/flows.md)

- **[03 API](./03-api/README.md)**
  - [Contract](./03-api/api-contract.md)
  - [Examples (cURL)](./03-api/api-examples.md)
  - [Integration Spec](./03-api/integration-spec.md)
  - [OpenAPI Sync](./03-api/openapi-sync.md)

- **[04 Database](./04-database/README.md)**
  - [Schema Overview](./04-database/schema.md)
  - [Migrations](./04-database/migrations.md)
  - [Audit Log](./04-database/audit-log.md)
  - [Schema Validation](./04-database/schema-validation.md)

- **[05 Frontend](./05-frontend/README.md)**
  - [Admin Web specs](./05-frontend/admin-web/ui-specs.md)
  - [Mobile specs](./05-frontend/mobile/ui-specs.md)
  - [UI Libraries](./05-frontend/ui-libraries.md)

- **[06 Backend](./06-backend/README.md)**
  - [Mutation Handlers](./06-backend/mutation-handlers.md)
  - [Security](./06-backend/security-requirements.md)
  - [Steel Thread Implementation](./06-backend/steel-thread-implementation.md)

- **[07 Quality](./07-quality/README.md)**
  - [Testing Guides & Checklists](./07-quality/qa/README.md)
    - [Test Plan](./07-quality/qa/test-plan.md)
    - [E2E Testing](./07-quality/qa/e2e-testing.md)
    - [Performance Testing](./07-quality/qa/performance-testing.md)
  - [Steel Thread Report](./07-quality/reports/steel-thread.md)
  - [Data Quality & Remediation](./07-quality/remediation-implementation-plan-2026-02.md) *(Feb 2026)*
  - [Sourcing Strategy Progress](./07-quality/reports/sourcing-strategy-2026.md)
  - [Structural Audit Results](./07-quality/results/krithi-structural-audit-2026-02.md)

- **[08 Operations](./08-operations/README.md)**
  - [Deployment](./08-operations/deployment.md)
  - [Monitoring](./08-operations/monitoring.md)
  - [Configuration](./08-operations/config.md)
  - [CLI Docs Command](./08-operations/cli-docs-command.md)
  - [Agent Workflows](./08-operations/agent-workflows.md)
  - [Query Optimization Plan](./08-operations/query-optimization-plan.md)
  - [Runbooks](./08-operations/runbooks/README.md)
    - [Steel Thread Runbook](./08-operations/runbooks/steel-thread-runbook.md)
    - [Database Runbook](./08-operations/runbooks/database-runbook.md)
    - [Incident Response](./08-operations/runbooks/incident-response.md)

- **[09 AI](./09-ai/README.md)**
  - [AI Integration](./09-ai/integration-summary.md)
  - [Knowledge Base](./09-ai/gemini-knowledge-base.md)

- **[Meta](./00-meta/README.md)** (`00-meta/`)
  - [Current Versions](./00-meta/current-versions.md)
  - [Standards](./00-meta/standards.md)
  - [Retention Plan](./00-meta/retention-plan.md)
  - [Track Closure Report](./00-meta/track-closure-report-2026-02-02.md)

- **[10 Implementations](./10-implementations/README.md)**
  - [Platform Standardization](./10-implementations/01-platform/environment-variable-standardization.md)
  - [Dependency Updates (Feb 2026)](./10-implementations/01-platform/dependency-updates-feb-2026.md)