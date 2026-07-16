package com.notificationbox.app.domain.classification

import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class EffectiveClassificationResolverTest {
    private val resolver = EffectiveClassificationResolver()

    @Test
    fun `user decision has highest priority`() {
        val result = resolver.resolve(
            automaticDecision = NotificationDecision.KeepNow,
            appRuleDecision = NotificationDecision.HoldForDigest,
            userDecision = NotificationDecision.Ignore
        )

        assertEquals(NotificationDecision.Ignore, result.decision)
        assertEquals(DecisionSource.UserOverride, result.source)
    }

    @Test
    fun `app rule overrides automatic classification`() {
        val result = resolver.resolve(
            automaticDecision = NotificationDecision.KeepNow,
            appRuleDecision = NotificationDecision.Ignore,
            userDecision = null
        )

        assertEquals(NotificationDecision.Ignore, result.decision)
        assertEquals(DecisionSource.AppRule, result.source)
    }

    @Test
    fun `automatic classification is used without overrides`() {
        val result = resolver.resolve(
            automaticDecision = NotificationDecision.HoldForDigest,
            appRuleDecision = null,
            userDecision = null
        )

        assertEquals(NotificationDecision.HoldForDigest, result.decision)
        assertEquals(DecisionSource.Automatic, result.source)
    }
}
