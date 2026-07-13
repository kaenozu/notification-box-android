package com.notificationbox.app.domain.dryrun

import com.notificationbox.app.model.DecisionSource

/** Aggregate-only statistics derived from the current preview. */
data class DryRunStatistics(
    val activeCandidateCount: Int,
    val uniqueApplicationCount: Int,
    val countsByDecisionSource: Map<DecisionSource, Int>,
    val countsByAction: Map<PlannedAction, Int>
)

fun DryRunPreview.toStatistics(): DryRunStatistics {
    val sourceCounts = plannedActions
        .groupingBy(PlannedNotificationAction::decisionSource)
        .eachCount()

    return DryRunStatistics(
        activeCandidateCount = activeNotificationCount,
        uniqueApplicationCount = plannedActions
            .asSequence()
            .map(PlannedNotificationAction::packageName)
            .distinct()
            .count(),
        countsByDecisionSource = DecisionSource.entries.associateWith { source ->
            sourceCounts[source] ?: 0
        },
        countsByAction = this.countsByAction
    )
}
