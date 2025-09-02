package org.cap.gold.repositories

import org.cap.gold.models.AboutUsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class AboutUsRepository {
    // Ensure at least one row exists (caller must be inside a transaction)
    private fun ensureRow() {
        val exists = AboutUsTable.selectAll().limit(1).empty().not()
        if (!exists) {
            AboutUsTable.insert {
                it[content] = ""
                it[updatedAt] = LocalDateTime.now()
            }
        }
    }

    suspend fun getContent(): String = transaction {
        ensureRow()
        AboutUsTable
            .slice(AboutUsTable.content)
            .selectAll()
            .limit(1)
            .single()[AboutUsTable.content]
    }

    suspend fun setContent(newContent: String) = transaction {
        val anyRowExists = AboutUsTable.selectAll().limit(1).empty().not()
        if (anyRowExists) {
            // Update all rows (there should be only one)
            AboutUsTable.update({ org.jetbrains.exposed.sql.Op.TRUE }) {
                it[content] = newContent
                it[updatedAt] = LocalDateTime.now()
            }
        } else {
            AboutUsTable.insert {
                it[content] = newContent
                it[updatedAt] = LocalDateTime.now()
            }
        }
    }
}
