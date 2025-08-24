package org.cap.gold.repositories

import org.cap.gold.models.Categories
import org.cap.gold.models.Category
import org.cap.gold.models.toCategory
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class CategoryRepository {
    suspend fun getAll(): List<Category> = transaction {
        Categories.selectAll().orderBy(Categories.name to SortOrder.ASC).map { it.toCategory() }
    }

    suspend fun create(name: String): Category = transaction {
        val id = Categories.insertAndGetId { row ->
            row[Categories.name] = name
        }.value
        Category(id = id.toString(), name = name)
    }

    suspend fun delete(id: UUID): Boolean = transaction {
        Categories.deleteWhere { Categories.id eq id } > 0
    }

    suspend fun findByName(name: String): Category? = transaction {
        Categories.select { Categories.name eq name }.map { it.toCategory() }.singleOrNull()
    }
}
