package com.example.ul_fitness

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        val dbUrl = System.getenv("DB_URL")
            ?: System.getenv("DATABASE_URL")
            ?: "jdbc:mariadb://localhost:3306/ul_fitness"
        val user = System.getenv("MARIADB_USER") ?: System.getenv("DB_USER") ?: "ulf"
        val password = System.getenv("MARIADB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: System.getenv("MARIADB_ROOT_PASSWORD") ?: "ulf"
        val hikari = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = user
            this.password = password
            driverClassName = "org.mariadb.jdbc.Driver"
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val ds = HikariDataSource(hikari)
        // Flyway migrate
        val flyway = Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
        flyway.migrate()
        Database.connect(ds)
    }
}
