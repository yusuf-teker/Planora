package com.yusufteker.pulse.server.database

import com.yusufteker.pulse.server.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Handles PostgreSQL database connection, connection pooling, and migrations.
 */
object DatabaseFactory {
    fun init() {
        val url = AppConfig.dbUrl
        val user = AppConfig.dbUser
        val password = AppConfig.dbPassword

        // 1. Configure HikariCP Connection Pool
        // ktor ile postgresql bağlantısı kurmak için HikariCP kullanıyoruz.
        // Birden fazla connection açıp havuzda saklıyoruz. Böylece her istekte tekrar bağlantı açmak yerine
        // havuzdan bir connection alıp kullanıyoruz.
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false //Birden fazla sıralı istek olduğunda hepsini tek seferde commit et
            // birisi patlarsa diğerinide iptal etmesi için autoCommit false yapıyoruz.
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(hikariConfig)

        // Flyaw database migration'ları çalıştırmak için Flyway kullanıyoruz.
        // Migration'lar, veritabanı şemasını güncel tutmamızı sağlar.
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true) // Eğer veritabanı boşsa, mevcut şemayı baseline olarak kabul et.
            .load()

        flyway.migrate()
        println("Flyway OK")

        // 3. Connect Exposed ORM to the Data Source
        Database.connect(dataSource)

        // Seed dummy data if DB is empty
        DatabaseSeeder.seed()
    }

    /**
     * Utility function to run database operations safely in a coroutine off the main thread.
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() } //Transaction oluştur IO 'da çalıştır
}
