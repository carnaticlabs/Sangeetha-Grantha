package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

/**
 * TRACK-112: transaction-nesting contract for [DatabaseFactory.dbQuery].
 *
 * A nested `dbQuery` must **join** the caller's transaction, so a service can make a multi-repo
 * operation atomic by wrapping it. Before this was fixed, each nested call opened its own
 * transaction and committed independently — a wrap looked like a transaction boundary, compiled
 * fine, and did nothing. `ImportService`'s approval path relied on that and could leave a
 * committed krithi behind when a later step failed.
 *
 * These tests are the regression guard. If they fail, every service that wraps repo calls for
 * atomicity has silently lost it.
 */
@DisplayName("dbQuery transaction nesting")
class DbQueryNestingTest : IntegrationTestBase() {

    private lateinit var dal: SangitaDal

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
    }

    @Test
    fun `a nested write rolls back when the enclosing dbQuery fails`() = runTest {
        val email = "nesting-rollback-${Uuid.random()}@example.test"

        val outcome = runCatching {
            DatabaseFactory.dbQuery {
                dal.users.create(email = email, fullName = "Rollback Probe")
                error("enclosing operation fails after the nested write")
            }
        }

        assertEquals(true, outcome.isFailure, "the enclosing dbQuery must propagate the failure")
        assertNull(
            dal.users.findByEmail(email),
            "the nested write must roll back with its enclosing transaction — if this row survived, " +
                "nested dbQuery is opening its own transaction again and no service wrap is atomic"
        )
    }

    @Test
    fun `nested writes commit together when the enclosing dbQuery succeeds`() = runTest {
        val first = "nesting-commit-a-${Uuid.random()}@example.test"
        val second = "nesting-commit-b-${Uuid.random()}@example.test"

        DatabaseFactory.dbQuery {
            dal.users.create(email = first, fullName = "Commit Probe A")
            dal.users.create(email = second, fullName = "Commit Probe B")
        }

        assertNotNull(dal.users.findByEmail(first), "both nested writes must be visible after commit")
        assertNotNull(dal.users.findByEmail(second), "both nested writes must be visible after commit")
    }

    @Test
    fun `an un-nested repo call still commits on its own`() = runTest {
        // The non-nesting path must be untouched: no ambient transaction, so dbQuery opens one.
        val email = "nesting-standalone-${Uuid.random()}@example.test"
        dal.users.create(email = email, fullName = "Standalone Probe")
        assertNotNull(dal.users.findByEmail(email), "a standalone repo call must commit as before")
    }

    @Test
    fun `a failure in one un-nested call does not roll back a previous one`() = runTest {
        // Sibling calls with no enclosing dbQuery remain independent — this is the pre-existing
        // behaviour that callers who do NOT wrap continue to get.
        val email = "nesting-independent-${Uuid.random()}@example.test"
        dal.users.create(email = email, fullName = "Independent Probe")

        runCatching { DatabaseFactory.dbQuery { error("a later, separate operation fails") } }

        assertNotNull(
            dal.users.findByEmail(email),
            "an unrelated earlier commit must survive a later independent failure"
        )
    }
}
