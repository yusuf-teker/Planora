package com.yusufteker.planora.server.database

import com.yusufteker.planora.server.AppConfig
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
        // Ktor ile PostgreSQL bağlantısı kurmak için HikariCP kullanıyoruz.
        // Birden fazla connection açıp havuzda saklıyoruz. Böylece her istekte tekrar bağlantı açmak yerine
        // havuzdan hazır bir connection alıp kullanıyoruz.
        val hikariConfig = HikariConfig().apply {
            poolName = "PlanoraHikariPool"
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = url
            username = user
            this.password = password

            // Havuz boyutu ayarları
            maximumPoolSize = 10
            minimumIdle = 2 // Boşta en az 2 bağlantıyı hazır tutarak ilk istek gecikmesini (cold-start) önler

            // Zaman aşımı ve yaşam döngüsü ayarları (Özellikle Render, Supabase, Neon gibi bulut ortamları için kritik)
            connectionTimeout = 30_000 // 30 sn: Havuz dolu olduğunda isteklerin sonsuz kilitlenmesini önler
            idleTimeout = 600_000 // 10 dk: minimumIdle üzerindeki boşta kalan fazla bağlantıları temizler
            maxLifetime = 1_800_000 // 30 dk: Bulut sağlayıcıların eski TCP bağlantılarını sessizce koparmasını önlemek için bağlantıyı tazeler
            keepaliveTime = 60_000 // 1 dk: Bulut proxy ve firewall'ların bağlantıyı düşürmesini önlemek için canlı tutma sinyali (ping) gönderir

            // Bağlantı sızıntısı tespiti
            leakDetectionThreshold = 10_000 // 10 sn: Kapatılmayan veya askıda kalan sorguları loglayarak tespit eder

            // Transaction ve commit yönetimi
            isAutoCommit = false // Birden fazla sıralı istek olduğunda hepsini tek seferde commit et
            // Birisi patlarsa diğerini de iptal etmesi için autoCommit false yapıyoruz.
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
        val dataSource = HikariDataSource(hikariConfig)

        // Flyaw database migration'ları çalıştırmak için Flyway kullanıyoruz.
        // Migration'lar, veritabanı şemasını güncel tutmamızı sağlar.
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .cleanDisabled(true) // Disable clean to prevent accidental wipes
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
