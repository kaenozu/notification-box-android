package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppRuleCard(
    rule: AppRule,
    changeCount: Long,
    onDecision: (NotificationDecision) -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(rule.packageName, rule.appLabel)
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.appLabel, style = MaterialTheme.typography.titleMedium)
                    Text(rule.packageName, style = MaterialTheme.typography.bodySmall)
                    Text("補正・設定: ${changeCount}回", style = MaterialTheme.typography.bodySmall)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationDecision.entries.forEach { decision ->
                    FilterChip(
                        selected = rule.decision == decision,
                        onClick = { onDecision(decision) },
                        label = { Text(decision.displayName()) }
                    )
                }
            }
            TextButton(onClick = onDelete) { Text("アプリ別設定を解除") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClassificationStatsCard(stats: ClassificationStats, onReset: () -> Unit) {
    val totalCorrections = stats.userOverrideChanges + stats.appRuleChanges
    val operationsPerNotification = if (stats.automaticallyClassified == 0L) {
        "0.00"
    } else {
        String.format(
            Locale.getDefault(),
            "%.2f",
            totalCorrections.toDouble() / stats.automaticallyClassified
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("端末内の分類傾向", style = MaterialTheme.typography.titleMedium)
            Text(
                "自動分類 ${stats.automaticallyClassified}件 / " +
                    "手動補正 ${stats.userOverrideChanges}回 / " +
                    "ルール変更 ${stats.appRuleChanges}回"
            )
            Text(
                "自動分類1件あたりの補正操作: ${operationsPerNotification}回",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationDecision.entries.forEach { decision ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text("${decision.displayName()} ${stats.automaticByDecision[decision] ?: 0}件")
                        }
                    )
                }
            }
            Text(
                "統計には通知本文やタイトルを保存せず、外部送信もしません。",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onReset, enabled = stats != ClassificationStats()) {
                Text("分類統計をリセット")
            }
        }
    }
}

@Composable
internal fun AppRuleDialog(
    item: NotificationItem,
    onSelect: (NotificationDecision?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.appLabel}のルール") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("このアプリ全体に適用します。通知ごとの手動指定を優先します。")
                NotificationDecision.entries.forEach { decision ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(decision) }
                    ) {
                        Text("常に${decision.displayName()}")
                    }
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelect(null) }
                ) {
                    Text("アプリ別設定を解除")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } }
    )
}

@Composable
internal fun EmptyAppRulesCard() {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("アプリ別ルールはありません", style = MaterialTheme.typography.titleMedium)
            Text("通知履歴から「このアプリのルール」を選ぶと登録できます。")
        }
    }
}
