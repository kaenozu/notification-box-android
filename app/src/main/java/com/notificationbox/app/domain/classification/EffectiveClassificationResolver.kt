package com.notificationbox.app.domain.classification

import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision

data class EffectiveClassification(
    val decision: NotificationDecision,
    val source: DecisionSource
)

/** Resolves the effective decision without depending on persistence or UI code. */
class EffectiveClassificationResolver {
    fun resolve(
        automaticDecision: NotificationDecision,
        appRuleDecision: NotificationDecision?,
        userDecision: NotificationDecision?
    ): EffectiveClassification = when {
        userDecision != null -> EffectiveClassification(
            decision = userDecision,
            source = DecisionSource.UserOverride
        )

        appRuleDecision != null -> EffectiveClassification(
            decision = appRuleDecision,
            source = DecisionSource.AppRule
        )

        else -> EffectiveClassification(
            decision = automaticDecision,
            source = DecisionSource.Automatic
        )
    }
}
