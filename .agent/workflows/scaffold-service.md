---
description: Scaffolds a new backend service following the clean architecture pattern (Interface, Implementation, DI, Test).
---

# Scaffold Backend Service

This workflow generates the boilerplate for a new backend service, ensuring adherence to the project's interface-based architecture and dependency injection patterns.

## 1. Gather Inputs

- **Service Name:** (e.g., `PlaylistService`)
- **Package:** (e.g., `com.sangita.grantha.backend.api.services`)
- **Dependencies:** (e.g., `SangitaDal`)

## 2. Generate Service Interface & Implementation

Create `modules/backend/api/src/main/kotlin/.../services/<ServiceName>.kt`:

```kotlin
package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import java.util.UUID

/**
 * Interface for <ServiceName>.
 */
interface I<ServiceName> {
    suspend fun exampleMethod(id: UUID): String
}

class <ServiceName>Impl(
    private val dal: SangitaDal
) : I<ServiceName> {

    override suspend fun exampleMethod(id: UUID): String {
        TODO("Not yet implemented")
    }
}
```

## 3. Generate Unit Test Skeleton

Create `modules/backend/api/src/test/kotlin/.../services/<ServiceName>Test.kt`:

```kotlin
package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.support.TestDatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class <ServiceName>Test {
    private lateinit var dal: SangitaDal
    private lateinit var service: I<ServiceName>

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.connectTestDb()
        dal = SangitaDalImpl()
        service = <ServiceName>Impl(dal)
    }

    @AfterEach
    fun teardown() {
        TestDatabaseFactory.reset()
    }

    @Test
    fun `example test case`() = runTest {
        // Given

        // When

        // Then
        // assertNotNull(result)
    }
}
```

## 4. Register in DI (If Koin is active)

*Note: If manual DI is still in use in `App.kt`, instruct the user to wire it there.*

If `di/AppModule.kt` exists:
1.  Read `modules/backend/api/src/main/kotlin/.../di/AppModule.kt`.
2.  Add `single<I<ServiceName>> { <ServiceName>Impl(get()) }` to the module definition.

## 5. Review & Refine

- User reviews the generated files.
- Run tests to ensure compilation (even if test fails `TODO`).
