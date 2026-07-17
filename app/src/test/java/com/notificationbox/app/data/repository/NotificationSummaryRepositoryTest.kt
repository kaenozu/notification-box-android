package com.notificationbox.app.data.repository

import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSummaryRepositoryTest {
    private val now = Instant.parse("2026-07-17T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `summary counts recent automatic decisions and ignores display overrides`() = runTest {
        val repository = FakeNotificationRepository()
        repository.emit(
            listOf(
                item(
                    key = "keep",
                    postTime = now.minus(Duration.ofHours(1)),
                    automatic = NotificationDecision.KeepNow
                ),
                item(
                    key = "overridden",
                    postTime = now.minus(Duration.ofHours(2)),
                    automatic = NotificationDecision.Ignore,
                    effective = NotificationDecision.KeepNow,
                    source = DecisionSource.UserOverride
                ),
                item(
                    key = "old",
                    postTime = now.minus(Duration.ofHours(25)),
                    automatic = NotificationDecision.HoldForDigest
                )
            )
        )
        val periodStart = now.minus(Duration.ofHours(24))
        val source = NotificationSummaryRepository(repository, clock)

        val summary = source.observeSummarySince(periodStart).first()

        assertEquals(2, summary.totalCount)
        assertEquals(1, summary.keepNowCount)
        assertEquals(0, summary.holdForDigestCount)
        assertEquals(1, summary.ignoreCount)
        assertEquals(periodStart, summary.periodStart)
        assertEquals(now, summary.generatedAt)
    }

    private fun item(
        key: String,
        postTime: Instant,
        automatic: NotificationDecision,
        effective: NotificationDecision = automatic,
        source: DecisionSource = DecisionSource.Automatic
    ) = NotificationItem(
        key = key,
        packageName = "com.example.app",
        appLabel = "Example",
        title = "title",
        text = "text",
        postTime = postTime,
        automaticDecision = automatic,
        userDecision = if (source == DecisionSource.UserOverride) effective else null,
        appRuleDecision = null,
        category = effective,
        decisionSource = source,
        automaticReason = "test",
        reason = "test"
    )
}
