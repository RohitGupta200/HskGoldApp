package org.cap.gold.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object AboutUsTable : Table("about_us") {
    // Single-row table without an id; we will always update-all or insert-one
    val content = text("content").default("")
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}
