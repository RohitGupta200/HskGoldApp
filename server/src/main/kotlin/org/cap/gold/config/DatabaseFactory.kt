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
        val sslMode = config.propertyOrNull("database.sslmode")?.getString()
        jdbcUrl = normalizeJdbcUrl(rawUrl, sslMode)
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

// Convert postgres URL to JDBC if needed and optionally apply sslmode from config.
private fun normalizeJdbcUrl(url: String, sslMode: String?): String {
    if (url.startsWith("jdbc:")) return applySslModeIfProvided(url, sslMode)
    if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
        val normalized = if (url.startsWith("postgres://")) url.replaceFirst("postgres://", "postgresql://") else url
        val uri = URI(normalized)
        val host = uri.host
        val port = if (uri.port == -1) 5432 else uri.port
        val database = uri.path.trimStart('/')
        val query = uri.rawQuery ?: ""
        val jdbcBase = "jdbc:postgresql://$host:$port/$database"
        val finalQuery = if (sslMode.isNullOrBlank()) query else ensureOrOverrideSslInQuery(query, sslMode)
        return if (finalQuery.isBlank()) jdbcBase else "$jdbcBase?$finalQuery"
    }
    return applySslModeIfProvided(url, sslMode)
}

private fun applySslModeIfProvided(jdbcUrl: String, sslMode: String?): String {
    if (sslMode.isNullOrBlank()) return jdbcUrl
    val parts = jdbcUrl.split("?", limit = 2)
    val base = parts[0]
    val query = if (parts.size > 1) parts[1] else ""
    val finalQuery = ensureOrOverrideSslInQuery(query, sslMode)
    return if (finalQuery.isBlank()) base else "$base?$finalQuery"
}

private fun ensureOrOverrideSslInQuery(query: String, sslMode: String): String {
    if (query.isBlank()) return "sslmode=$sslMode"
    val params = query.split('&').toMutableList()
    var found = false
    for (i in params.indices) {
        if (params[i].startsWith("sslmode=")) {
            params[i] = "sslmode=$sslMode"
            found = true
            break
        }
    }
    if (!found) params.add("sslmode=$sslMode")
    return params.joinToString("&")
}

