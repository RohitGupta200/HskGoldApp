package org.cap.gold.migration

import kotlinx.coroutines.runBlocking

/**
 * Simple interactive migration that prompts for credentials
 */
fun main() = runBlocking {
    println("🎯 CapGold Database Migration Tool")
    println("==================================")
    println()

    // Get Render database details
    println("📋 Render Database (Source):")
    print("Enter Render database URL: ")
    val sourceUrl = readLine() ?: ""

    print("Enter Render database user: ")
    val sourceUser = readLine() ?: ""

    print("Enter Render database password: ")
    val sourcePassword = readLine() ?: ""

    println()

    // Get Supabase database details
    println("📋 Supabase Database (Target):")
    print("Enter Supabase project reference (from db.XXX.supabase.co): ")
    val projectRef = readLine() ?: ""

    print("Enter Supabase database password: ")
    val targetPassword = readLine() ?: ""

    // Construct Supabase URL
    val targetUrl = "postgresql://postgres:$targetPassword@db.$projectRef.supabase.co:5432/postgres"

    println()
    println("📋 Migration Summary:")
    println("Source: ${maskCredentials(sourceUrl)}")
    println("Target: postgresql://postgres:****@db.$projectRef.supabase.co:5432/postgres")
    println()

    print("Do you want to proceed with the migration? (y/N): ")
    val proceed = readLine()?.lowercase()

    if (proceed != "y") {
        println("Migration cancelled.")
        return@runBlocking
    }

    try {
        println("🚀 Starting migration...")

        val migrationService = DatabaseMigrationService(
            sourceUrl = sourceUrl,
            sourceUser = sourceUser,
            sourcePassword = sourcePassword,
            targetUrl = targetUrl,
            targetUser = "postgres",
            targetPassword = targetPassword
        )

        // Initialize and run migration
        migrationService.initializeDatabases()
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
                println("✅ Migration verification passed!")
                println()
                println("🎉 Next Steps:")
                println("1. Update your Render environment variables to use Supabase")
                println("2. Deploy your application")
                println("3. Test all functionality")
            } else {
                println("❌ Migration verification failed!")
                verification.error?.let { println("Error: $it") }
            }
        } else {
            println("❌ Migration failed!")
            result.error?.let { println("Error: $it") }
        }

    } catch (e: Exception) {
        println("❌ Migration failed: ${e.message}")
        e.printStackTrace()
    }
}

private fun maskCredentials(url: String): String {
    return url.replace(Regex("://([^:]+):([^@]+)@"), "://$1:****@")
}