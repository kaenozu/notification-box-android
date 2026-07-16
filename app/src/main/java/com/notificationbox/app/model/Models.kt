package com.notificationbox.app.model

import java.time.Instant

enum class NotificationDecision {
    KeepNow,
    HoldForDigest,
    Ignore
}

enum class DecisionSource {
    Automatic,
    AppRule,
    UserOverride
}

enum class AppMode {
    Observation,
    Active
}

enum class NotificationContentAvailability {
    AVAILABLE,
    EMPTY,
    REDACTED_OR_UNAVAILABLE
}

data class NotificationItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postTime: Instant,
    val automaticDecision: NotificationDecision,
    val userDecision: NotificationDecision?,
    val appRuleDecision: NotificationDecision?,
    val category: NotificationDecision,
    val decisionSource: DecisionSource,
    val automaticReason: String,
    val reason: String,
    val contentAvailability: NotificationContentAvailability =
        NotificationContentAvailability.AVAILABLE,
    val userPinned: Boolean = false,
    val isActive: Boolean = true,
    val removedAt: Instant? = null
)

data class AppRule(
    val packageName: String,
    val appLabel: String,
    val decision: NotificationDecision,
    val updatedAt: Instant
)

data class ClassificationStats(
    val automaticallyClassified: Long = 0,
    val userOverrideChanges: Long = 0,
    val appRuleChanges: Long = 0,
    val automaticByDecision: Map<NotificationDecision, Long> = emptyMap(),
    val selectedByDecision: Map<NotificationDecision, Long> = emptyMap(),
    val appChangeCounts: Map<String, Long> = emptyMap()
)

data class DigestSchedule(
    val hours: List<Int> = listOf(8, 12, 18, 21)
)

data class AppState(
    val mode: AppMode = AppMode.Observation,
    val preferencesLoaded: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val notificationAccessGranted: Boolean = false,
    val digestSchedule: DigestSchedule = DigestSchedule(),
    val pausedUntilText: String = "解除まで",
    val items: List<NotificationItem> = emptyList(),
    val appRules: List<AppRule> = emptyList(),
    val classificationStats: ClassificationStats = ClassificationStats(),
    val ingestionHealth: NotificationIngestionHealth = NotificationIngestionHealth(),
    val selectedFilter: NotificationDecision? = null
)
