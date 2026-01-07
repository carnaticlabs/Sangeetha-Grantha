# Query Optimization Evaluation: Krithi UPDATE Operations

## Current Query Pattern Analysis

### Observed Query Sequence (from logs)

When updating only `temple_id` for a Krithi:

1. **UPDATE query** - Updates ALL fields (even unchanged ones):
   ```sql
   UPDATE krithis SET 
     title='...', title_normalized='...', incipit='...', 
     incipit_normalized='...', composer_id='...', 
     musical_form='KRITHI'::musical_form_enum, 
     primary_language='sa'::language_code_enum, 
     tala_id='...', temple_id='...',  -- Only this changed!
     is_ragamalika=FALSE, workflow_state='draft'::workflow_state_enum,
     sahitya_summary='...', notes='...', updated_at='...'
   WHERE krithis.id = '...'
   ```

2. **SELECT krithi_ragas** - Always executed if `ragaIds` parameter is provided:
   ```sql
   SELECT krithi_ragas.krithi_id, krithi_ragas.raga_id, 
          krithi_ragas.order_index, krithi_ragas."section", 
          krithi_ragas.notes 
   FROM krithi_ragas 
   WHERE krithi_ragas.krithi_id = '...'
   ```

3. **SELECT krithis** - To return updated DTO:
   ```sql
   SELECT krithis.id, krithis.title, krithis.incipit, ...
   FROM krithis 
   WHERE krithis.id = '...'
   ```

4. **SELECT krithi_tags** - If tags are being updated:
   ```sql
   SELECT krithi_tags.krithi_id, krithi_tags.tag_id, ...
   FROM krithi_tags 
   WHERE krithi_tags.krithi_id = '...'
   ```

5. **INSERT audit_log** - Audit trail entry

## Issues Identified

### 1. **Inefficient UPDATE: All Fields Included**
**Problem**: The UPDATE statement includes ALL non-null fields from the request, even if they haven't changed.

**Impact**:
- Unnecessary data transfer
- Potential trigger overhead (if any)
- Wasted write I/O
- Larger WAL entries

**Root Cause**: Exposed's `update` method includes all fields set in the lambda, regardless of actual changes.

### 2. **Redundant SELECT After UPDATE**
**Problem**: After updating, a separate SELECT is executed to fetch the updated row.

**Impact**:
- Extra round-trip to database
- Additional query parsing/execution overhead

**Better Approach**: Use PostgreSQL's `RETURNING` clause to get updated row in same query.

### 3. **Unnecessary Related Data Queries**
**Problem**: 
- `krithi_ragas` is queried even when `ragaIds` parameter is `null` (not being updated)
- Multiple separate queries instead of batched/joined queries

**Impact**:
- Extra queries when not needed
- N+1 query pattern potential

### 4. **No Change Detection**
**Problem**: No comparison with existing values before updating.

**Impact**:
- Updates fields that haven't changed
- Wasted database resources
- Unnecessary audit log entries (though audit log doesn't capture diffs currently)

## Optimization Recommendations

### Priority 1: Use PostgreSQL RETURNING Clause

**Current**:
```kotlin
val updated = KrithisTable.update(...) { ... }
// ... raga updates ...
KrithisTable.selectAll().where { ... }.map { it.toKrithiDto() }.singleOrNull()
```

**Optimized**:
```kotlin
val updatedRow = KrithisTable.update({ ... }) { ... }
    .returning()
    .singleOrNull()
    ?.let { it.toKrithiDto() }
```

**Benefits**:
- Eliminates one SELECT query
- Atomic: get updated row in same transaction
- Reduces round-trips

**Note**: Check if Exposed v1 supports `RETURNING`. If not, may need raw SQL or upgrade.

### Priority 2: Selective Field Updates (Change Detection)

**Current**:
```kotlin
KrithisTable.update({ KrithisTable.id eq id.toJavaUuid() }) {
    title?.let { value -> it[KrithisTable.title] = value }
    templeId?.let { value -> it[KrithisTable.templeId] = value }
    // ... all fields included if non-null
}
```

**Optimized**:
```kotlin
// Fetch existing row first (or cache from previous query)
val existing = KrithisTable.selectAll()
    .where { KrithisTable.id eq id.toJavaUuid() }
    .singleOrNull() ?: return@dbQuery null

// Only update changed fields
KrithisTable.update({ KrithisTable.id eq id.toJavaUuid() }) {
    if (title != null && title != existing[KrithisTable.title]) {
        it[KrithisTable.title] = title
    }
    if (templeId != null && templeId != existing[KrithisTable.templeId]) {
        it[KrithisTable.templeId] = templeId
    }
    // ... only include changed fields
    it[KrithisTable.updatedAt] = now
}
```

**Trade-off**: Adds one SELECT upfront, but:
- Reduces UPDATE payload size
- Only updates what changed
- Can reuse the SELECT result for RETURNING if supported

**Alternative**: If frontend sends only changed fields, this becomes easier.

### Priority 3: Conditional Related Data Queries

**Current**:
```kotlin
ragaIds?.let { ragas ->
    // Always queries existing ragas
    val existingRagas = KrithiRagasTable.selectAll()...
}
```

**Optimized**:
```kotlin
// Only query if ragas are actually being updated
if (ragaIds != null) {
    val existingRagas = KrithiRagasTable.selectAll()...
    // ... update logic
}
```

**Note**: Current code already does this with `?.let`, but ensure it's not being called unnecessarily.

### Priority 4: Batch DELETE Operations

**Current**:
```kotlin
toDelete.forEach { (ragaId, orderIndex) ->
    KrithiRagasTable.deleteWhere {
        (KrithiRagasTable.krithiId eq javaKrithiId) and
        (KrithiRagasTable.ragaId eq ragaId) and
        (KrithiRagasTable.orderIndex eq orderIndex)
    }
}
```

**Optimized**:
```kotlin
if (toDelete.isNotEmpty()) {
    // Single DELETE with IN clause for composite key
    // Note: PostgreSQL supports composite IN, but Exposed may need workaround
    KrithiRagasTable.deleteWhere {
        (KrithiRagasTable.krithiId eq javaKrithiId) and
        // Use OR conditions for composite key deletes
        // Or use raw SQL if Exposed doesn't support efficiently
    }
}
```

**Challenge**: Exposed may not support efficient composite key batch deletes. May need raw SQL:
```sql
DELETE FROM krithi_ragas 
WHERE krithi_id = ? 
  AND (raga_id, order_index) IN ((?, ?), (?, ?), ...)
```

### Priority 5: Combine Queries with JOINs (If Needed)

If the frontend needs ragas/tags in the response, consider:

**Current**: Separate queries
**Optimized**: Single query with LEFT JOINs (if Exposed supports efficiently)

**Trade-off**: More complex query, but fewer round-trips. Only beneficial if data is always needed.

## Implementation Strategy

### Phase 1: Quick Wins (Low Risk)
1. ✅ Use `RETURNING` clause if Exposed supports it
2. ✅ Add conditional checks to skip unnecessary related data queries
3. ✅ Batch DELETE operations where possible

### Phase 2: Change Detection (Medium Risk)
1. Fetch existing row before update
2. Compare values and only update changed fields
3. Reuse existing row data for RETURNING if possible

### Phase 3: Advanced Optimizations (Higher Risk)
1. Raw SQL for complex batch operations
2. Caching strategies for frequently accessed data
3. Consider GraphQL-style field selection if frontend supports it

## Expected Impact

### Query Count Reduction
- **Current**: 4-5 queries per update (UPDATE + 1-3 SELECTs + INSERT audit)
- **Optimized**: 2-3 queries per update (UPDATE with RETURNING + conditional SELECTs + INSERT audit)
- **Savings**: ~40-50% query reduction

### Query Size Reduction
- **Current**: UPDATE includes all fields (~15-20 columns)
- **Optimized**: UPDATE includes only changed fields (1-3 columns typically)
- **Savings**: ~70-80% reduction in UPDATE payload

### Performance Improvement
- Reduced database load
- Lower network overhead
- Faster response times (especially with RETURNING)
- Better scalability

## Code Example: Optimized Update Method

```kotlin
suspend fun update(
    id: Uuid,
    // ... parameters ...
): KrithiDto? = DatabaseFactory.dbQuery {
    val javaId = id.toJavaUuid()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    
    // Fetch existing row for change detection
    val existing = KrithisTable
        .selectAll()
        .where { KrithisTable.id eq javaId }
        .singleOrNull() ?: return@dbQuery null
    
    // Build update with only changed fields
    var hasChanges = false
    val updated = KrithisTable.update({ KrithisTable.id eq javaId }) {
        title?.let { value ->
            if (value != existing[KrithisTable.title]) {
                it[KrithisTable.title] = value
                hasChanges = true
            }
        }
        titleNormalized?.let { value ->
            if (value != existing[KrithisTable.titleNormalized]) {
                it[KrithisTable.titleNormalized] = value
                hasChanges = true
            }
        }
        templeId?.let { value ->
            if (value != existing[KrithisTable.templeId]) {
                it[KrithisTable.templeId] = value
                hasChanges = true
            }
        }
        // ... other fields with change detection ...
        
        // Always update updatedAt if any field changed
        if (hasChanges) {
            it[KrithisTable.updatedAt] = now
        }
    }
    
    if (updated == 0 || !hasChanges) {
        // No changes or not found - return existing
        return@dbQuery existing.toKrithiDto()
    }
    
    // Handle ragas only if provided
    ragaIds?.let { ragas ->
        // ... existing raga update logic ...
    }
    
    // Use RETURNING if available, otherwise SELECT
    // For now, reuse existing row data (updated in place)
    // Or fetch fresh if needed for consistency
    existing.toKrithiDto().copy(
        // Update fields that changed
        templeId = templeId?.let { it.toKotlinUuid() } ?: existing[KrithisTable.templeId]?.toKotlinUuid(),
        updatedAt = now.toKotlinInstant()
    )
}
```

## Testing Considerations

1. **Verify change detection** - Ensure unchanged fields aren't updated
2. **Test with NULL values** - Ensure NULL handling works correctly
3. **Concurrent updates** - Ensure no race conditions
4. **Audit logging** - Verify audit logs still capture updates correctly
5. **Performance benchmarks** - Measure before/after query counts and execution times

## Notes

- Exposed v1 may have limitations with `RETURNING` clause
- Change detection adds one SELECT upfront, but reduces UPDATE size
- Consider frontend changes to send only changed fields (PATCH semantics)
- Monitor database query logs to validate optimizations

