package com.sangita.grantha.backend.dal.errors

import java.sql.SQLException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.postgresql.util.PSQLException

/**
 * Typed DAL errors (TRACK-111, D5).
 *
 * Repositories let Exposed/Postgres exceptions propagate; the central mapper in
 * [com.sangita.grantha.backend.dal.DatabaseFactory.dbQuery] translates well-known Postgres
 * SQLState codes into these so callers (services, routes) can branch on a meaningful type instead
 * of inspecting a raw `PSQLException`. Anything not modelled here propagates unchanged.
 */
sealed class DalException(message: String, cause: Throwable?) : RuntimeException(message, cause) {
    /** The Postgres SQLState that produced this error. */
    abstract val sqlState: String

    /** The violated constraint name, when Postgres reported one. */
    abstract val constraint: String?
}

/** Unique / primary-key violation — Postgres SQLState `23505`. */
class DuplicateKeyException(
    override val constraint: String?,
    cause: Throwable? = null,
) : DalException("Duplicate key violates constraint ${constraint ?: "(unknown)"}", cause) {
    override val sqlState: String = SQL_STATE

    companion object {
        const val SQL_STATE: String = "23505"
    }
}

/** Foreign-key violation — Postgres SQLState `23503`. */
class ForeignKeyViolationException(
    override val constraint: String?,
    cause: Throwable? = null,
) : DalException("Foreign key violates constraint ${constraint ?: "(unknown)"}", cause) {
    override val sqlState: String = SQL_STATE

    companion object {
        const val SQL_STATE: String = "23503"
    }
}

/**
 * Maps an [ExposedSQLException] to a typed [DalException], or returns `null` when the underlying
 * SQLState is not one we model (the original exception should then propagate unchanged).
 *
 * The SQLState lives on the wrapped [SQLException], so we walk the cause chain (same idiom as
 * `EntityResolutionCacheRepository.isTypeAndNameUniqueViolation`).
 */
fun ExposedSQLException.toDalExceptionOrNull(): DalException? {
    val sqlState =
        generateSequence<Throwable>(this) { it.cause }
            .filterIsInstance<SQLException>()
            .mapNotNull { it.sqlState }
            .firstOrNull()
            ?: return null

    val constraint = constraintName()
    return when (sqlState) {
        DuplicateKeyException.SQL_STATE -> DuplicateKeyException(constraint, this)
        ForeignKeyViolationException.SQL_STATE -> ForeignKeyViolationException(constraint, this)
        else -> null
    }
}

/** Best-effort constraint name from the Postgres server error message, if present. */
private fun Throwable.constraintName(): String? =
    generateSequence<Throwable>(this) { it.cause }
        .filterIsInstance<PSQLException>()
        .mapNotNull { it.serverErrorMessage?.constraint }
        .firstOrNull()
