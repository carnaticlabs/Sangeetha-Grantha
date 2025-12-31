# Audit Log Diff JSONB Schema

## Overview

The `audit_log.diff` field stores field-level changes as a JSONB object. This document defines the standard schema for this structure.

## Schema Definition

```typescript
interface AuditLogDiff {
  [fieldName: string]: {
    before?: any;  // Value before the change (null/undefined if field was added)
    after?: any;   // Value after the change (null/undefined if field was removed)
  };
}
```

## Field Naming Convention

- Use **snake_case** for database column names (e.g., `composer_id`, `workflow_state`)
- Use **camelCase** for nested object properties (e.g., `composer.name`, `tags[0].id`)

## Value Types

- **Primitives**: Strings, numbers, booleans stored as-is
- **Null values**: Represented as `null` (not omitted)
- **Arrays**: Stored as JSON arrays
- **Objects**: Stored as JSON objects
- **UUIDs**: Stored as strings
- **Timestamps**: Stored as ISO 8601 strings

## Examples

### Simple Field Update

```json
{
  "title": {
    "before": "Nagumomu Ganaleni",
    "after": "Nagumomu Ganaleni (Updated)"
  }
}
```

### Field Addition

```json
{
  "incipit": {
    "after": "Nagumomu ganaleni"
  }
}
```

### Field Removal

```json
{
  "notes": {
    "before": "Some notes here",
    "after": null
  }
}
```

### Foreign Key Update

```json
{
  "composer_id": {
    "before": "550e8400-e29b-41d4-a716-446655440000",
    "after": "660e8400-e29b-41d4-a716-446655440001"
  }
}
```

### Array Field Update

```json
{
  "raga_ids": {
    "before": ["550e8400-e29b-41d4-a716-446655440000"],
    "after": ["550e8400-e29b-41d4-a716-446655440000", "660e8400-e29b-41d4-a716-446655440001"]
  }
}
```

### Multiple Field Changes

```json
{
  "workflow_state": {
    "before": "draft",
    "after": "in_review"
  },
  "title": {
    "before": "Old Title",
    "after": "New Title"
  },
  "is_ragamalika": {
    "before": false,
    "after": true
  }
}
```

### Nested Object Changes

```json
{
  "composer.name": {
    "before": "Tyagaraja",
    "after": "Tyāgarāja"
  }
}
```

## Backend Implementation Guidelines

1. **Always include both `before` and `after`** when a field is updated
2. **Only include `after`** when a field is added
3. **Only include `before`** when a field is removed (set `after` to `null`)
4. **Omit unchanged fields** from the diff
5. **Use consistent field names** matching database columns
6. **Handle null/undefined** explicitly (don't omit keys)

## Frontend Parsing

The frontend should:
- Display field names in a user-friendly format (e.g., `composer_id` → "Composer")
- Format values appropriately (e.g., UUIDs → entity names, timestamps → readable dates)
- Show visual diff (red for removed, green for added)
- Handle nested objects and arrays gracefully

## Migration Notes

- Existing audit logs may not follow this schema
- Backend should validate diff structure before writing
- Frontend should handle missing or malformed diffs gracefully


