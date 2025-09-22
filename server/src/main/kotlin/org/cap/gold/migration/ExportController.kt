package org.cap.gold.migration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cap.gold.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Serializable
data class ExportData(
    val adminUsers: List<AdminUserExport>,
    val categories: List<CategoryExport>,
    val aboutUs: List<AboutUsExport>,
    val productsApproved: List<ProductExport>,
    val productsUnapproved: List<ProductExport>,
    val productImages: List<ProductImageExport>,
    val orders: List<OrderExport>,
    val exportTimestamp: String
)

@Serializable
data class AdminUserExport(
    val userId: String,
    val fireDeviceToken: String?,
    val deviceType: String
)

@Serializable
data class CategoryExport(
    val id: String,
    val name: String
)

@Serializable
data class AboutUsExport(
    val content: String,
    val updatedAt: String
)

@Serializable
data class ProductExport(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val weight: String,
    val dimension: String,
    val purity: String,
    val maxQuantity: Int,
    val category: String,
    val margin: Double,
    val multiplier: Double,
    val customFields: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ProductImageExport(
    val productId: String,
    val imageBase64: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class OrderExport(
    val id: String,
    val productId: String,
    val productName: String,
    val productPrice: Double,
    val productWeight: Double,
    val productDimensions: String,
    val status: String,
    val createdAt: Long,
    val productQuantity: Int,
    val userMobile: String,
    val userName: String,
    val totalAmount: Double
)

fun Route.exportRoutes() {
    route("/admin/export") {

        // Export all data as JSON
        get("/json") {
            try {
                val exportData = newSuspendedTransaction {
                    val adminUsers = AdminUsers.selectAll().map { row ->
                        AdminUserExport(
                            userId = row[AdminUsers.userId],
                            fireDeviceToken = row[AdminUsers.fireDeviceToken],
                            deviceType = row[AdminUsers.deviceType]
                        )
                    }

                    val categories = Categories.selectAll().map { row ->
                        CategoryExport(
                            id = row[Categories.id].value.toString(),
                            name = row[Categories.name]
                        )
                    }

                    val aboutUs = AboutUsTable.selectAll().map { row ->
                        AboutUsExport(
                            content = row[AboutUsTable.content],
                            updatedAt = row[AboutUsTable.updatedAt].toString()
                        )
                    }

                    val productsApproved = ProductsApproved.selectAll().map { row ->
                        ProductExport(
                            id = row[ProductsApproved.id].value.toString(),
                            name = row[ProductsApproved.name],
                            description = row[ProductsApproved.description],
                            price = row[ProductsApproved.price],
                            weight = row[ProductsApproved.weight],
                            dimension = row[ProductsApproved.dimension],
                            purity = row[ProductsApproved.purity],
                            maxQuantity = row[ProductsApproved.maxQuantity],
                            category = row[ProductsApproved.category],
                            margin = row[ProductsApproved.margin],
                            multiplier = row[ProductsApproved.multiplier],
                            customFields = row[ProductsApproved.customFields],
                            createdAt = row[ProductsApproved.createdAt].toString(),
                            updatedAt = row[ProductsApproved.updatedAt].toString()
                        )
                    }

                    val productsUnapproved = ProductsUnapproved.selectAll().map { row ->
                        ProductExport(
                            id = row[ProductsUnapproved.id].value.toString(),
                            name = row[ProductsUnapproved.name],
                            description = row[ProductsUnapproved.description],
                            price = row[ProductsUnapproved.price],
                            weight = row[ProductsUnapproved.weight],
                            dimension = row[ProductsUnapproved.dimension],
                            purity = row[ProductsUnapproved.purity],
                            maxQuantity = row[ProductsUnapproved.maxQuantity],
                            category = row[ProductsUnapproved.category],
                            margin = row[ProductsUnapproved.margin],
                            multiplier = row[ProductsUnapproved.multiplier],
                            customFields = row[ProductsUnapproved.customFields],
                            createdAt = row[ProductsUnapproved.createdAt].toString(),
                            updatedAt = row[ProductsUnapproved.updatedAt].toString()
                        )
                    }

                    val productImages = ProductImages.selectAll().map { row ->
                        ProductImageExport(
                            productId = row[ProductImages.productId].toString(),
                            imageBase64 = Base64.getEncoder().encodeToString(row[ProductImages.image].bytes),
                            createdAt = row[ProductImages.createdAt].toString(),
                            updatedAt = row[ProductImages.updatedAt].toString()
                        )
                    }

                    val orders = Orders.selectAll().map { row ->
                        OrderExport(
                            id = row[Orders.id].value.toString(),
                            productId = row[Orders.productId].toString(),
                            productName = row[Orders.productName],
                            productPrice = row[Orders.productPrice],
                            productWeight = row[Orders.productWeight],
                            productDimensions = row[Orders.productDimensions],
                            status = row[Orders.status].toString(),
                            createdAt = row[Orders.createdAt],
                            productQuantity = row[Orders.productQuantity],
                            userMobile = row[Orders.userMobile],
                            userName = row[Orders.userName],
                            totalAmount = row[Orders.totalAmount]
                        )
                    }

                    ExportData(
                        adminUsers = adminUsers,
                        categories = categories,
                        aboutUs = aboutUs,
                        productsApproved = productsApproved,
                        productsUnapproved = productsUnapproved,
                        productImages = productImages,
                        orders = orders,
                        exportTimestamp = LocalDateTime.now().toString()
                    )
                }

                val jsonString = Json.encodeToString(exportData)
                call.respondText(jsonString, ContentType.Application.Json)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to e.message,
                    "message" to "Export failed"
                ))
            }
        }

        // Export table schemas (CREATE statements)
        get("/schema") {
            try {
                val schemaDump = StringBuilder()

                schemaDump.append("-- CapGold Database Schema Export\n")
                schemaDump.append("-- Generated: ${LocalDateTime.now()}\n\n")

                // Admin Users table
                schemaDump.append("""
-- Admin Users Table
CREATE TABLE IF NOT EXISTS "Admin_users" (
    "userId" VARCHAR(255) PRIMARY KEY,
    "Fire_device_token" VARCHAR(255),
    "deviceType" VARCHAR(16) DEFAULT 'android'
);

""")

                // Categories table
                schemaDump.append("""
-- Categories Table
CREATE TABLE IF NOT EXISTS "categories" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "name" VARCHAR(255) NOT NULL
);

""")

                // About Us table
                schemaDump.append("""
-- About Us Table
CREATE TABLE IF NOT EXISTS "about_us" (
    "content" TEXT NOT NULL,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

""")

                // Products Approved table
                schemaDump.append("""
-- Products Approved Table
CREATE TABLE IF NOT EXISTS "products_approved" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "name" VARCHAR(200) NOT NULL,
    "description" VARCHAR(1000) NOT NULL,
    "price" DECIMAL(10,2) NOT NULL,
    "weight" VARCHAR(50) NOT NULL,
    "dimension" VARCHAR(255) NOT NULL,
    "purity" VARCHAR(255) NOT NULL,
    "max_quantity" INTEGER NOT NULL,
    "category" VARCHAR(255) NOT NULL,
    "margin" DECIMAL(10,2) NOT NULL,
    "multiplier" DECIMAL(10,2) NOT NULL,
    "custom_fields" TEXT NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

""")

                // Products Unapproved table
                schemaDump.append("""
-- Products Unapproved Table
CREATE TABLE IF NOT EXISTS "products_unapproved" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "name" VARCHAR(200) NOT NULL,
    "description" VARCHAR(1000) NOT NULL,
    "price" DECIMAL(10,2) NOT NULL,
    "weight" VARCHAR(50) NOT NULL,
    "dimension" VARCHAR(255) NOT NULL,
    "purity" VARCHAR(255) NOT NULL,
    "max_quantity" INTEGER NOT NULL,
    "category" VARCHAR(255) NOT NULL,
    "margin" DECIMAL(10,2) NOT NULL,
    "multiplier" DECIMAL(10,2) NOT NULL,
    "custom_fields" TEXT NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

""")

                // Product Images table
                schemaDump.append("""
-- Product Images Table
CREATE TABLE IF NOT EXISTS "product_images" (
    "product_id" UUID NOT NULL,
    "image" BYTEA NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

""")

                // Orders table
                schemaDump.append("""
-- Orders Table
CREATE TABLE IF NOT EXISTS "orders" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "product_id" UUID NOT NULL,
    "product_name" VARCHAR(255) NOT NULL,
    "product_price" DECIMAL(10,2) NOT NULL,
    "product_weight" DECIMAL(10,2) NOT NULL,
    "product_dimensions" VARCHAR(255) NOT NULL,
    "status" VARCHAR(50) NOT NULL,
    "created_at" BIGINT NOT NULL,
    "product_quantity" INTEGER NOT NULL,
    "user_mobile" VARCHAR(20) NOT NULL,
    "user_name" VARCHAR(255) NOT NULL,
    "total_amount" DECIMAL(10,2) NOT NULL
);

""")

                call.respondText(schemaDump.toString(), ContentType.Text.Plain)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to e.message,
                    "message" to "Schema export failed"
                ))
            }
        }

        // Export as SQL dump for direct Supabase import
        get("/sql") {
            try {
                val sqlDump = StringBuilder()

                sqlDump.append("-- CapGold Database Export\n")
                sqlDump.append("-- Generated: ${LocalDateTime.now()}\n\n")

                newSuspendedTransaction {
                    // Export AdminUsers
                    sqlDump.append("-- Admin Users\n")
                    AdminUsers.selectAll().forEach { row ->
                        val userId = row[AdminUsers.userId].replace("'", "''")
                        val deviceToken = row[AdminUsers.fireDeviceToken]?.replace("'", "''") ?: ""
                        val deviceType = row[AdminUsers.deviceType].replace("'", "''")

                        sqlDump.append("INSERT INTO \"Admin_users\" (\"userId\", \"Fire_device_token\", \"deviceType\") VALUES ")
                        sqlDump.append("('$userId', ")
                        if (deviceToken.isNotEmpty()) {
                            sqlDump.append("'$deviceToken', ")
                        } else {
                            sqlDump.append("NULL, ")
                        }
                        sqlDump.append("'$deviceType');\n")
                    }

                    // Export Categories
                    sqlDump.append("\n-- Categories\n")
                    Categories.selectAll().forEach { row ->
                        val id = row[Categories.id].value.toString()
                        val name = row[Categories.name].replace("'", "''")

                        sqlDump.append("INSERT INTO \"categories\" (\"id\", \"name\") VALUES ")
                        sqlDump.append("('$id', '$name');\n")
                    }

                    // Export AboutUs
                    sqlDump.append("\n-- About Us\n")
                    AboutUsTable.selectAll().forEach { row ->
                        val content = row[AboutUsTable.content].replace("'", "''")
                        val updatedAt = row[AboutUsTable.updatedAt].toString()

                        sqlDump.append("INSERT INTO \"about_us\" (\"content\", \"updated_at\") VALUES ")
                        sqlDump.append("('$content', '$updatedAt');\n")
                    }

                    // Export ProductsApproved
                    sqlDump.append("\n-- Products Approved\n")
                    ProductsApproved.selectAll().forEach { row ->
                        val id = row[ProductsApproved.id].value.toString()
                        val name = row[ProductsApproved.name].replace("'", "''")
                        val description = row[ProductsApproved.description].replace("'", "''")
                        val customFields = row[ProductsApproved.customFields].replace("'", "''")

                        sqlDump.append("INSERT INTO \"products_approved\" (\"id\", \"name\", \"description\", \"price\", \"weight\", \"dimension\", \"purity\", \"max_quantity\", \"category\", \"margin\", \"multiplier\", \"custom_fields\", \"created_at\", \"updated_at\") VALUES ")
                        sqlDump.append("('$id', '$name', '$description', ${row[ProductsApproved.price]}, '${row[ProductsApproved.weight]}', '${row[ProductsApproved.dimension]}', '${row[ProductsApproved.purity]}', ${row[ProductsApproved.maxQuantity]}, '${row[ProductsApproved.category]}', ${row[ProductsApproved.margin]}, ${row[ProductsApproved.multiplier]}, '$customFields', '${row[ProductsApproved.createdAt]}', '${row[ProductsApproved.updatedAt]}');\n")
                    }

                    sqlDump.append("\n-- Products UnApproved\n")
                    ProductsUnapproved.selectAll().forEach { row ->
                        val id = row[ProductsUnapproved.id].value.toString()
                        val name = row[ProductsUnapproved.name].replace("'", "''")
                        val description = row[ProductsUnapproved.description].replace("'", "''")
                        val customFields = row[ProductsUnapproved.customFields].replace("'", "''")

                        sqlDump.append("INSERT INTO \"products_unapproved\" (\"id\", \"name\", \"description\", \"price\", \"weight\", \"dimension\", \"purity\", \"max_quantity\", \"category\", \"margin\", \"multiplier\", \"custom_fields\", \"created_at\", \"updated_at\") VALUES ")
                        sqlDump.append("('$id', '$name', '$description', ${row[ProductsUnapproved.price]}, '${row[ProductsUnapproved.weight]}', '${row[ProductsUnapproved.dimension]}', '${row[ProductsUnapproved.purity]}', ${row[ProductsUnapproved.maxQuantity]}, '${row[ProductsUnapproved.category]}', ${row[ProductsUnapproved.margin]}, ${row[ProductsUnapproved.multiplier]}, '$customFields', '${row[ProductsUnapproved.createdAt]}', '${row[ProductsUnapproved.updatedAt]}');\n")
                    }

                    // Export Orders
                    sqlDump.append("\n-- Orders\n")
                    Orders.selectAll().forEach { row ->
                        val id = row[Orders.id].value.toString()
                        val productId = row[Orders.productId].toString()
                        val productName = row[Orders.productName].replace("'", "''")
                        val userName = row[Orders.userName].replace("'", "''")

                        sqlDump.append("INSERT INTO \"orders\" (\"id\", \"product_id\", \"product_name\", \"product_price\", \"product_weight\", \"product_dimensions\", \"status\", \"created_at\", \"product_quantity\", \"user_mobile\", \"user_name\", \"total_amount\") VALUES ")
                        sqlDump.append("('$id', '$productId', '$productName', ${row[Orders.productPrice]}, ${row[Orders.productWeight]}, '${row[Orders.productDimensions]}', '${row[Orders.status]}', ${row[Orders.createdAt]}, ${row[Orders.productQuantity]}, '${row[Orders.userMobile]}', '$userName', ${row[Orders.totalAmount]});\n")
                    }
                }

                call.respondText(sqlDump.toString(), ContentType.Text.Plain)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to e.message,
                    "message" to "SQL export failed"
                ))
            }
        }

        // Get export summary
        get("/summary") {
            try {
                val summary = newSuspendedTransaction {
                    mapOf(
                        "adminUsers" to AdminUsers.selectAll().count(),
                        "categories" to Categories.selectAll().count(),
                        "aboutUs" to AboutUsTable.selectAll().count(),
                        "productsApproved" to ProductsApproved.selectAll().count(),
                        "productsUnapproved" to ProductsUnapproved.selectAll().count(),
                        "productImages" to ProductImages.selectAll().count(),
                        "orders" to Orders.selectAll().count(),
                        "timestamp" to LocalDateTime.now().toString()
                    )
                }

                call.respond(summary)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to e.message,
                    "message" to "Summary failed"
                ))
            }
        }
    }
}