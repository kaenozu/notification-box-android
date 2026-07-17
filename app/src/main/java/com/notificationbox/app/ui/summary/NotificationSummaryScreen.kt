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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.R
import com.notificationbox.app.model.NotificationSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                    .heightIn(min = 160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        NotificationSummaryUiState.Empty -> {
            MessageCard(
                title = stringResource(R.string.notification_summary_empty_title),
                message = stringResource(R.string.notification_summary_empty_body),
                modifier = modifier
            )
        }

        NotificationSummaryUiState.Error -> {
            MessageCard(
                title = stringResource(R.string.notification_summary_error_title),
                message = stringResource(R.string.notification_summary_error_body),
                modifier = modifier
            )
        }

        is NotificationSummaryUiState.Content -> {
            SummaryContent(summary = uiState.summary, modifier = modifier)
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
            description = stringResource(R.string.notification_summary_total_description)
        )
        SummaryMetricCard(
            title = stringResource(R.string.notification_summary_keep_title),
            count = summary.keepNowCount,
            description = stringResource(R.string.notification_summary_keep_description)
        )
        SummaryMetricCard(
            title = stringResource(R.string.notification_summary_digest_title),
            count = summary.holdForDigestCount,
            description = stringResource(R.string.notification_summary_digest_description)
        )
        SummaryMetricCard(
            title = stringResource(R.string.notification_summary_ignore_title),
            count = summary.ignoreCount,
            description = stringResource(R.string.notification_summary_ignore_description)
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
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
