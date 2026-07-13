package com.notificationbox.app.domain.dryrun

import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DryRunStatisticsTest {
    @Test
    fun `statistics aggregate applications sources and actions without identifiers`() {
        val preview = DryRunPreview(
            mode = OrganizationMode.DRY_RUN,
            activeNotificationCount = 3,
            plannedActions = listOf(
                action("candidate-1", "com.example.mail", DecisionSource.Automatic, PlannedAction.KEEP_IN_CURRENT_VIEW),
                action("candidate-2", "com.example.mail", DecisionSource.UserOverride, PlannedAction.ADD_TO_DIGEST_PREVIEW),
                action("candidate-3", "com.example.chat", DecisionSource.AppRule, PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW)
            ),
            countsByAction = mapOf(
                PlannedAction.KEEP_IN_CURRENT_VIEW to 1,
                PlannedAction.ADD_TO_DIGEST_PREVIEW to 1,
                PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW to 1
            )
        )

        val statistics = preview.toStatistics()

        assertEquals(3, statistics.activeCandidateCount)
        assertEquals(2, statistics.uniqueApplicationCount)
        assertEquals(1, statistics.countsByDecisionSource[DecisionSource.Automatic])
        assertEquals(1, statistics.countsByDecisionSource[DecisionSource.AppRule])
        assertEquals(1, statistics.countsByDecisionSource[DecisionSource.UserOverride])
        assertEquals(1, statistics.countsByAction[PlannedAction.KEEP_IN_CURRENT_VIEW])
        assertFalse(statistics.toString().contains("candidate-1"))
        assertFalse(statistics.toString().contains("com.example.mail"))
    }

    @Test
    fun `observe only statistics contain no planned source or application data`() {
        val statistics = DryRunPreview.observeOnly(activeNotificationCount = 4).toStatistics()

        assertEquals(4, statistics.activeCandidateCount)
        assertEquals(0, statistics.uniqueApplicationCount)
        assertEquals(DecisionSource.entries.associateWith { 0 }, statistics.countsByDecisionSource)
        assertEquals(PlannedAction.entries.associateWith { 0 }, statistics.countsByAction)
    }

    private fun action(
        candidateId: String,
        packageName: String,
        source: DecisionSource,
        plannedAction: PlannedAction
    ): PlannedNotificationAction = PlannedNotificationAction(
        candidateId = candidateId,
        packageName = packageName,
        selectedDecision = NotificationDecision.KeepNow,
        decisionSource = source,
        plannedAction = plannedAction
    )
}
