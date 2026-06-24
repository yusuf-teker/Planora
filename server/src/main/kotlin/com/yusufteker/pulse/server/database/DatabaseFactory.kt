package com.yusufteker.pulse.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Handles PostgreSQL database connection, connection pooling, and migrations.
 */
object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val driverClass = "org.postgresql.Driver"
        val url = config.property("database.url").getString()
        val user = config.property("database.user").getString()
        val password = config.property("database.password").getString()

        println("========== DATABASE CONFIG ==========")
        println("DB_URL = $url")
        println("DB_USER = $user")
        println("DB_PASSWORD = ${password?.replace(Regex("."), "*")}")
        println("====================================")

        // 1. Configure HikariCP Connection Pool
        val hikariConfig = HikariConfig().apply {
            driverClassName = driverClass
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(hikariConfig)

        // 2. Run Flyway Migrations automatically on startup
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .load()

        flyway.migrate()
        println("Flyway OK")        // 3. Connect Exposed ORM to the Data Source
        Database.connect(dataSource)
    }

    /**
     * Utility function to run database operations safely in a coroutine off the main thread.
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
