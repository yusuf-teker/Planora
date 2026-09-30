package com.yusufteker.planora.server.support

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Utility responsible for cleaning database state between integration tests.
 *
 * Employs a deterministic TRUNCATE CASCADE strategy:
 * - Dynamically fetches all user tables under the 'public' schema from PostgreSQL metadata.
 * - Excludes 'flyway_schema_history' so database migrations are never erased or compromised.
 * - Executes TRUNCATE with RESTART IDENTITY CASCADE to reset primary key sequences and clear data.
 */
object DatabaseCleanupHelper {

    /**
     * Truncates all tables in the public schema except `flyway_schema_history`.
     */
    fun cleanDatabase() {
        transaction {
            // Find all user tables in public schema
            val tables = mutableListOf<String>()
            exec(
                """
                SELECT tablename 
                FROM pg_tables 
                WHERE schemaname = 'public' 
                  AND tablename != 'flyway_schema_history';
                """.trimIndent()
            ) { rs ->
                while (rs.next()) {
                    tables.add(rs.getString("tablename"))
                }
            }

            if (tables.isNotEmpty()) {
                // Construct single statement: TRUNCATE TABLE "t1", "t2" RESTART IDENTITY CASCADE
                val tableNamesSql = tables.joinToString(", ") { "\"$it\"" }
                exec("TRUNCATE TABLE $tableNamesSql RESTART IDENTITY CASCADE;")
            }
        }
    }
}
