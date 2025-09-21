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
    private val sourceUrl: String,
    private val sourceUser: String,
    private val sourcePassword: String,
    private val targetUrl: String,
    private val targetUser: String,
    private val targetPassword: String
) {
    private lateinit var sourceDb: Database
    private lateinit var targetDb: Database

    suspend fun initializeDatabases() {
        sourceDb = createDatabase(sourceUrl, sourceUser, sourcePassword, "source")
        targetDb = createDatabase(targetUrl, targetUser, targetPassword, "target")

        // Create tables in target database
        createTargetSchema()
    }

    private fun createDatabase(url: String, user: String, password: String, name: String): Database {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = normalizeJdbcUrl(url)
            username = user
            this.password = password
            maximumPoolSize = 5
            connectionTimeout = 30000
            validate()
        }

        val dataSource = HikariDataSource(config)
        return Database.connect(dataSource)
    }

    private fun normalizeJdbcUrl(url: String): String {
        // If already a JDBC URL, fix port if missing
        if (url.startsWith("jdbc:postgresql://")) {
            return addPortIfMissing(url)
        }

        // Convert postgresql:// or postgres:// to jdbc:postgresql://
        val jdbcUrl = when {
            url.startsWith("postgresql://") -> url.replace("postgresql://", "jdbc:postgresql://")
            url.startsWith("postgres://") -> url.replace("postgres://", "jdbc:postgresql://")
            else -> url
        }

        return addPortIfMissing(jdbcUrl)
    }

    private fun addPortIfMissing(jdbcUrl: String): String {
        if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
            return jdbcUrl
        }

        try {
            // Extract the part after jdbc:postgresql://
            val urlPart = jdbcUrl.substring("jdbc:postgresql://".length)

            // Check if it already has a port
            if (urlPart.contains(":") && urlPart.indexOf(":") < urlPart.indexOf("/")) {
                return jdbcUrl // Port already present
            }

            // Split by @ to handle user:password@host/database format
            val parts = urlPart.split("@")
            if (parts.size == 2) {
                val credentials = parts[0]
                val hostAndDb = parts[1]

                // Add port 5432 before the database name
                val dbIndex = hostAndDb.indexOf("/")
                if (dbIndex > 0) {
                    val host = hostAndDb.substring(0, dbIndex)
                    val database = hostAndDb.substring(dbIndex)
                    return "jdbc:postgresql://$credentials@$host:5432$database"
                } else {
                    // No database specified, add port at the end
                    return "jdbc:postgresql://$credentials@$hostAndDb:5432"
                }
            } else {
                // No credentials, just host/database
                val dbIndex = urlPart.indexOf("/")
                if (dbIndex > 0) {
                    val host = urlPart.substring(0, dbIndex)
                    val database = urlPart.substring(dbIndex)
                    return "jdbc:postgresql://$host:5432$database"
                } else {
                    // No database specified
                    return "jdbc:postgresql://$urlPart:5432"
                }
            }
        } catch (e: Exception) {
            println("Warning: Could not parse JDBC URL: $jdbcUrl, using as-is")
            return jdbcUrl
        }
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

        // Read from source
        val sourceData = newSuspendedTransaction(db = sourceDb) {
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

        val sourceData = newSuspendedTransaction(db = sourceDb) {
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

        val sourceData = newSuspendedTransaction(db = sourceDb) {
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

        val sourceData = newSuspendedTransaction(db = sourceDb) {
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

        val sourceData = newSuspendedTransaction(db = sourceDb) {
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

        val sourceData = newSuspendedTransaction(db = sourceDb) {
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

        val sourceData = newSuspendedTransaction(db = sourceDb) {
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
            // Count records in source
            val sourceCounts = newSuspendedTransaction(db = sourceDb) {
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