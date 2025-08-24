package org.cap.gold.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource
import java.net.URI

class DatabaseFactory(config: ApplicationConfig) {
    private val hikariConfig = HikariConfig().apply {
        driverClassName = config.property("database.driverClassName").getString()
        val rawUrl = config.property("database.jdbcURL").getString()
        jdbcUrl = normalizeJdbcUrl(rawUrl)
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

// Convert Render-style postgres URL to JDBC if needed and enforce sslmode=require
private fun normalizeJdbcUrl(url: String): String {
    if (url.startsWith("jdbc:")) return ensureSslMode(url)
    if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
        val normalized = if (url.startsWith("postgres://")) url.replaceFirst("postgres://", "postgresql://") else url
        val uri = URI(normalized)
        val host = uri.host
        val port = if (uri.port == -1) 5432 else uri.port
        val database = uri.path.trimStart('/')
        val query = uri.rawQuery ?: ""
        val jdbcBase = "jdbc:postgresql://$host:$port/$database"
        val finalQuery = if (query.isBlank()) "sslmode=require" else ensureSslInQuery(query)
        return if (finalQuery.isBlank()) jdbcBase else "$jdbcBase?$finalQuery"
    }
    return url
}

private fun ensureSslMode(jdbcUrl: String): String {
    val parts = jdbcUrl.split("?", limit = 2)
    val base = parts[0]
    val query = if (parts.size > 1) parts[1] else ""
    val finalQuery = if (query.isBlank()) "sslmode=require" else ensureSslInQuery(query)
    return if (finalQuery.isBlank()) base else "$base?$finalQuery"
}

private fun ensureSslInQuery(query: String): String {
    val hasSsl = query.split('&').any { it.startsWith("sslmode=") }
    return if (hasSsl) query else if (query.isBlank()) "sslmode=require" else "$query&sslmode=require"
}
