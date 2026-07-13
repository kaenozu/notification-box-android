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
        assertEquals(PlannedAction.entries.associateWith { 0 }, preview.countsByAction)
    }

    @Test
    fun dryRunMapsResolvedDecisionsWithoutOperatingOnNotifications() {
        val preview = planner.plan(
            mode = OrganizationMode.DRY_RUN,
            notifications = listOf(
                notification("manual", decision = NotificationDecision.KeepNow, source = DecisionSource.UserOverride),
                notification("rule", decision = NotificationDecision.HoldForDigest, source = DecisionSource.AppRule),
                notification("automatic", decision = NotificationDecision.Ignore),
                notification("inactive", isActive = false)
            )
        )
        assertEquals(OrganizationMode.DRY_RUN, preview.mode)
        assertEquals(3, preview.activeNotificationCount)
        assertEquals(
            listOf(
                PlannedNotificationAction("candidate-1", "com.example.manual", NotificationDecision.KeepNow, DecisionSource.UserOverride, PlannedAction.KEEP_IN_CURRENT_VIEW),
                PlannedNotificationAction("candidate-2", "com.example.rule", NotificationDecision.HoldForDigest, DecisionSource.AppRule, PlannedAction.ADD_TO_DIGEST_PREVIEW),
                PlannedNotificationAction("candidate-3", "com.example.automatic", NotificationDecision.Ignore, DecisionSource.Automatic, PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW)
            ),
            preview.plannedActions
        )
        assertEquals(1, preview.countsByAction[PlannedAction.KEEP_IN_CURRENT_VIEW])
        assertEquals(1, preview.countsByAction[PlannedAction.ADD_TO_DIGEST_PREVIEW])
        assertEquals(1, preview.countsByAction[PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW])
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
            "key", "notificationKey", "rawKey", "osKey", "title", "text",
            "appLabel", "automaticReason", "reason", "extras", "messagingStyle", "ticker"
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
            OrganizationMode.DRY_RUN,
            firstRawKeys.mapIndexed { index, rawKey ->
                notification(rawKey, packageName = "com.example.first.$index")
            }
        )
        val secondPreview = planner.plan(
            OrganizationMode.DRY_RUN,
            secondRawKeys.mapIndexed { index, rawKey ->
                notification(rawKey, packageName = "com.example.second.$index")
            }
        )
        val expectedCandidateIds = listOf("candidate-1", "candidate-2")
        assertEquals(expectedCandidateIds, firstPreview.plannedActions.map(PlannedNotificationAction::candidateId))
        assertEquals(expectedCandidateIds, secondPreview.plannedActions.map(PlannedNotificationAction::candidateId))
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
    ): NotificationItem = NotificationItem(
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
