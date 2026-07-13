package com.notificationbox.app.domain.dryrun

import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem

/**
 * Phase 1 modes that cannot operate on Android notifications.
 *
 * This type is intentionally independent from the persisted legacy [com.notificationbox.app.model.AppMode]
 * so introducing dry-run does not reinterpret an existing enabled value.
 */
enum class OrganizationMode {
    OBSERVE_ONLY,
    DRY_RUN
}

/**
 * App-only preview outcomes. None of these values represents an Android notification operation.
 */
enum class PlannedAction {
    KEEP_IN_CURRENT_VIEW,
    ADD_TO_DIGEST_PREVIEW,
    EXCLUDE_FROM_DIGEST_PREVIEW
}

/**
 * Minimal, non-content representation of what dry-run would do inside the application.
 *
 * Notification title, text, application label, extras, and style payloads are deliberately absent.
 */
data class PlannedNotificationAction(
    val notificationKey: String,
    val packageName: String,
    val selectedDecision: NotificationDecision,
    val decisionSource: DecisionSource,
    val plannedAction: PlannedAction
)

data class DryRunPreview(
    val mode: OrganizationMode,
    val activeNotificationCount: Int,
    val plannedActions: List<PlannedNotificationAction>,
    val countsByAction: Map<PlannedAction, Int>
) {
    companion object {
        fun observeOnly(activeNotificationCount: Int): DryRunPreview =
            DryRunPreview(
                mode = OrganizationMode.OBSERVE_ONLY,
                activeNotificationCount = activeNotificationCount,
                plannedActions = emptyList(),
                countsByAction = PlannedAction.entries.associateWith { 0 }
            )
    }
}

/**
 * Pure Phase 1 planner. It consumes the decision already resolved by the repository
 * (`manual > app rule > automatic`) and never reaches an Android service or manager.
 */
class DryRunPlanner {
    fun plan(
        mode: OrganizationMode,
        notifications: List<NotificationItem>
    ): DryRunPreview {
        val activeNotifications = notifications.filter(NotificationItem::isActive)

        if (mode == OrganizationMode.OBSERVE_ONLY) {
            return DryRunPreview.observeOnly(activeNotifications.size)
        }

        val plannedActions = activeNotifications.map { notification ->
            PlannedNotificationAction(
                notificationKey = notification.key,
                packageName = notification.packageName,
                selectedDecision = notification.category,
                decisionSource = notification.decisionSource,
                plannedAction = notification.category.toPlannedAction()
            )
        }

        return DryRunPreview(
            mode = OrganizationMode.DRY_RUN,
            activeNotificationCount = activeNotifications.size,
            plannedActions = plannedActions,
            countsByAction = PlannedAction.entries.associateWith { action ->
                plannedActions.count { it.plannedAction == action }
            }
        )
    }
}

private fun NotificationDecision.toPlannedAction(): PlannedAction =
    when (this) {
        NotificationDecision.KeepNow -> PlannedAction.KEEP_IN_CURRENT_VIEW
        NotificationDecision.HoldForDigest -> PlannedAction.ADD_TO_DIGEST_PREVIEW
        NotificationDecision.Ignore -> PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW
    }
