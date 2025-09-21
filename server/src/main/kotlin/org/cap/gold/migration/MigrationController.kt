package org.cap.gold.migration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.cap.gold.config.DatabaseFactory

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