package com.notificationbox.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["postTimeMillis"]),
        Index(value = ["userPinned"]),
        Index(value = ["isActive"])
    ]
)
data class NotificationEntity(
    @PrimaryKey val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postTimeMillis: Long,
    val notificationId: Int,
    val tag: String?,
    val channelId: String?,
    val category: String,
    val reason: String,
    val userPinned: Boolean,
    val isActive: Boolean,
    val removedAtMillis: Long?
)
