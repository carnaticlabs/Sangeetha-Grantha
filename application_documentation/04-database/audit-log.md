# Audit Log Specification (Sangita Grantha)

> **Status**: Draft | **Version**: 0.2 | **Last Updated**: 2025-01-27
> **Owners**: Sangita Grantha Backend & Curation Teams

**Related Documents**
- [Admin Web Prd](../01-requirements/admin-web/prd.md)
- [Mutation Handlers](../06-backend/mutation-handlers.md)
- [Sangita_Schema_Overview](schema.md)

# 1. Purpose

`audit_log` provides a tamper-evident record of editorial and
administrative actions taken on the Sangita Grantha catalog. It is used
for provenance tracking, internal review, and operational debugging.

All **mutations** to Krithis, lyric variants, notation variants, tags, reference data, and
import mappings must record an audit entry.

---

# 2. Table Definition (Excerpt)

The canonical definition is in `01__baseline-schema-and-types.sql`.

```sql
CREATE TABLE IF NOT EXISTS audit_log (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id  UUID,
  actor_ip       INET,
  action         TEXT NOT NULL,
  entity_table   TEXT NOT NULL,
  entity_id      UUID,
  changed_at     TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  diff           JSONB,
  metadata       JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_audit_entity_time
  ON audit_log(entity_table, entity_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_actor
  ON audit_log(actor_user_id, changed_at DESC);
```

- `actor_user_id`: admin user performing the action (nullable for system
  tasks).
- `action`: logical label such as `CREATE_KRITHI`, `UPDATE_VARIANT`,
  `PUBLISH_KRITHI`, `IMPORT_MAP`, etc.
- `entity_table`: logical or physical table name (e.g. `krithis`,
  `krithi_lyric_variants`).
- `entity_id`: primary key of the entity affected.
- `diff`: JSONB blob describing before/after state or request payload.
- `metadata`: JSONB for extra context (e.g. `{"source":"admin_web", "requestId":"..."}`).

---

# 3. Event Categories

Suggested `action` prefixes:

| Category     | Examples                                  |
|--------------|-------------------------------------------|
| `KRITHI_*`   | `KRITHI_CREATE`, `KRITHI_UPDATE`, `KRITHI_PUBLISH`, `KRITHI_ARCHIVE` |
| `LYRICS_*`   | `LYRICS_VARIANT_CREATE`, `LYRICS_VARIANT_UPDATE`, `LYRICS_SECTIONS_UPDATE` |
| `NOTATION_*` | `NOTATION_VARIANT_CREATE`, `NOTATION_VARIANT_UPDATE`, `NOTATION_VARIANT_DELETE`, `NOTATION_ROWS_UPDATE` |
| `TAG_*`      | `TAG_CREATE`, `TAG_UPDATE`, `KRITHI_TAG_ASSIGN`, `KRITHI_TAG_REMOVE` |
| `IMPORT_*`   | `IMPORT_MAP`, `IMPORT_REJECT`             |
| `REFDATA_*`  | `COMPOSER_CREATE`, `RAGA_UPDATE`, etc.    |
| `AUTH_*`     | `ADMIN_LOGIN_SUCCESS`, `ADMIN_LOGIN_FAILURE` |

The exact taxonomy can evolve; the important invariant is that
categories remain **machine-filterable**.

---

# 4. Recording Guidelines

- Every `/v1/admin/**` mutation must:
  - Identify the actor (`actor_user_id`).
  - Choose a clear `action` string.
  - Set `entity_table` and `entity_id` where applicable.
  - Populate `diff` with:

    ```json
    {
      "before": { /* optional */ },
      "after": { /* optional */ },
      "request": { /* original request body (optional) */ }
    }
    ```

### Notation Mutations

Notation mutations (for Varnams/Swarajathis) should use:
- `entity_table`: `krithi_notation_variants` or `krithi_notation_rows`
- `action`: `NOTATION_VARIANT_CREATE`, `NOTATION_VARIANT_UPDATE`, `NOTATION_VARIANT_DELETE`, `NOTATION_ROWS_UPDATE`
- `diff`: Include notation variant metadata and row changes

Example for notation variant creation:
```json
{
  "action": "NOTATION_VARIANT_CREATE",
  "entity_table": "krithi_notation_variants",
  "entity_id": "<variant_uuid>",
  "diff": {
    "krithi_id": "<krithi_uuid>",
    "notation_type": "SWARA",
    "tala_id": "<tala_uuid>",
    "kalai": 1,
    "variant_label": "Lalgudi bani",
    "row_count": 42
  }
}
```

- System scripts or background jobs should use `actor_user_id = null`
  and an appropriate `action` (e.g. `REFDATA_SEED`).

- Frontend should **not** write directly to `audit_log`; only backend
  services should.

---

# 5. Access Patterns

- Short-term:
  - Exposed via internal tools or SQL for debugging.
- Future (optional):
  - `/v1/admin/audit/logs` endpoint with filters on `entity_table`,
    `entity_id`, `actor_user_id`, `action`, and time ranges.

---

# 6. Retention

- Retain audit logs for the life of the project (subject to infra
  constraints).
- Consider partitioning by time if the table becomes very large.
