| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Exposed ORM: DAO vs DSL Approach - Comprehensive Comparison


---


## Executive Summary

This document provides a detailed comparison between Exposed ORM's **DAO (Data Access Object)** and **DSL (Domain-Specific Language)** approaches to persistence. Our codebase currently uses the **DSL approach**, which provides type-safe SQL generation but requires careful implementation to avoid inefficient operations like DELETE+INSERT when UPDATE would suffice.

## Table of Contents

1. [Overview](#overview)
2. [DAO Approach](#dao-approach)
3. [DSL Approach](#dsl-approach)
4. [Detailed Comparison](#detailed-comparison)
5. [Current Implementation Analysis](#current-implementation-analysis)
6. [Recommendations](#recommendations)
7. [Migration Considerations](#migration-considerations)

---

## Overview

JetBrains Exposed provides two primary approaches for database operations:

### DAO (Data Access Object) Approach
- **Object-oriented** - Entities are Kotlin classes that extend `Entity<T>`
- **Active Record pattern** - Entities know how to save/delete themselves
- **Automatic change tracking** - Exposed tracks modifications and generates optimal SQL
- **Relationship management** - Built-in support for references and relationships

### DSL (Domain-Specific Language) Approach
- **Type-safe SQL builder** - Compile-time checked SQL-like syntax
- **Functional style** - Operations on `Table` objects, not entity instances
- **Explicit control** - You write the exact SQL operations
- **No automatic change tracking** - You must manually implement UPDATE logic

---

## DAO Approach

### Core Concepts

```kotlin
// Table definition
object UsersTable : UUIDTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val createdAt = timestampWithTimeZone("created_at")
}

// Entity class
class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
    var createdAt by UsersTable.createdAt
}
```

### Key Characteristics

#### 1. **Entity-Based Operations**
```kotlin
// Create
val user = User.new {
    name = "John Doe"
    email = "john@example.com"
    createdAt = OffsetDateTime.now(ZoneOffset.UTC)
}

// Update (automatic change tracking)
user.name = "Jane Doe"
user.email = "jane@example.com"
// Only modified fields are updated in SQL

// Delete
user.delete()
```

#### 2. **Automatic Change Tracking**
- Exposed tracks which fields have changed
- Generates optimized UPDATE statements with only changed columns
- No need to manually compare old vs new values

#### 3. **Relationship Management**
```kotlin
object PostsTable : UUIDTable("posts") {
    val userId = uuid("user_id").references(UsersTable.id)
    val title = varchar("title", 255)
}

class Post(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Post>(PostsTable)
    
    var userId by PostsTable.userId
    var title by PostsTable.title
    
    // Automatic relationship
    var user by User referencedOn PostsTable.userId
}

// Usage
val post = Post.new { 
    user = existingUser  // Automatic foreign key handling
    title = "My Post"
}
```

#### 4. **Querying**
```kotlin
// Find by ID
val user = User.findById(userId)

// Query with conditions
val users = User.find { UsersTable.email like "%@example.com" }

// Eager loading
val posts = Post.find { PostsTable.userId eq userId }
    .with(Post::user)  // Loads related user in single query
```

### Advantages

✅ **Automatic Optimization**
- Only changed fields are updated
- Efficient SQL generation
- No unnecessary DELETE+INSERT operations

✅ **Type Safety**
- Compile-time checking of entity properties
- IDE autocomplete for relationships

✅ **Less Boilerplate**
- No manual UPDATE logic needed
- Relationships handled automatically

✅ **Change Tracking**
- Built-in dirty checking
- Automatic `updated_at` handling (if configured)

### Disadvantages

❌ **Memory Overhead**
- Entities are objects in memory
- Can be heavy for large result sets

❌ **Learning Curve**
- Different from traditional SQL
- Requires understanding of Entity lifecycle

❌ **Less Explicit Control**
- Harder to see exact SQL being generated
- Some operations may be less intuitive

❌ **Limited Query Flexibility**
- Complex queries may require falling back to DSL
- Less direct control over JOIN strategies

---

## DSL Approach

### Core Concepts

```kotlin
// Table definition (same as DAO)
object UsersTable : UUIDTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val createdAt = timestampWithTimeZone("created_at")
}

// No entity class needed
```

### Key Characteristics

#### 1. **Table-Based Operations**
```kotlin
// Create
UsersTable.insert {
    it[id] = UUID.randomUUID()
    it[name] = "John Doe"
    it[email] = "john@example.com"
    it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
}

// Update (explicit)
UsersTable.update({ UsersTable.id eq userId }) {
    it[name] = "Jane Doe"
    it[email] = "jane@example.com"
}

// Delete
UsersTable.deleteWhere { UsersTable.id eq userId }
```

#### 2. **Manual Change Tracking**
- You must implement your own logic to determine what changed
- Can lead to inefficient DELETE+INSERT patterns if not careful

#### 3. **Explicit Relationship Handling**
```kotlin
// Manual foreign key handling
PostsTable.insert {
    it[id] = UUID.randomUUID()
    it[userId] = existingUserId
    it[title] = "My Post"
}

// Joins are explicit
(PostsTable innerJoin UsersTable)
    .selectAll()
    .where { PostsTable.userId eq UsersTable.id }
    .map { row ->
        PostDto(
            id = row[PostsTable.id].value,
            title = row[PostsTable.title],
            userName = row[UsersTable.name]
        )
    }
```

#### 4. **Querying**
```kotlin
// Find by ID
val userRow = UsersTable
    .selectAll()
    .where { UsersTable.id eq userId }
    .singleOrNull()

// Query with conditions
val users = UsersTable
    .selectAll()
    .where { UsersTable.email like "%@example.com" }
    .map { it.toUserDto() }
```

### Advantages

✅ **Explicit Control**
- You see exactly what SQL is generated
- Full control over query structure
- Easy to optimize complex queries

✅ **Lower Memory Footprint**
- No entity objects created
- Direct mapping to DTOs
- Better for large result sets

✅ **SQL-Like Syntax**
- Familiar to developers with SQL background
- Easy to translate SQL queries to Exposed DSL

✅ **Flexibility**
- Easy to write complex queries
- Can use raw SQL when needed
- Better for reporting/analytics queries

### Disadvantages

❌ **Manual Change Tracking**
- Must implement UPDATE logic yourself
- Easy to fall into DELETE+INSERT anti-pattern
- More boilerplate for updates

❌ **No Automatic Optimization**
- You must manually ensure only changed fields are updated
- Risk of inefficient operations

❌ **More Verbose**
- More code for simple operations
- Relationship handling requires manual JOINs

❌ **Type Safety Limitations**
- Compile-time checking is good but not perfect
- Runtime errors possible with complex queries

---

## Detailed Comparison

### Performance Comparison

| Operation | DAO Approach | DSL Approach |
|-----------|--------------|--------------|
| **Single Row Insert** | Similar performance | Similar performance |
| **Batch Insert** | `Entity.batchInsert()` | `Table.batchInsert()` - Both similar |
| **Update Single Field** | ✅ Only updates changed field | ⚠️ Must manually specify fields |
| **Update Multiple Fields** | ✅ Only updates changed fields | ⚠️ Must manually specify all fields |
| **Update Collection (1-to-Many)** | ✅ Automatic diff and update | ❌ Often DELETE+INSERT (inefficient) |
| **Complex Queries** | ⚠️ May require DSL fallback | ✅ Excellent |
| **Large Result Sets** | ⚠️ Entity overhead | ✅ Direct to DTO mapping |

### Code Complexity

| Scenario | DAO Approach | DSL Approach |
|----------|--------------|--------------|
| **Simple CRUD** | ✅ Less code | ⚠️ More verbose |
| **Complex Queries** | ⚠️ May need DSL | ✅ Natural fit |
| **Collection Updates** | ✅ Automatic | ❌ Manual implementation |
| **Relationship Navigation** | ✅ Automatic | ⚠️ Manual JOINs |
| **Bulk Operations** | ⚠️ Entity overhead | ✅ Efficient |

### Maintainability

| Aspect | DAO Approach | DSL Approach |
|-------|--------------|---------------|
| **Readability** | ✅ OOP style, intuitive | ✅ SQL-like, familiar |
| **Testability** | ✅ Easy to mock entities | ✅ Easy to test queries |
| **Debugging** | ⚠️ Hidden SQL generation | ✅ Explicit SQL |
| **Refactoring** | ✅ IDE support for entities | ⚠️ String-based column names |

---

## Current Implementation Analysis

### Problem: Inefficient DELETE+INSERT Pattern

Our current implementation in `KrithiRepository.saveSections()` uses a **delete-then-insert** pattern:

```kotlin
suspend fun saveSections(krithiId: Uuid, sections: List<Pair<String, Int>>) = DatabaseFactory.dbQuery {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val javaKrithiId = krithiId.toJavaUuid()
    
    // ❌ PROBLEM: Deletes ALL sections, even if only one changed
    KrithiSectionsTable.deleteWhere { KrithiSectionsTable.krithiId eq javaKrithiId }
    
    // Then re-inserts everything
    if (sections.isNotEmpty()) {
        KrithiSectionsTable.batchInsert(sections.withIndex()) { (index, section) ->
            val (sectionType, orderIndex) = section
            this[KrithiSectionsTable.id] = UUID.randomUUID()
            this[KrithiSectionsTable.krithiId] = javaKrithiId
            this[KrithiSectionsTable.sectionType] = sectionType
            this[KrithiSectionsTable.orderIndex] = orderIndex
            this[KrithiSectionsTable.label] = null
            this[KrithiSectionsTable.notes] = null
            this[KrithiSectionsTable.createdAt] = now  // ❌ Loses original created_at
            this[KrithiSectionsTable.updatedAt] = now
        }
    }
}
```

### Issues with Current Approach

1. **Inefficient SQL**
   - DELETE all sections, then INSERT all sections
   - Even if only one section changed, all are deleted and recreated
   - Loses original `created_at` timestamps

2. **Cascade Effects**
   - If `krithi_lyric_sections` references `krithi_sections.id`, foreign keys may break
   - Requires careful handling of dependent data

3. **Audit Trail Loss**
   - Original creation timestamps are lost
   - Makes it harder to track when sections were actually created

4. **Performance Impact**
   - More database operations than necessary
   - Higher transaction overhead
   - Potential for lock contention

### Why This Pattern Exists

The DELETE+INSERT pattern is common in DSL implementations because:
- It's simpler to implement than diff logic
- Works correctly for "replace entire collection" scenarios
- Avoids complex UPDATE logic for collections

However, it's **not optimal** for scenarios where:
- Only a few items in the collection change
- You want to preserve metadata (created_at, etc.)
- Foreign key relationships exist

---

## Recommendations

### Option 1: Improve DSL Implementation (Recommended for Current Codebase)

Implement proper diff logic to use UPDATE where possible:

```kotlin
suspend fun saveSections(krithiId: Uuid, sections: List<Pair<String, Int>>) = DatabaseFactory.dbQuery {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val javaKrithiId = krithiId.toJavaUuid()
    
    // Get existing sections
    val existingSections = KrithiSectionsTable
        .selectAll()
        .where { KrithiSectionsTable.krithiId eq javaKrithiId }
        .associateBy { it[KrithiSectionsTable.orderIndex] }
    
    // Build map of new sections by order_index
    val newSectionsMap = sections.associateBy { it.second }
    
    // Determine what to update, insert, and delete
    val toUpdate = mutableListOf<Pair<UUID, Pair<String, Int>>>()
    val toInsert = mutableListOf<Pair<String, Int>>()
    val toDelete = mutableListOf<UUID>()
    
    // Find sections to update (same order_index, different type)
    existingSections.forEach { (orderIndex, row) ->
        val existingId = row[KrithiSectionsTable.id].value
        val existingType = row[KrithiSectionsTable.sectionType]
        val newSection = newSectionsMap[orderIndex]
        
        if (newSection != null) {
            if (newSection.first != existingType) {
                // Section type changed - update it
                toUpdate.add(existingId to newSection)
            }
            // If same, no change needed
        } else {
            // Section removed
            toDelete.add(existingId)
        }
    }
    
    // Find sections to insert (new order_index)
    newSectionsMap.forEach { (orderIndex, section) ->
        if (!existingSections.containsKey(orderIndex)) {
            toInsert.add(section)
        }
    }
    
    // Execute updates
    toUpdate.forEach { (id, section) ->
        KrithiSectionsTable.update({ KrithiSectionsTable.id eq id }) {
            it[KrithiSectionsTable.sectionType] = section.first
            it[KrithiSectionsTable.updatedAt] = now
            // Preserve created_at, label, notes
        }
    }
    
    // Execute inserts
    if (toInsert.isNotEmpty()) {
        KrithiSectionsTable.batchInsert(toInsert.withIndex()) { (index, section) ->
            val (sectionType, orderIndex) = section
            this[KrithiSectionsTable.id] = UUID.randomUUID()
            this[KrithiSectionsTable.krithiId] = javaKrithiId
            this[KrithiSectionsTable.sectionType] = sectionType
            this[KrithiSectionsTable.orderIndex] = orderIndex
            this[KrithiSectionsTable.label] = null
            this[KrithiSectionsTable.notes] = null
            this[KrithiSectionsTable.createdAt] = now
            this[KrithiSectionsTable.updatedAt] = now
        }
    }
    
    // Execute deletes
    if (toDelete.isNotEmpty()) {
        KrithiSectionsTable.deleteWhere { 
            KrithiSectionsTable.id inList toDelete 
        }
    }
}
```

**Benefits:**
- ✅ Only updates changed sections
- ✅ Preserves `created_at` timestamps
- ✅ More efficient SQL generation
- ✅ Maintains foreign key relationships

**Drawbacks:**
- ⚠️ More complex code
- ⚠️ Requires reading existing data first
- ⚠️ More database round trips

### Option 2: Hybrid Approach

Use DSL for queries, but create lightweight entity-like wrappers for updates:

```kotlin
data class KrithiSectionEntity(
    val id: UUID,
    val krithiId: UUID,
    var sectionType: String,
    var orderIndex: Int,
    var label: String?,
    var notes: String?,
    val createdAt: OffsetDateTime,
    var updatedAt: OffsetDateTime
) {
    fun save() = DatabaseFactory.dbQuery {
        KrithiSectionsTable.update({ KrithiSectionsTable.id eq id }) {
            it[KrithiSectionsTable.sectionType] = sectionType
            it[KrithiSectionsTable.orderIndex] = orderIndex
            it[KrithiSectionsTable.label] = label
            it[KrithiSectionsTable.notes] = notes
            it[KrithiSectionsTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }
}
```

### Option 3: Migrate to DAO (Long-term Consideration)

For entities with frequent collection updates, consider migrating to DAO:

```kotlin
class KrithiSection(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<KrithiSection>(KrithiSectionsTable)
    
    var krithiId by KrithiSectionsTable.krithiId
    var sectionType by KrithiSectionsTable.sectionType
    var orderIndex by KrithiSectionsTable.orderIndex
    var label by KrithiSectionsTable.label
    var notes by KrithiSectionsTable.notes
    var createdAt by KrithiSectionsTable.createdAt
    var updatedAt by KrithiSectionsTable.updatedAt
}

// Usage - automatic change tracking
val sections = KrithiSection.find { KrithiSectionsTable.krithiId eq krithiId }
sections.forEach { it.sectionType = newType }
// Only changed sections are updated automatically
```

---

## Migration Considerations

### When to Use DAO

✅ **Good for:**
- Entities with frequent updates
- Complex relationships (1-to-many, many-to-many)
- When change tracking is important
- When you want automatic optimization

❌ **Avoid when:**
- Read-heavy workloads with large result sets
- Complex reporting queries
- Need explicit control over SQL

### When to Use DSL

✅ **Good for:**
- Complex queries and reporting
- Bulk operations
- When you need explicit SQL control
- Read-heavy operations

❌ **Avoid when:**
- Frequently updating collections
- When you need automatic change tracking
- Simple CRUD with relationships

### Hybrid Strategy

Our codebase can benefit from a **hybrid approach**:

1. **Use DSL for:**
   - Complex queries (search, filtering)
   - Bulk operations
   - Read operations

2. **Use DAO for:**
   - Entities with frequent collection updates (like `krithi_sections`)
   - Complex relationships
   - When change tracking matters

3. **Improve DSL for:**
   - Collection updates (implement diff logic)
   - Preserve metadata (created_at, etc.)

---

## Conclusion

### Current State
- ✅ DSL approach provides good control and flexibility
- ❌ DELETE+INSERT pattern is inefficient for collection updates
- ⚠️ Missing change tracking for collections

### Recommended Actions

1. **Short-term:** Improve `saveSections()` with diff logic (Option 1)
2. **Medium-term:** Evaluate hybrid approach for frequently-updated entities
3. **Long-term:** Consider DAO migration for entities with complex relationships

### Key Takeaway

The DSL approach is powerful and flexible, but requires **careful implementation** to avoid inefficient patterns. The DELETE+INSERT anti-pattern is common but can be avoided with proper diff logic. For our use case, improving the DSL implementation is the most pragmatic solution that maintains our current architecture while fixing the performance issue.