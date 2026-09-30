package com.yusufteker.planora.server.support

import org.junit.Before
import org.junit.BeforeClass

/**
 * Base test class for all integration tests requiring real PostgreSQL database interactions.
 *
 * Ensures:
 * 1. A single PostgreSQL Testcontainer instance is running and fully migrated via Flyway.
 * 2. Database state is cleanly wiped before each test method execution via [DatabaseCleanupHelper.cleanDatabase].
 * 3. Complete test isolation without relying on transactions that may be committed by Ktor route handlers.
 */
abstract class BaseDatabaseIntegrationTest {

    companion object {
        /**
         * Ensures Testcontainers PostgreSQL is booted and Flyway migrations are executed
         * before running any test cases in this test suite.
         */
        @JvmStatic
        @BeforeClass
        fun setupDatabase() {
            TestDatabaseContainer.start(runSeeder = false)
        }
    }

    /**
     * Cleans up all tables prior to each test to ensure tests do not leak state to one another.
     */
    @Before
    fun resetDatabaseState() {
        DatabaseCleanupHelper.cleanDatabase()
    }
}
