package com.notificationbox.app.model

import java.time.Instant

enum class NotificationDecision {
    KeepNow,
    HoldForDigest,
    Ignore
}

enum class AppMode {
    Observation,
    Active
}

data class NotificationItem(
    val id: Long,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postTime: Instant,
    val category: NotificationDecision,
    val reason: String,
    val userPinned: Boolean = false
)

data class DigestSchedule(
    val hours: List<Int> = listOf(8, 12, 18, 21)
)

data class AppState(
    val mode: AppMode = AppMode.Observation,
    val notificationAccessGranted: Boolean = false,
    val postNotificationsGranted: Boolean = false,
    val digestSchedule: DigestSchedule = DigestSchedule(),
    val pausedUntilText: String = "解除まで",
    val items: List<NotificationItem> = emptyList(),
    val selectedFilter: NotificationDecision? = null
)
