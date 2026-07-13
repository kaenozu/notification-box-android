package com.notificationbox.app.domain.dryrun

import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DryRunPlannerTest {
    private val planner = DryRunPlanner()

    @Test
    fun observeOnlyReportsCandidatesWithoutPlanningActions() {
        val preview = planner.plan(
            mode = OrganizationMode.OBSERVE_ONLY,
            notifications = listOf(
                notification(key = "active", isActive = true),
                notification(key = "removed", isActive = false)
            )
        )

        assertEquals(OrganizationMode.OBSERVE_ONLY, preview.mode)
        assertEquals(1, preview.activeNotificationCount)
        assertTrue(preview.plannedActions.isEmpty())
        assertEquals(
            PlannedAction.entries.associateWith { 0 },
            preview.countsByAction
        )
    }

    @Test
    fun dryRunMapsResolvedDecisionsWithoutOperatingOnNotifications() {
        val preview = planner.plan(
            mode = OrganizationMode.DRY_RUN,
            notifications = listOf(
                notification(
                    key = "manual",
                    decision = NotificationDecision.KeepNow,
                    source = DecisionSource.UserOverride
                ),
                notification(
                    key = "rule",
                    decision = NotificationDecision.HoldForDigest,
                    source = DecisionSource.AppRule
                ),
                notification(
                    key = "automatic",
                    decision = NotificationDecision.Ignore,
                    source = DecisionSource.Automatic
                ),
                notification(
                    key = "inactive",
                    decision = NotificationDecision.KeepNow,
                    source = DecisionSource.Automatic,
                    isActive = false
                )
            )
        )

        assertEquals(OrganizationMode.DRY_RUN, preview.mode)
        assertEquals(3, preview.activeNotificationCount)
        assertEquals(
            listOf(
                PlannedNotificationAction(
                    candidateId = "candidate-1",
                    packageName = "com.example.manual",
                    selectedDecision = NotificationDecision.KeepNow,
                    decisionSource = DecisionSource.UserOverride,
                    plannedAction = PlannedAction.KEEP_IN_CURRENT_VIEW
                ),
                PlannedNotificationAction(
                    candidateId = "candidate-2",
                    packageName = "com.example.rule",
                    selectedDecision = NotificationDecision.HoldForDigest,
                    decisionSource = DecisionSource.AppRule,
                    plannedAction = PlannedAction.ADD_TO_DIGEST_PREVIEW
                ),
                PlannedNotificationAction(
                    candidateId = "candidate-3",
                    packageName = "com.example.automatic",
                    selectedDecision = NotificationDecision.Ignore,
                    decisionSource = DecisionSource.Automatic,
                    plannedAction = PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW
                )
            ),
            preview.plannedActions
        )
        assertEquals(1, preview.countsByAction[PlannedAction.KEEP_IN_CURRENT_VIEW])
        assertEquals(1, preview.countsByAction[PlannedAction.ADD_TO_DIGEST_PREVIEW])
        assertEquals(1, preview.countsByAction[PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW])
        assertEquals(
            listOf("candidate-1", "candidate-2", "candidate-3"),
            preview.plannedActions.map(PlannedNotificationAction::candidateId)
        )
    }

    @Test
    fun plannedActionsDoNotExposeNotificationContentOrRawKey() {
        val title = "private-title-sentinel"
        val text = "private-body-sentinel"
        val rawKey = "raw-notification-key-sentinel"
        val preview = planner.plan(
            mode = OrganizationMode.DRY_RUN,
            notifications = listOf(
                notification(
                    key = rawKey,
                    packageName = "com.example.private",
                    title = title,
                    text = text
                )
            )
        )

        val rendered = preview.toString()
        assertFalse(rendered.contains(title))
        assertFalse(rendered.contains(text))
        assertFalse(rendered.contains(rawKey))

        val fields = PlannedNotificationAction::class.java.declaredFields
            .map { it.name }
            .filterNot { it.startsWith("$") }
            .toSet()
        val sensitiveFields = setOf(
            "key",
            "notificationKey",
            "rawKey",
            "osKey",
            "title",
            "text",
            "appLabel",
            "automaticReason",
            "reason",
            "extras",
            "messagingStyle",
            "ticker"
        )
        val exposedSensitiveFields = fields.intersect(sensitiveFields)

        assertTrue(
            "PlannedNotificationAction exposes sensitive fields: $exposedSensitiveFields",
            exposedSensitiveFields.isEmpty()
        )
    }

    @Test
    fun candidateIdentifiersArePreviewScopedAndIndependentOfRawKeys() {
        val firstRawKeys = listOf("first-raw-key", "second-raw-key")
        val secondRawKeys = listOf("different-raw-key", "another-raw-key")

        val firstPreview = planner.plan(
            mode = OrganizationMode.DRY_RUN,
            notifications = firstRawKeys.map(::notification)
        )
        val secondPreview = planner.plan(
            mode = OrganizationMode.DRY_RUN,
            notifications = secondRawKeys.map(::notification)
        )

        val expectedCandidateIds = listOf("candidate-1", "candidate-2")
        assertEquals(
            expectedCandidateIds,
            firstPreview.plannedActions.map(PlannedNotificationAction::candidateId)
        )
        assertEquals(
            expectedCandidateIds,
            secondPreview.plannedActions.map(PlannedNotificationAction::candidateId)
        )
        (firstRawKeys + secondRawKeys).forEach { rawKey ->
            assertFalse(firstPreview.toString().contains(rawKey))
            assertFalse(secondPreview.toString().contains(rawKey))
        }
    }

    private fun notification(
        key: String,
        packageName: String = "com.example.$key",
        decision: NotificationDecision = NotificationDecision.KeepNow,
        source: DecisionSource = DecisionSource.Automatic,
        isActive: Boolean = true,
        title: String? = "title-$key",
        text: String? = "text-$key"
    ): NotificationItem =
        NotificationItem(
            key = key,
            packageName = packageName,
            appLabel = "Example",
            title = title,
            text = text,
            postTime = Instant.ofEpochMilli(1_000),
            automaticDecision = decision,
            userDecision = if (source == DecisionSource.UserOverride) decision else null,
            appRuleDecision = if (source == DecisionSource.AppRule) decision else null,
            category = decision,
            decisionSource = source,
            automaticReason = "automatic-reason",
            reason = "resolved-reason",
            userPinned = false,
            isActive = isActive,
            removedAt = if (isActive) null else Instant.ofEpochMilli(2_000)
        )
}
