package org.cap.gold.migration

import kotlinx.coroutines.runBlocking

/**
 * Standalone migration runner that can be executed separately from the main application.
 * This allows you to run the migration without affecting your running server.
 */
object MigrationRunner {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 6) {
            println("""
                Usage: MigrationRunner <sourceUrl> <sourceUser> <sourcePassword> <targetUrl> <targetUser> <targetPassword>

                Example:
                MigrationRunner \
                  "jdbc:postgresql://dpg-abc123-a.oregon-postgres.render.com:5432/capgold_db" \
                  "capgold_user" \
                  "source_password" \
                  "postgresql://postgres:target_password@db.supabase_project_id.supabase.co:5432/postgres" \
                  "postgres" \
                  "target_password"

                Environment variables can also be used:
                - SOURCE_DATABASE_URL
                - SOURCE_DB_USER
                - SOURCE_DB_PASSWORD
                - TARGET_DATABASE_URL
                - TARGET_DB_USER
                - TARGET_DB_PASSWORD
            """.trimIndent())
            return
        }

        val sourceUrl = args.getOrNull(0) ?: System.getenv("SOURCE_DATABASE_URL")
        val sourceUser = args.getOrNull(1) ?: System.getenv("SOURCE_DB_USER")
        val sourcePassword = args.getOrNull(2) ?: System.getenv("SOURCE_DB_PASSWORD")
        val targetUrl = args.getOrNull(3) ?: System.getenv("TARGET_DATABASE_URL")
        val targetUser = args.getOrNull(4) ?: System.getenv("TARGET_DB_USER")
        val targetPassword = args.getOrNull(5) ?: System.getenv("TARGET_DB_PASSWORD")

        if (listOf(sourceUrl, sourceUser, sourcePassword, targetUrl, targetUser, targetPassword).any { it.isNullOrBlank() }) {
            println("❌ Error: All database connection parameters must be provided")
            return
        }

        runBlocking {
            try {
                println("🚀 Starting database migration...")
                println("Source: ${maskUrl(sourceUrl!!)}")
                println("Target: ${maskUrl(targetUrl!!)}")

                val migrationService = DatabaseMigrationService(
                    sourceUrl = sourceUrl,
                    sourceUser = sourceUser!!,
                    sourcePassword = sourcePassword!!,
                    targetUrl = targetUrl,
                    targetUser = targetUser!!,
                    targetPassword = targetPassword!!
                )

                // Initialize databases and create schema
                println("📋 Initializing databases and creating target schema...")
                migrationService.initializeDatabases()

                // Run the migration
                println("📦 Starting data migration...")
                val result = migrationService.migrateAllData()

                if (result.success) {
                    println("✅ Migration completed successfully!")
                    println("📊 Migration Summary:")
                    println("  - Admin Users: ${result.adminUsers}")
                    println("  - Categories: ${result.categories}")
                    println("  - About Us: ${result.aboutUs}")
                    println("  - Approved Products: ${result.productsApproved}")
                    println("  - Unapproved Products: ${result.productsUnapproved}")
                    println("  - Product Images: ${result.productImages}")
                    println("  - Orders: ${result.orders}")
                    println("  - Total Records: ${result.getTotalRecords()}")

                    // Verify migration
                    println("🔍 Verifying migration...")
                    val verification = migrationService.verifyMigration()

                    if (verification.success) {
                        println("✅ Migration verification passed! All data transferred successfully.")
                    } else {
                        println("❌ Migration verification failed!")
                        verification.error?.let { println("Error: $it") }
                    }
                } else {
                    println("❌ Migration failed!")
                    result.error?.let { println("Error: $it") }
                }

            } catch (e: Exception) {
                println("❌ Migration failed with exception: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun maskUrl(url: String): String {
        return url.replace(Regex("://([^:]+):([^@]+)@"), "://$1:****@")
    }
}

/**
 * Interactive migration runner with confirmation prompts
 */
class InteractiveMigrationRunner {

    fun runInteractiveMigration() = runBlocking {
        println("🎯 CapGold Database Migration Tool")
        println("==================================")

        // Get connection details
        val sourceUrl = promptForInput("Enter source database URL (Render): ")
        val sourceUser = promptForInput("Enter source database user: ")
        val sourcePassword = promptForPassword("Enter source database password: ")

        val targetUrl = promptForInput("Enter target database URL (Supabase): ")
        val targetUser = promptForInput("Enter target database user: ")
        val targetPassword = promptForPassword("Enter target database password: ")

        // Confirm migration
        println("\n📋 Migration Summary:")
        println("Source: ${maskUrl(sourceUrl)}")
        println("Target: ${maskUrl(targetUrl)}")

        if (!confirmAction("Do you want to proceed with the migration? (y/N): ")) {
            println("Migration cancelled.")
            return@runBlocking
        }

        try {
            val migrationService = DatabaseMigrationService(
                sourceUrl = sourceUrl,
                sourceUser = sourceUser,
                sourcePassword = sourcePassword,
                targetUrl = targetUrl,
                targetUser = targetUser,
                targetPassword = targetPassword
            )

            migrationService.initializeDatabases()
            val result = migrationService.migrateAllData()

            if (result.success) {
                println("✅ Migration completed successfully!")
                migrationService.verifyMigration()
            } else {
                println("❌ Migration failed: ${result.error}")
            }

        } catch (e: Exception) {
            println("❌ Migration failed: ${e.message}")
        }
    }

    private fun promptForInput(prompt: String): String {
        print(prompt)
        return readlnOrNull() ?: ""
    }

    private fun promptForPassword(prompt: String): String {
        print(prompt)
        return System.console()?.readPassword()?.let { String(it) } ?: readlnOrNull() ?: ""
    }

    private fun confirmAction(prompt: String): Boolean {
        print(prompt)
        return readlnOrNull()?.lowercase() == "y"
    }

    private fun maskUrl(url: String): String {
        return url.replace(Regex("://([^:]+):([^@]+)@"), "://$1:****@")
    }
}