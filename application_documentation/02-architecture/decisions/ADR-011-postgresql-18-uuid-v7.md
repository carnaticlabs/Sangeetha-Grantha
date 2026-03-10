| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# ADR-011: PostgreSQL 18 Upgrade with UUID v7 Adoption

## Context

The platform was running PostgreSQL 15 since inception. Two drivers motivated an upgrade:

1. **PostgreSQL 18 GA release** (September 2025): Brought native `uuidv7()` function, improved query planning, and better JSON performance.
2. **UUID v4 limitations**: Random UUIDs (`gen_random_uuid()`) cause B-tree index fragmentation and provide no temporal ordering. For a growing dataset of compositions, temporally-ordered IDs improve index locality and enable implicit chronological sorting.

TRACK-072 tracked the upgrade work (February 2026).

## Decision

Upgrade from **PostgreSQL 15 to PostgreSQL 18.3** and adopt **UUID v7** as the default primary key generation strategy for all tables.

- Docker image: `postgres:18.3-alpine`
- All 27 tables migrated from `gen_random_uuid()` to `uuidv7()` default values
- Migration 37 (`37__uuid-v7-adoption.sql`) performed the conversion

## Rationale

1. **Index performance**: UUID v7 is time-ordered, producing monotonically increasing values that maintain B-tree locality. This eliminates the page-split overhead of random UUID v4.
2. **Native support**: PostgreSQL 18 provides `uuidv7()` as a built-in function — no extensions required.
3. **Implicit ordering**: New records are naturally ordered by creation time via their ID, useful for audit trails and feed-style queries.
4. **Zero application changes**: UUID v7 is format-compatible with UUID v4. All existing UUIDs remain valid. Backend code (Exposed ORM) required no changes.
5. **Existing data preserved**: Migration only changed DEFAULT values on columns; existing rows kept their original UUID v4 values.

## Implementation Details

### Migration 37 Structure

```sql
-- For each of 27 tables:
ALTER TABLE composers ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE ragas ALTER COLUMN id SET DEFAULT uuidv7();
-- ... (all domain and infrastructure tables)
```

### Docker Compose Update

```yaml
services:
  db:
    image: postgres:18.3-alpine
```

### Exposed ORM Compatibility

No changes required — Exposed's `uuid()` column type works identically with v4 and v7 UUIDs:

```kotlin
object ComposersTable : Table("composers") {
    val id = uuid("id").autoGenerate()  // Works with uuidv7() default
}
```

## Consequences

### Positive

- **Better write performance**: Reduced B-tree page splits for insert-heavy workloads
- **Temporal ordering**: IDs encode creation timestamp (millisecond precision)
- **Future-proof**: PostgreSQL 18 is the current LTS track
- **Zero downtime**: Migration was additive (ALTER DEFAULT), no data rewrite

### Negative

- **Mixed ID versions**: Existing rows have UUID v4, new rows get UUID v7. This is cosmetic only — both are valid UUIDs.

### Neutral

- **ID size unchanged**: Both v4 and v7 are 128-bit UUIDs, no storage impact

## References

- TRACK-072: PostgreSQL 18 Upgrade
- [Database Schema](../../04-database/schema.md)
- [Current Versions](../../00-meta/current-versions.md)
