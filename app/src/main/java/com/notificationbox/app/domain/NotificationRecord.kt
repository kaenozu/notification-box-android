package com.notificationbox.app.domain

import com.notificationbox.app.model.NotificationDecision

data class NotificationRecord(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postTimeMillis: Long,
    val notificationId: Int,
    val tag: String?,
    val channelId: String?,
    val category: NotificationDecision,
    val reason: String,
    val userPinned: Boolean,
    val isActive: Boolean,
    val removedAtMillis: Long?
)
