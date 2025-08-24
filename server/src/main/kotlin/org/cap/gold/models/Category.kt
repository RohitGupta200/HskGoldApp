package org.cap.gold.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import java.util.UUID

// Exposed table for categories
object Categories : UUIDTable(name = "categories") {
    val name = varchar("name", 255).uniqueIndex()
}

// Data class used in API payloads and repository returns
@kotlinx.serialization.Serializable
data class Category(
    val id: String,
    val name: String
)

fun ResultRow.toCategory(): Category = Category(
    id = this[Categories.id].value.toString(),
    name = this[Categories.name]
)
