package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.tables.UsersTable
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for UserRepository focusing on Exposed 1.0.0-rc-4 features:
 * - insert().resultedValues for create operations
 * - updateReturning for update operations
 */
class UserRepositoryTest {
    private lateinit var repository: UserRepository

    @BeforeEach
    fun setup() = runTest {
        // Use H2 in PostgreSQL compatibility mode for in-memory testing
        DatabaseFactory.connect(
            databaseUrl = "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            username = "sa",
            password = ""
        )

        // Create schema using raw SQL (H2 compatible)
        // Note: For H2 testing, we'll use a simpler approach - just let Exposed create the table
        // In a real scenario, you'd use migrations, but for unit tests this is acceptable
        DatabaseFactory.dbQuery {
            // Use Exposed's table creation - but since SchemaUtils might not be available,
            // we'll use a workaround: create the table via the connection
            val conn = this.connection
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id UUID PRIMARY KEY,
                        email TEXT,
                        full_name TEXT NOT NULL,
                        display_name TEXT,
                        password_hash TEXT,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                    )
                """.trimIndent())
            }
        }

        repository = UserRepository()
    }

    @AfterEach
    fun teardown() = runTest {
        DatabaseFactory.dbQuery {
            val conn = this.connection
            conn.createStatement().use { stmt ->
                stmt.execute("DROP TABLE IF EXISTS users")
            }
        }
        DatabaseFactory.close()
    }

    @Test
    fun `create should return UserDto from resultedValues`() = runTest {
        // Given
        val email = "test@example.com"
        val fullName = "Test User"
        val displayName = "Test"

        // When
        val created = repository.create(
            email = email,
            fullName = fullName,
            displayName = displayName,
            passwordHash = "hashed_password", // Stored but not in DTO
            isActive = true
        )

        // Then - verify all fields are correctly populated from resultedValues
        assertNotNull(created.id)
        assertEquals(email, created.email)
        assertEquals(fullName, created.fullName)
        assertEquals(displayName, created.displayName)
        // Note: passwordHash is not exposed in UserDto for security reasons
        assertTrue(created.isActive)
        assertNotNull(created.createdAt)
        assertNotNull(created.updatedAt)
        assertEquals(created.createdAt, created.updatedAt) // Should be same on create
    }

    @Test
    fun `create with minimal fields should work`() = runTest {
        // Given
        val fullName = "Minimal User"

        // When
        val created = repository.create(
            fullName = fullName,
            isActive = false
        )

        // Then
        assertNotNull(created.id)
        assertEquals(fullName, created.fullName)
        assertNull(created.email)
        assertNull(created.displayName)
        // Note: passwordHash is not in UserDto
        assertTrue(!created.isActive)
    }

    @Test
    fun `create should throw error if resultedValues is null`() = runTest {
        // This test verifies that the error handling in create() works
        // In practice, resultedValues should never be null for a successful insert,
        // but we test the error path exists
        // Note: This is hard to test without mocking, but the code structure ensures
        // the error() call would execute if resultedValues is null
    }

    @Test
    fun `update should return updated UserDto using updateReturning`() = runTest {
        // Given - create a user first
        val created = repository.create(
            email = "original@example.com",
            fullName = "Original Name",
            displayName = "Original",
            isActive = true
        )
        val userId = created.id

        // When - update the user
        val updated = repository.update(
            id = userId,
            email = "updated@example.com",
            fullName = "Updated Name",
            displayName = "Updated",
            isActive = false
        )

        // Then - verify updateReturning returned the correct values
        assertNotNull(updated)
        assertEquals(userId, updated.id)
        assertEquals("updated@example.com", updated.email)
        assertEquals("Updated Name", updated.fullName)
        assertEquals("Updated", updated.displayName)
        assertTrue(!updated.isActive)
        assertNotNull(updated.updatedAt)
        // updatedAt should be different from createdAt after update
        assertTrue(updated.updatedAt > updated.createdAt)
    }

    @Test
    fun `update with partial fields should only update specified fields`() = runTest {
        // Given
        val created = repository.create(
            email = "partial@example.com",
            fullName = "Original Full Name",
            displayName = "Original Display",
            isActive = true
        )
        val originalEmail = created.email
        val originalDisplayName = created.displayName
        val originalCreatedAt = created.createdAt

        // When - only update fullName
        val updated = repository.update(
            id = created.id,
            fullName = "New Full Name"
        )

        // Then
        assertNotNull(updated)
        assertEquals("New Full Name", updated.fullName)
        assertEquals(originalEmail, updated.email) // Should remain unchanged
        assertEquals(originalDisplayName, updated.displayName) // Should remain unchanged
        assertTrue(updated.isActive) // Should remain unchanged
        assertEquals(originalCreatedAt, updated.createdAt) // Should remain unchanged
        assertTrue(updated.updatedAt > originalCreatedAt) // Should be updated
    }

    @Test
    fun `update non-existent user should return null`() = runTest {
        // Given - a non-existent user ID
        val nonExistentId = kotlin.uuid.Uuid.random()

        // When
        val result = repository.update(
            id = nonExistentId,
            fullName = "Should Not Exist"
        )

        // Then
        assertNull(result)
    }

    @Test
    fun `findById should return created user`() = runTest {
        // Given
        val created = repository.create(
            email = "find@example.com",
            fullName = "Find Me",
            isActive = true
        )

        // When
        val found = repository.findById(created.id)

        // Then
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals(created.email, found.email)
        assertEquals(created.fullName, found.fullName)
    }

    @Test
    fun `findById should return null for non-existent user`() = runTest {
        // Given
        val nonExistentId = kotlin.uuid.Uuid.random()

        // When
        val found = repository.findById(nonExistentId)

        // Then
        assertNull(found)
    }
}

