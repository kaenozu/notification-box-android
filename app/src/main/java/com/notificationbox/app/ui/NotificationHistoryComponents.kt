package com.notificationbox.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.notificationbox.app.BuildConfig
import com.notificationbox.app.model.IngestionErrorCode
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatusCard(
    notificationAccessGranted: Boolean,
    processed: Long,
    failed: Long,
    lastError: IngestionErrorCode?,
    hasItems: Boolean,
    onOpenListenerSettings: () -> Unit,
    onSeed: () -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("通知を端末内で整理", style = MaterialTheme.typography.titleLarge)
            Text(
                if (notificationAccessGranted) {
                    "通知アクセスは許可済みです。届いた通知を端末内の履歴へ反映します。"
                } else {
                    "通知アクセスが未許可です。許可するまで通知内容は読み取りません。"
                }
            )
            Text(
                ingestionHealthText(processed, failed, lastError),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenListenerSettings) {
                    Text(if (notificationAccessGranted) "通知アクセスを確認" else "通知アクセスを設定")
                }
                if (BuildConfig.DEBUG) {
                    Button(onClick = onSeed) { Text("デモ追加") }
                }
                TextButton(onClick = onClearAll, enabled = hasItems) {
                    Text("履歴をすべて削除")
                }
            }
        }
    }
}

@Composable
internal fun SafetyNoticeCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("安全な観察・試算版", style = MaterialTheme.typography.titleMedium)
            Text(
                "このバージョンは元のOS通知を削除、抑制、スヌーズ、遅延しません。" +
                    "分類と整理プレビューだけを行います。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationCard(
    item: NotificationItem,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit,
    onDecision: (NotificationDecision) -> Unit,
    onEditAppRule: () -> Unit
) {
    var expanded by rememberSaveable(item.key) { mutableStateOf(false) }

    Card {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AppIcon(item.packageName, item.appLabel)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(item.appLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        item.postTime.displayTimestamp(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        item.title ?: "タイトルなし",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (expanded) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePinned) {
                    Icon(
                        imageVector =
                            if (item.userPinned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription =
                            if (item.userPinned) "ピン留めを解除" else "ピン留め"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "履歴から削除")
                }
            }
            item.text?.let { body ->
                Text(
                    text = body,
                    maxLines = if (expanded) 8 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(item.category.displayName()) }
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(if (item.isActive) "端末に表示中" else "通知終了済み") }
                )
            }
            Text("この通知の分類", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationDecision.entries.forEach { decision ->
                    FilterChip(
                        selected = item.userDecision == decision,
                        onClick = { onDecision(decision) },
                        label = { Text(decision.displayName()) }
                    )
                }
            }
            Text(
                "選択中の分類をもう一度押すと、この通知だけ自動分類へ戻ります。",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onEditAppRule) {
                    Text("このアプリのルールを設定")
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                    Text(if (expanded) "詳細を閉じる" else "判定の詳細")
                }
            }
            if (expanded) {
                HorizontalDivider()
                Text(item.displayReason())
                Text(
                    "判定元: ${item.decisionSource.displayName()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "自動分類: ${item.automaticDecision.displayName()} — ${item.automaticReason}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun EmptyNotificationsCard(
    hasAnyNotifications: Boolean,
    notificationAccessGranted: Boolean,
    onOpenListenerSettings: () -> Unit
) {
    Card {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasAnyNotifications) {
                Text("通知履歴はまだありません", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (notificationAccessGranted) {
                        "通知が届くと、ここに端末内の分類履歴が表示されます。"
                    } else {
                        "通知アクセスを許可すると、以降に届く通知を端末内で分類できます。"
                    }
                )
                if (!notificationAccessGranted) {
                    Button(onClick = onOpenListenerSettings) {
                        Text("通知アクセスを設定")
                    }
                }
            } else {
                Text("この分類の通知はありません", style = MaterialTheme.typography.titleMedium)
                Text("別の分類を選ぶと、保存済みの通知を確認できます。")
            }
        }
    }
}

@Composable
internal fun AppIcon(packageName: String, appLabel: String) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = 48, height = 48)
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap,
            contentDescription = "${appLabel}のアイコン",
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "${appLabel}のアイコン",
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp)
        )
    }
}
