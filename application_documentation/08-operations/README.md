| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# 08 Operations


---


## Contents

- [agent-workflows.md](./agent-workflows.md) - AI agent workflow documentation
- [cli-docs-command.md](./cli-docs-command.md) - Sangita CLI documentation commands
- [config.md](./config.md) - Configuration management
- [deployment.md](./deployment.md) - Deployment procedures for all environments
- [monitoring.md](./monitoring.md) - Monitoring, metrics, and alerting
- [query-optimization-plan.md](./query-optimization-plan.md) - Database query optimization plan
- [runbooks/](./runbooks/) - Operational runbooks
  - [database-runbook.md](./runbooks/database-runbook.md) - Database operations and disaster recovery
  - [incident-response.md](./runbooks/incident-response.md) - Incident response procedures
  - [steel-thread-runbook.md](./runbooks/steel-thread-runbook.md) - Steel thread test execution

## PDF Extraction Service Operations

The Python PDF extraction service runs as a Docker container alongside PostgreSQL. Key operational commands:

```bash
# Start extraction service (with database)
sangita-cli extraction start --with-db

# Check extraction queue status
sangita-cli extraction status

# View extraction service logs
sangita-cli extraction logs

# Stop extraction service
sangita-cli extraction stop

# Docker Compose (direct)
docker compose --profile extraction up -d
```

For architecture details, see [Backend System Design §5.8](../02-architecture/backend-system-design.md) and [Krithi Data Sourcing Strategy §8](../01-requirements/krithi-data-sourcing/quality-strategy.md#8-technology-decisions--containerised-deployment).