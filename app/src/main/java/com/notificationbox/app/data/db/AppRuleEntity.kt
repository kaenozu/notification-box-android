package com.notificationbox.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val decision: String,
    val updatedAtMillis: Long
)
