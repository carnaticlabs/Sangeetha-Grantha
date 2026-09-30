---
name: scaffold-service
description: Scaffolds a new backend service adhering to clean architecture patterns in Kotlin Ktor: Interface (I<Service>), Implementation (<Service>Impl), Koin DI registration, and JUnit unit test skeleton. Use when creating a new backend domain or application service.
---

# Scaffold Backend Service

This skill generates standard boilerplate for a new backend service in `modules/backend/api`, adhering to Sangita Grantha's interface-driven architecture, Koin dependency injection, and test harness conventions.

## 1. Gather Service Specifications

Determine:
- **Service Name**: e.g., `AnnotationService`
- **Package Path**: `com.sangita.grantha.backend.api.services`
- **Injected Dependencies**: e.g., `SangitaDal`

---

## 2. Generate Service Interface & Implementation

File: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/<ServiceName>.kt`

```kotlin
package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import java.util.UUID

/**
 * Interface for <ServiceName>.
 */
interface I<ServiceName> {
    suspend fun getById(id: UUID): String?
}

/**
 * Production implementation of [I<ServiceName>].
 */
class <ServiceName>Impl(
    private val dal: SangitaDal
) : I<ServiceName> {

    override suspend fun getById(id: UUID): String? {
        TODO("Not yet implemented")
    }
}
```

---

## 3. Generate Service Unit Test Skeleton

File: `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/<ServiceName>Test.kt`

```kotlin
package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.support.TestDatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

class <ServiceName>Test {
    private lateinit var dal: SangitaDalImpl
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
    fun `test getById returns expected entity`() = runTest {
        // Given
        val id = UUID.randomUUID()

        // When
        // val result = service.getById(id)

        // Then
        // assertNotNull(result)
    }
}
```

---

## 4. Register in Dependency Injection (Koin)

File: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/di/AppModule.kt`

Add the single binding inside `appModule`:

```kotlin
single<I<ServiceName>> { <ServiceName>Impl(get()) }
```

---

## 5. Verify Build & Compilation

Verify the new service compiles cleanly with Gradle:

```bash
./gradlew :modules:backend:api:compileKotlin :modules:backend:api:compileTestKotlin
```
