package org.cap.gold.migration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class MigrationRequest(
    val targetUrl: String,
    val targetUser: String = "postgres",
    val targetPassword: String,
    val dryRun: Boolean = false
)

@Serializable
data class MigrationResponse(
    val success: Boolean,
    val message: String,
    val result: MigrationResult? = null,
    val verification: MigrationVerification? = null
)

fun Route.migrationRoutes() {
    route("/admin/migration") {

        // GET endpoint to check migration status
        get("/status") {
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "Migration tool ready",
                "message" to "Use POST /admin/migration/start to begin migration"
            ))
        }

        // Debug endpoint to test URL parsing
        post("/test-url") {
            try {
                val request = call.receive<MigrationRequest>()
                val migrationService = DatabaseMigrationService(
                    targetUrl = request.targetUrl,
                    targetUser = request.targetUser,
                    targetPassword = request.targetPassword
                )

                // Test URL normalization without actually connecting
                val testResult = migrationService.testUrlNormalization(request.targetUrl)

                call.respond(HttpStatusCode.OK, mapOf(
                    "original_url" to request.targetUrl,
                    "normalized_url" to testResult,
                    "message" to "URL parsing test completed"
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to e.message,
                    "message" to "URL parsing test failed"
                ))
            }
        }

        // Network connectivity test endpoint
        post("/test-network") {
            try {
                val request = call.receive<MigrationRequest>()

                // Parse URL to get host and port
                val (jdbcUrl, username, password) = try {
                    val migrationService = DatabaseMigrationService(
                        targetUrl = request.targetUrl,
                        targetUser = request.targetUser,
                        targetPassword = request.targetPassword
                    )
                    val urlInfo = request.targetUrl
                    val withoutProtocol = if (urlInfo.startsWith("postgresql://")) urlInfo.substring("postgresql://".length) else urlInfo
                    val atIndex = withoutProtocol.indexOf('@')
                    val hostAndPath = if (atIndex != -1) withoutProtocol.substring(atIndex + 1) else withoutProtocol
                    val slashIndex = hostAndPath.indexOf('/')
                    val hostPart = if (slashIndex != -1) hostAndPath.substring(0, slashIndex) else hostAndPath
                    val colonIndex = hostPart.lastIndexOf(':')
                    val host = if (colonIndex != -1) hostPart.substring(0, colonIndex) else hostPart
                    val port = if (colonIndex != -1) hostPart.substring(colonIndex + 1).toIntOrNull() ?: 5432 else 5432

                    Triple(host, port, "parsed successfully")
                } catch (e: Exception) {
                    Triple("unknown", 0, "parsing failed: ${e.message}")
                }

                val host = jdbcUrl
                val port = try { username.toInt() } catch (e: Exception) { 5432 }

                // Test network connectivity
                val networkTest = try {
                    java.net.Socket().use { socket ->
                        socket.connect(java.net.InetSocketAddress(host, port), 10000)
                        "✅ Network connection successful to $host:$port"
                    }
                } catch (e: java.net.ConnectException) {
                    "❌ Connection refused to $host:$port - ${e.message}"
                } catch (e: java.net.SocketTimeoutException) {
                    "❌ Connection timeout to $host:$port - ${e.message}"
                } catch (e: java.net.UnknownHostException) {
                    "❌ Unknown host: $host - ${e.message}"
                } catch (e: java.net.SocketException) {
                    "❌ Network error to $host:$port - ${e.message}"
                } catch (e: Exception) {
                    "❌ Network test failed: ${e.message}"
                }

                call.respond(HttpStatusCode.OK, mapOf(
                    "host" to host,
                    "port" to port,
                    "network_test" to networkTest,
                    "url_parsing" to password,
                    "message" to "Network connectivity test completed"
                ))

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to e.message,
                    "message" to "Network test failed"
                ))
            }
        }

        // POST endpoint to start migration
        post("/start") {
            try {
                val request = call.receive<MigrationRequest>()

                // Validate request
                if (request.targetUrl.isBlank() || request.targetPassword.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, MigrationResponse(
                        success = false,
                        message = "Target database URL and password are required"
                    ))
                    return@post
                }

                if (request.dryRun) {
                    call.respond(HttpStatusCode.OK, MigrationResponse(
                        success = true,
                        message = "Dry run - would migrate from current database to ${request.targetUrl}"
                    ))
                    return@post
                }

                // Run migration using current database connection
                val migrationService = DatabaseMigrationService(
                    targetUrl = request.targetUrl,
                    targetUser = request.targetUser,
                    targetPassword = request.targetPassword
                )

                withContext(Dispatchers.IO) {
                    migrationService.initializeDatabases()
                }

                val result = migrationService.migrateAllData()
                val verification = if (result.success) {
                    migrationService.verifyMigration()
                } else null

                call.respond(HttpStatusCode.OK, MigrationResponse(
                    success = result.success,
                    message = if (result.success) {
                        "Migration completed successfully! Migrated ${result.getTotalRecords()} records."
                    } else {
                        "Migration failed: ${result.error}"
                    },
                    result = result,
                    verification = verification
                ))

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, MigrationResponse(
                    success = false,
                    message = "Migration failed: ${e.message}"
                ))
            }
        }

        // POST endpoint for verification only
        post("/verify") {
            try {
                val request = call.receive<MigrationRequest>()

                val migrationService = DatabaseMigrationService(
                    targetUrl = request.targetUrl,
                    targetUser = request.targetUser,
                    targetPassword = request.targetPassword
                )

                withContext(Dispatchers.IO) {
                    migrationService.initializeDatabases()
                }

                val verification = migrationService.verifyMigration()

                call.respond(HttpStatusCode.OK, MigrationResponse(
                    success = verification.success,
                    message = if (verification.success) {
                        "Verification passed! Databases are in sync."
                    } else {
                        "Verification failed: ${verification.error}"
                    },
                    verification = verification
                ))

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, MigrationResponse(
                    success = false,
                    message = "Verification failed: ${e.message}"
                ))
            }
        }
    }
}