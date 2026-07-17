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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notificationbox.app.R
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
                    Text(
                        stringResource(R.string.notification_rule_change_count, changeCount),
                        style = MaterialTheme.typography.bodySmall
                    )
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
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.notification_rule_remove))
            }
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
            Text(
                stringResource(R.string.notification_stats_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(
                    R.string.notification_stats_summary,
                    stats.automaticallyClassified,
                    stats.userOverrideChanges,
                    stats.appRuleChanges
                )
            )
            Text(
                stringResource(
                    R.string.notification_stats_operation_ratio,
                    operationsPerNotification
                ),
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
                            Text(
                                stringResource(
                                    R.string.notification_stats_decision_count,
                                    decision.displayName(),
                                    stats.automaticByDecision[decision] ?: 0
                                )
                            )
                        }
                    )
                }
            }
            Text(
                stringResource(R.string.notification_stats_privacy),
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onReset, enabled = stats != ClassificationStats()) {
                Text(stringResource(R.string.notification_stats_reset_action))
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
        title = {
            Text(stringResource(R.string.notification_rule_title, item.appLabel))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.notification_rule_body))
                NotificationDecision.entries.forEach { decision ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(decision) }
                    ) {
                        Text(
                            stringResource(
                                R.string.notification_rule_always,
                                decision.displayName()
                            )
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelect(null) }
                ) {
                    Text(stringResource(R.string.notification_rule_remove))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}

@Composable
internal fun EmptyAppRulesCard() {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.notification_rules_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.notification_rules_empty_body))
        }
    }
}
