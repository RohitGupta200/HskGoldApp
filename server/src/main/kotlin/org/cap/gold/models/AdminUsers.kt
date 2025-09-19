package org.cap.gold.models

import org.jetbrains.exposed.sql.Table

// Table: admin_users
// Single column primary key: user_id (stores Firebase UID or app user id)
object AdminUsers : Table(name = "Admin_users") {
    val userId = varchar("userId", 128)
    val fireDeviceToken = varchar("Fire_device_token", 256).nullable()
    val deviceType = varchar("deviceType", 16).default("android")
    override val primaryKey = PrimaryKey(userId)
}
