package com.notificationbox.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.R
import com.notificationbox.app.model.NotificationSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object NotificationSummaryTestTags {
    const val LOADING = "notification_summary_loading"
    const val EMPTY = "notification_summary_empty"
    const val ERROR = "notification_summary_error"
    const val CONTENT = "notification_summary_content"
    const val TOTAL_CARD = "notification_summary_total_card"
    const val KEEP_NOW_CARD = "notification_summary_keep_now_card"
    const val HOLD_FOR_DIGEST_CARD = "notification_summary_hold_for_digest_card"
    const val IGNORE_CARD = "notification_summary_ignore_card"
}

@Composable
fun NotificationSummaryRoute(
    viewModel: NotificationSummaryViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationSummaryScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun NotificationSummaryScreen(
    uiState: NotificationSummaryUiState,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        NotificationSummaryUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .testTag(NotificationSummaryTestTags.LOADING),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        NotificationSummaryUiState.Empty -> {
            MessageCard(
                title = stringResource(R.string.notification_summary_empty_title),
                message = stringResource(R.string.notification_summary_empty_body),
                modifier = modifier.testTag(NotificationSummaryTestTags.EMPTY)
            )
        }

        NotificationSummaryUiState.Error -> {
            MessageCard(
                title = stringResource(R.string.notification_summary_error_title),
                message = stringResource(R.string.notification_summary_error_body),
                modifier = modifier.testTag(NotificationSummaryTestTags.ERROR)
            )
        }

        is NotificationSummaryUiState.Content -> {
            SummaryContent(
                summary = uiState.summary,
                modifier = modifier.testTag(NotificationSummaryTestTags.CONTENT)
            )
        }
    }
}

@Composable
private fun SummaryContent(
    summary: NotificationSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.notification_summary_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(
                    R.string.notification_summary_period,
                    summary.periodStart.displayTimestamp(),
                    summary.generatedAt.displayTimestamp()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SummaryMetricCard(
            title = stringResource(R.string.notification_summary_total_title),
            count = summary.totalCount,
            description = stringResource(R.string.notification_summary_total_description),
            testTag = NotificationSummaryTestTags.TOTAL_CARD
        )
        SummaryMetricCard(
            title = stringResource(R.string.notification_summary_keep_title),
            count = summary.keepNowCount,
            description = stringResource(R.string.notification_summary_keep_description),
            testTag = NotificationSummaryTestTags.KEEP_NOW_CARD
        )
        SummaryMetricCard(
            title = stringResource(R.string.notification_summary_digest_title),
            count = summary.holdForDigestCount,
            description = stringResource(R.string.notification_summary_digest_description),
            testTag = NotificationSummaryTestTags.HOLD_FOR_DIGEST_CARD
        )
        SummaryMetricCard(
            title = stringResource(R.string.notification_summary_ignore_title),
            count = summary.ignoreCount,
            description = stringResource(R.string.notification_summary_ignore_description),
            testTag = NotificationSummaryTestTags.IGNORE_CARD
        )
        Text(
            text = stringResource(R.string.notification_summary_automatic_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    count: Int,
    description: String,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.notification_summary_count, count),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message)
        }
    }
}

private fun Instant.displayTimestamp(): String =
    DateTimeFormatter.ofPattern("M/d HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(this)
