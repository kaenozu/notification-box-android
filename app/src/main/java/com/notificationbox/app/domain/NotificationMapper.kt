package com.notificationbox.app.domain

import com.notificationbox.app.data.NotificationEntity
import com.notificationbox.app.model.NotificationDecision

fun NotificationEntity.toRecord(): NotificationRecord = NotificationRecord(
    key = key,
    packageName = packageName,
    appLabel = appLabel,
    title = title,
    text = text,
    postTimeMillis = postTimeMillis,
    notificationId = notificationId,
    tag = tag,
    channelId = channelId,
    category = parseCategory(category),
    reason = reason,
    userPinned = userPinned,
    isActive = isActive,
    removedAtMillis = removedAtMillis
)

fun NotificationRecord.toEntity(): NotificationEntity = NotificationEntity(
    key = key,
    packageName = packageName,
    appLabel = appLabel,
    title = title,
    text = text,
    postTimeMillis = postTimeMillis,
    notificationId = notificationId,
    tag = tag,
    channelId = channelId,
    category = category.name,
    reason = reason,
    userPinned = userPinned,
    isActive = isActive,
    removedAtMillis = removedAtMillis
)

fun parseCategory(value: String?): NotificationDecision {
    if (value == null) return NotificationDecision.HoldForDigest
    return try {
        NotificationDecision.valueOf(value)
    } catch (_: IllegalArgumentException) {
        NotificationDecision.HoldForDigest
    }
}
