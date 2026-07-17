package com.notificationbox.app.data.repository

import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationSummary
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun interface NotificationSummarySource {
    fun observeSummarySince(since: Instant): Flow<NotificationSummary>
}

class NotificationSummaryRepository(
    private val notificationRepository: NotificationRepository,
    private val clock: Clock = Clock.systemUTC()
) : NotificationSummarySource {
    override fun observeSummarySince(since: Instant): Flow<NotificationSummary> =
        notificationRepository.observeNotifications().map { items ->
            val recent = items.filterNot { item -> item.postTime.isBefore(since) }
            NotificationSummary(
                totalCount = recent.size,
                keepNowCount = recent.count {
                    it.automaticDecision == NotificationDecision.KeepNow
                },
                holdForDigestCount = recent.count {
                    it.automaticDecision == NotificationDecision.HoldForDigest
                },
                ignoreCount = recent.count {
                    it.automaticDecision == NotificationDecision.Ignore
                },
                periodStart = since,
                generatedAt = clock.instant()
            )
        }
}
