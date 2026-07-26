package com.notificationbox.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_events",
    indices = [
        Index(value = ["occurredAtMillis"]),
        Index(value = ["packageName"]),
        Index(value = ["transactionType"])
    ]
)
data class PaymentEventEntity(
    @PrimaryKey val sourceNotificationKey: String,
    val packageName: String,
    val appLabel: String,
    val amountYen: Long,
    val merchantName: String?,
    val transactionType: String,
    val occurredAtMillis: Long,
    val parserId: String,
    val parserVersion: Int,
    val confidencePercent: Int,
    @ColumnInfo(defaultValue = "'UNREVIEWED'")
    val status: String = "UNREVIEWED"
)
