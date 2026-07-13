package com.notificationbox.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classification_stats")
data class ClassificationStatEntity(
    @PrimaryKey val key: String,
    val count: Long
)
