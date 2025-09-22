package org.cap.gold.migration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cap.gold.config.DatabaseFactory
import org.cap.gold.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.sql.DataSource

class DatabaseMigrationService(
    private val targetUrl: String,
    private val targetUser: String,
    private val targetPassword: String
) {
    private lateinit var targetDb: Database

    suspend fun initializeDatabases() {
        // Use existing database connection for source (already connected via DatabaseFactory)
        // Only create new connection for target
        targetDb = createDatabase(targetUrl, targetUser, targetPassword, "target")

        // Create tables in target database
        createTargetSchema()
    }

    private fun createDatabase(url: String, user: String, password: String, name: String): Database {
        val (jdbcUrl, extractedUser, extractedPassword) = parsePostgreSQLUrl(url)

        // Use extracted credentials if available, otherwise use provided ones
        val finalUser = if (extractedUser.isNotBlank()) extractedUser else user
        val finalPassword = if (extractedPassword.isNotBlank()) extractedPassword else password

        println("Connecting to $name database: $jdbcUrl (user: $finalUser)")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            this.jdbcUrl = jdbcUrl
            username = finalUser
            this.password = finalPassword
            maximumPoolSize = 3
            minimumIdle = 1
            connectionTimeout = 60000  // 60 seconds
            idleTimeout = 300000       // 5 minutes
            maxLifetime = 600000       // 10 minutes
            leakDetectionThreshold = 60000

            // Add connection test query
            connectionTestQuery = "SELECT 1"

            validate()
        }

        try {
            val dataSource = HikariDataSource(config)

            // Test the connection immediately
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { rs ->
                        if (rs.next()) {
                            println("✅ Successfully connected to $name database")
                        }
                    }
                }
            }

            return Database.connect(dataSource)
        } catch (e: Exception) {
            println("❌ Failed to connect to $name database: ${e.message}")
            throw e
        }
    }

    private fun parsePostgreSQLUrl(url: String): Triple<String, String, String> {
        try {
            if (url.startsWith("jdbc:postgresql://")) {
                // Already a JDBC URL, add SSL if needed
                val finalUrl = if (url.contains("sslmode=")) url else "$url?sslmode=require"
                return Triple(finalUrl, "", "")
            }

            if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
                val normalized = if (url.startsWith("postgres://")) url.replaceFirst("postgres://", "postgresql://") else url
                val withoutProtocol = normalized.substring("postgresql://".length)

                val atIndex = withoutProtocol.indexOf('@')
                if (atIndex == -1) {
                    // No credentials in URL
                    val uri = java.net.URI(normalized)
                    val host = uri.host
                    val port = if (uri.port == -1) 5432 else uri.port
                    val database = uri.path.trimStart('/')
                    val jdbcUrl = "jdbc:postgresql://$host:$port/$database?sslmode=require"
                    return Triple(jdbcUrl, "", "")
                } else {
                    // Has credentials
                    val credentials = withoutProtocol.substring(0, atIndex)
                    val hostAndPath = withoutProtocol.substring(atIndex + 1)

                    // Parse credentials
                    val colonIndex = credentials.indexOf(':')
                    val username = if (colonIndex != -1) credentials.substring(0, colonIndex) else credentials
                    val password = if (colonIndex != -1) credentials.substring(colonIndex + 1) else ""

                    // Parse host and database
                    val slashIndex = hostAndPath.indexOf('/')
                    val questionIndex = hostAndPath.indexOf('?')

                    val hostPart = if (slashIndex != -1) hostAndPath.substring(0, slashIndex) else
                                  if (questionIndex != -1) hostAndPath.substring(0, questionIndex) else hostAndPath

                    val colonIndex2 = hostPart.lastIndexOf(':')
                    val host = if (colonIndex2 != -1) hostPart.substring(0, colonIndex2) else hostPart
                    val port = if (colonIndex2 != -1) hostPart.substring(colonIndex2 + 1).toIntOrNull() ?: 5432 else 5432

                    val database = if (slashIndex != -1) {
                        val pathPart = hostAndPath.substring(slashIndex + 1)
                        val queryStart = pathPart.indexOf('?')
                        if (queryStart != -1) pathPart.substring(0, queryStart) else pathPart
                    } else "postgres"

                    val jdbcUrl = "jdbc:postgresql://$host:$port/$database?sslmode=require"
                    return Triple(jdbcUrl, username, password)
                }
            }

            throw IllegalArgumentException("Unsupported URL format: $url")
        } catch (e: Exception) {
            println("Warning: Failed to parse PostgreSQL URL: ${e.message}")
            // Fallback
            val fallbackUrl = if (url.startsWith("jdbc:")) url else "jdbc:postgresql://localhost:5432/postgres"
            return Triple(fallbackUrl, "", "")
        }
    }

    private fun maskCredentials(url: String): String {
        return url.replace(Regex("://([^:]+):([^@]+)@"), "://$1:****@")
    }

    // Debug function to test URL normalization
    fun testUrlNormalization(url: String): String {
        val (jdbcUrl, username, password) = parsePostgreSQLUrl(url)
        return "JDBC URL: $jdbcUrl, User: $username, Password: ${if (password.isNotBlank()) "***" else "not provided"}"
    }


    private fun createTargetSchema() {
        transaction(targetDb) {
            SchemaUtils.createMissingTablesAndColumns(
                AdminUsers,
                Categories,
                ProductsApproved,
                ProductsUnapproved,
                ProductImages,
                Orders,
                AboutUsTable
            )
        }
    }

    suspend fun migrateAllData(): MigrationResult {
        val result = MigrationResult()

        try {
            println("Starting database migration...")

            // Migrate in dependency order
            result.adminUsers = migrateAdminUsers()
            result.categories = migrateCategories()
            result.aboutUs = migrateAboutUs()
            result.productsApproved = migrateProductsApproved()
            result.productsUnapproved = migrateProductsUnapproved()
            result.productImages = migrateProductImages()
            result.orders = migrateOrders()

            result.success = true
            println("Migration completed successfully!")

        } catch (e: Exception) {
            result.success = false
            result.error = e.message
            println("Migration failed: ${e.message}")
            e.printStackTrace()
        }

        return result
    }

    private suspend fun migrateAdminUsers(): Int = withContext(Dispatchers.IO) {
        var count = 0

        // Read from source (using default database connection)
        val sourceData = newSuspendedTransaction {
            AdminUsers.selectAll().map { row ->
                Triple(
                    row[AdminUsers.userId],
                    row[AdminUsers.fireDeviceToken],
                    row[AdminUsers.deviceType]
                )
            }
        }

        // Write to target
        newSuspendedTransaction(db = targetDb) {
            sourceData.forEach { (userId, fireDeviceToken, deviceType) ->
                AdminUsers.insertIgnore {
                    it[AdminUsers.userId] = userId
                    it[AdminUsers.fireDeviceToken] = fireDeviceToken
                    it[AdminUsers.deviceType] = deviceType
                }
                count++
            }
        }

        println("Migrated $count admin users")
        count
    }

    private suspend fun migrateCategories(): Int = withContext(Dispatchers.IO) {
        var count = 0

        val sourceData = newSuspendedTransaction {
            Categories.selectAll().map { row ->
                Pair(
                    row[Categories.id].value,
                    row[Categories.name]
                )
            }
        }

        newSuspendedTransaction(db = targetDb) {
            sourceData.forEach { (id, name) ->
                Categories.insertIgnore {
                    it[Categories.id] = id
                    it[Categories.name] = name
                }
                count++
            }
        }

        println("Migrated $count categories")
        count
    }

    private suspend fun migrateAboutUs(): Int = withContext(Dispatchers.IO) {
        var count = 0

        val sourceData = newSuspendedTransaction {
            AboutUsTable.selectAll().map { row ->
                Pair(
                    row[AboutUsTable.content],
                    row[AboutUsTable.updatedAt]
                )
            }
        }

        newSuspendedTransaction(db = targetDb) {
            sourceData.forEach { (content, updatedAt) ->
                AboutUsTable.insertIgnore {
                    it[AboutUsTable.content] = content
                    it[AboutUsTable.updatedAt] = updatedAt
                }
                count++
            }
        }

        println("Migrated $count about us records")
        count
    }

    private suspend fun migrateProductsApproved(): Int = withContext(Dispatchers.IO) {
        var count = 0

        val sourceData = newSuspendedTransaction {
            ProductsApproved.selectAll().map { row ->
                row.toApprovedProduct()
            }
        }

        newSuspendedTransaction(db = targetDb) {
            sourceData.forEach { product ->
                ProductsApproved.insertIgnore {
                    it[ProductsApproved.id] = product.id
                    it[ProductsApproved.name] = product.name
                    it[ProductsApproved.description] = product.description
                    it[ProductsApproved.price] = product.price
                    it[ProductsApproved.weight] = product.weight
                    it[ProductsApproved.dimension] = product.dimension
                    it[ProductsApproved.purity] = product.purity
                    it[ProductsApproved.maxQuantity] = product.maxQuantity
                    it[ProductsApproved.category] = product.category
                    it[ProductsApproved.margin] = product.margin
                    it[ProductsApproved.multiplier] = product.multiplier
                    it[ProductsApproved.customFields] = product.customFields
                    it[ProductsApproved.createdAt] = product.createdAt
                    it[ProductsApproved.updatedAt] = product.updatedAt
                }
                count++
            }
        }

        println("Migrated $count approved products")
        count
    }

    private suspend fun migrateProductsUnapproved(): Int = withContext(Dispatchers.IO) {
        var count = 0

        val sourceData = newSuspendedTransaction {
            ProductsUnapproved.selectAll().map { row ->
                row.toUnapprovedProduct()
            }
        }

        newSuspendedTransaction(db = targetDb) {
            sourceData.forEach { product ->
                ProductsUnapproved.insertIgnore {
                    it[ProductsUnapproved.id] = product.id
                    it[ProductsUnapproved.name] = product.name
                    it[ProductsUnapproved.description] = product.description
                    it[ProductsUnapproved.price] = product.price
                    it[ProductsUnapproved.weight] = product.weight
                    it[ProductsUnapproved.dimension] = product.dimension
                    it[ProductsUnapproved.purity] = product.purity
                    it[ProductsUnapproved.maxQuantity] = product.maxQuantity
                    it[ProductsUnapproved.category] = product.category
                    it[ProductsUnapproved.margin] = product.margin
                    it[ProductsUnapproved.multiplier] = product.multiplier
                    it[ProductsUnapproved.customFields] = product.customFields
                    it[ProductsUnapproved.createdAt] = product.createdAt
                    it[ProductsUnapproved.updatedAt] = product.updatedAt
                }
                count++
            }
        }

        println("Migrated $count unapproved products")
        count
    }

    private suspend fun migrateProductImages(): Int = withContext(Dispatchers.IO) {
        var count = 0

        val sourceData = newSuspendedTransaction {
            ProductImages.selectAll().map { row ->
                Triple(
                    row[ProductImages.productId],
                    row[ProductImages.image],
                    row[ProductImages.createdAt]
                )
            }
        }

        newSuspendedTransaction(db = targetDb) {
            sourceData.forEach { (productId, image, createdAt) ->
                ProductImages.insertIgnore {
                    it[ProductImages.productId] = productId
                    it[ProductImages.image] = image
                    it[ProductImages.createdAt] = createdAt
                    it[ProductImages.updatedAt] = createdAt
                }
                count++
            }
        }

        println("Migrated $count product images")
        count
    }

    private suspend fun migrateOrders(): Int = withContext(Dispatchers.IO) {
        var count = 0

        val sourceData = newSuspendedTransaction {
            Orders.selectAll().map { row ->
                Order.fromRow(row)
            }
        }

        newSuspendedTransaction(db = targetDb) {
            sourceData.forEach { order ->
                Orders.insertIgnore {
                    it[Orders.id] = order.id
                    it[Orders.productId] = order.productId
                    it[Orders.productName] = order.productName
                    it[Orders.productPrice] = order.productPrice
                    it[Orders.productWeight] = order.productWeight
                    it[Orders.productDimensions] = order.productDimensions
                    it[Orders.status] = order.status
                    it[Orders.createdAt] = order.createdAt
                    it[Orders.productQuantity] = order.productQuantity
                    it[Orders.userMobile] = order.userMobile
                    it[Orders.userName] = order.userName
                    it[Orders.totalAmount] = order.totalAmount
                }
                count++
            }
        }

        println("Migrated $count orders")
        count
    }

    suspend fun verifyMigration(): MigrationVerification = withContext(Dispatchers.IO) {
        val verification = MigrationVerification()

        try {
            // Count records in source (using default database connection)
            val sourceCounts = newSuspendedTransaction {
                mapOf(
                    "admin_users" to AdminUsers.selectAll().count(),
                    "categories" to Categories.selectAll().count(),
                    "about_us" to AboutUsTable.selectAll().count(),
                    "products_approved" to ProductsApproved.selectAll().count(),
                    "products_unapproved" to ProductsUnapproved.selectAll().count(),
                    "product_images" to ProductImages.selectAll().count(),
                    "orders" to Orders.selectAll().count()
                )
            }

            // Count records in target
            val targetCounts = newSuspendedTransaction(db = targetDb) {
                mapOf(
                    "admin_users" to AdminUsers.selectAll().count(),
                    "categories" to Categories.selectAll().count(),
                    "about_us" to AboutUsTable.selectAll().count(),
                    "products_approved" to ProductsApproved.selectAll().count(),
                    "products_unapproved" to ProductsUnapproved.selectAll().count(),
                    "product_images" to ProductImages.selectAll().count(),
                    "orders" to Orders.selectAll().count()
                )
            }

            verification.sourceCounts = sourceCounts
            verification.targetCounts = targetCounts
            verification.success = sourceCounts == targetCounts

            if (verification.success) {
                println("✅ Migration verification passed!")
            } else {
                println("❌ Migration verification failed!")
                println("Source counts: $sourceCounts")
                println("Target counts: $targetCounts")
            }

        } catch (e: Exception) {
            verification.success = false
            verification.error = e.message
            println("Verification failed: ${e.message}")
        }

        verification
    }
}

@kotlinx.serialization.Serializable
data class MigrationResult(
    var success: Boolean = false,
    var error: String? = null,
    var adminUsers: Int = 0,
    var categories: Int = 0,
    var aboutUs: Int = 0,
    var productsApproved: Int = 0,
    var productsUnapproved: Int = 0,
    var productImages: Int = 0,
    var orders: Int = 0
) {
    fun getTotalRecords() = adminUsers + categories + aboutUs + productsApproved +
                           productsUnapproved + productImages + orders
}

@kotlinx.serialization.Serializable
data class MigrationVerification(
    var success: Boolean = false,
    var error: String? = null,
    var sourceCounts: Map<String, Long> = emptyMap(),
    var targetCounts: Map<String, Long> = emptyMap()
)