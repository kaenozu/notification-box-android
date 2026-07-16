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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.model.NotificationSummary
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationSummaryRoute(
    viewModel: NotificationSummaryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationSummaryScreen(
        uiState = uiState,
        modifier = modifier
    )
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
                title = "過去24時間の通知はありません",
                message = "通知が届くと、分類別の件数をここで確認できます。",
                modifier = modifier
            )
        }

        is NotificationSummaryUiState.Error -> {
            MessageCard(
                title = "サマリーを表示できません",
                message = uiState.message,
                modifier = modifier
            )
        }

        is NotificationSummaryUiState.Content -> {
            SummaryContent(
                summary = uiState.summary,
                modifier = modifier
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
                text = "過去24時間の通知サマリー",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${summary.periodStart.displayTimestamp()} 以降・" +
                    "${summary.generatedAt.displayTimestamp()} 更新",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SummaryMetricCard(
            title = "総通知",
            count = summary.totalCount,
            description = "端末内へ保存された通知"
        )
        SummaryMetricCard(
            title = "優先",
            count = summary.keepNowCount,
            description = "KeepNow に自動分類"
        )
        SummaryMetricCard(
            title = "あとで確認",
            count = summary.holdForDigestCount,
            description = "HoldForDigest に自動分類"
        )
        SummaryMetricCard(
            title = "低優先",
            count = summary.ignoreCount,
            description = "Ignore に自動分類"
        )
        Text(
            text = "件数は通知受信時の自動分類を集計します。手動指定やアプリ別ルールによる表示上の変更は含みません。",
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
            Text("${count}件", style = MaterialTheme.typography.headlineMedium)
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

private fun java.time.Instant.displayTimestamp(): String =
    DateTimeFormatter.ofPattern("M/d HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(this)
