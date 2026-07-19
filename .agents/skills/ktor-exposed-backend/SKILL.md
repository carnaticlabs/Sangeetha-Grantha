---
name: ktor-exposed-backend
description: Guidelines for Kotlin + Ktor + Exposed Backend development.
---

# Kotlin, Ktor & Exposed Backend Development Guidelines

This skill enforces backend development design principles and standard patterns in `modules/backend/`.

## 1. Programming Paradigm: OOP + Functional Hybrid
Backend Kotlin code must combine robust object-oriented architecture (clean separation of concerns, SOLID principles, and abstraction layer separation) with functional programming idioms:
- **Functional Idioms**: Favor immutability (`val` over `var`). Return expressions directly (e.g. `fun get(id: UUID) = ...`). Use Kotlin's standard library collection APIs (`map`, `filter`, `associateBy`, etc.) to process data functionally instead of using mutable loops. Use Monad/Either-like routing results or exception handling mapping.
- **Object-Oriented Design**: Encapsulate business rules in services and data access in repositories. Define explicit contracts (interfaces) for dependencies and inject them.
- **Example of Gold-Standard Kotlin**:
  ```kotlin
  interface KrithiRepository {
      suspend fun findById(id: UUID): KrithiDto?
      suspend fun save(krithi: KrithiDto): KrithiDto
  }

  class KrithiService(
      private val repository: KrithiRepository,
      private val auditLogger: AuditLogger
  ) {
      suspend fun retrieveAndAuditKrithi(id: UUID, actor: String): KrithiDto? =
          repository.findById(id)?.also { krithi ->
              auditLogger.log(
                  action = "READ_KRITHI",
                  target = "krithi/${krithi.id}",
                  actor = actor,
                  details = "User retrieved Krithi: ${krithi.title}"
              )
          }
  }
  ```

## 2. Ktor Routing & Controller Patterns
- **Route Delegation**: Keep route definitions lightweight. Routes should deserialize payloads, perform authentication checks, invoke service layer methods, and map results to HTTP statuses.
- **JWT Claims & Role Checks**: Protect administrative actions with role-based claim checkers:
  ```kotlin
  authenticate("jwt") {
      requireRole(Role.ADMIN) {
          post("/v1/krithis") {
              val payload = call.receive<CreateKrithiDto>()
              val user = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
              val result = service.create(payload, user.email)
              call.respond(HttpStatusCode.Created, result)
          }
      }
  }
  ```
- **Error Handling**: Use global status pages or exception interception rather than wrapping every route in manual try-catch blocks.

## 3. Database Access & Exposed ORM
- **Transaction Scope**: All DB queries, reads, and writes must run inside `DatabaseFactory.dbQuery { }` to guarantee proper transaction handling and connection pooling.
- **DTO Boundaries**: Never return Exposed `Table` row mapping results or `Entity` objects from the data access layer. Map database rows to clean, serializable Kotlin multiplatform DTOs inside `DatabaseFactory.dbQuery` before they leave the repository layer.
- **Audit Logging**: Every mutating database transaction (Insert, Update, Delete) must append an audit entry to the `AUDIT_LOG` table with details of the mutation.
