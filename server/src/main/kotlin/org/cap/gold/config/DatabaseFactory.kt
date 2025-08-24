package org.cap.gold.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class DatabaseFactory(config: ApplicationConfig) {
    private val hikariConfig = HikariConfig().apply {
        driverClassName = config.property("database.driverClassName").getString()
        jdbcUrl = config.property("database.jdbcURL").getString()
        username = config.propertyOrNull("database.user")?.getString()
        password = config.propertyOrNull("database.password")?.getString()
        maximumPoolSize = config.property("database.maxPoolSize").getString().toInt()
        connectionTimeout = config.property("database.connectionTimeout").getString().toLong()
        idleTimeout = config.property("database.idleTimeout").getString().toLong()
        maxLifetime = config.property("database.maxLifetime").getString().toLong()
        isAutoCommit = false
        validate()
    }

    private val dataSource: DataSource = HikariDataSource(hikariConfig)

    init {
        // Initialize Exposed
        Database.connect(dataSource)
        
        // Test the connection
        transaction {
            // This will throw an exception if the connection fails
            exec("SELECT 1")
        }
    }

    companion object {
        suspend fun <T> dbQuery(block: suspend () -> T): T =
            newSuspendedTransaction(Dispatchers.IO) { block() }
    }
}

// Extension function to get database config from ApplicationConfig
fun ApplicationConfig.getDatabaseConfig(): DatabaseFactory {
    return DatabaseFactory(this)
}
