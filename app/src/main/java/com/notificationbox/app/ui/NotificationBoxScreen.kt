package com.notificationbox.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.BuildConfig
import com.notificationbox.app.R
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.IngestionErrorCode
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class HomeSection {
    Notifications,
    AppRules
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationBoxScreen(vm: NotificationBoxViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showStatsResetConfirmation by rememberSaveable { mutableStateOf(false) }
    var showPrivacyInfo by rememberSaveable { mutableStateOf(false) }
    var selectedSectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var appRuleTarget by remember { mutableStateOf<NotificationItem?>(null) }
    var deleteTarget by remember { mutableStateOf<NotificationItem?>(null) }
    val selectedSection = HomeSection.entries[selectedSectionIndex]
    val openListenerSettings = remember(context) { notificationListenerSettingsIntent(context) }
    val filteredItems = remember(state.items, state.selectedFilter) {
        state.items.filter { state.selectedFilter == null || it.category == state.selectedFilter }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("通知履歴をすべて削除しますか？") },
            text = { Text("ピン留めを含む端末内の履歴が削除されます。アプリ別ルールは残ります。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    vm.clearAll()
                }) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showStatsResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showStatsResetConfirmation = false },
            title = { Text("分類統計をリセットしますか？") },
            text = { Text("分類件数と補正操作の集計だけを削除します。通知履歴とアプリ別ルールは残ります。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStatsResetConfirmation = false
                        vm.resetClassificationStats()
                    }
                ) {
                    Text("リセット")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatsResetConfirmation = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("この履歴を削除しますか？") },
            text = { Text("「${target.appLabel}」のこの通知履歴だけを端末内から削除します。元のOS通知は変更しません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.delete(target.key)
                        deleteTarget = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showPrivacyInfo) {
        PrivacyInfoDialog(
            onDismiss = { showPrivacyInfo = false },
            onShowOnboarding = {
                showPrivacyInfo = false
                vm.resetOnboarding()
            }
        )
    }

    appRuleTarget?.let { target ->
        AppRuleDialog(
            item = target,
            onSelect = { decision ->
                vm.setAppRule(target.packageName, target.appLabel, decision)
                appRuleTarget = null
            },
            onDismiss = { appRuleTarget = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知箱") },
                actions = {
                    IconButton(onClick = { showPrivacyInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "データと安全性")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                StatusCard(
                    notificationAccessGranted = state.notificationAccessGranted,
                    processed = state.ingestionHealth.processedCommands,
                    failed = state.ingestionHealth.failedCommands,
                    lastError = state.ingestionHealth.lastError,
                    hasItems = state.items.isNotEmpty(),
                    onOpenListenerSettings = { context.startActivity(openListenerSettings) },
                    onSeed = vm::seed,
                    onClearAll = { showClearConfirmation = true }
                )
            }
            item { SafetyNoticeCard() }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSection == HomeSection.Notifications,
                        onClick = {
                            selectedSectionIndex = HomeSection.Notifications.ordinal
                        },
                        label = { Text("通知履歴 (${state.items.size})") }
                    )
                    FilterChip(
                        selected = selectedSection == HomeSection.AppRules,
                        onClick = {
                            selectedSectionIndex = HomeSection.AppRules.ordinal
                        },
                        label = { Text("アプリ別ルール (${state.appRules.size})") }
                    )
                }
            }

            when (selectedSection) {
                HomeSection.Notifications -> {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedFilter == null,
                                onClick = { vm.setFilter(null) },
                                label = { Text("すべて") }
                            )
                            NotificationDecision.entries.forEach { decision ->
                                FilterChip(
                                    selected = state.selectedFilter == decision,
                                    onClick = { vm.setFilter(decision) },
                                    label = { Text(decision.displayName()) }
                                )
                            }
                        }
                    }
                    items(
                        items = filteredItems,
                        key = { it.key }
                    ) { item ->
                        NotificationCard(
                            item = item,
                            onTogglePinned = {
                                vm.togglePinned(item.key, !item.userPinned)
                            },
                            onDelete = { deleteTarget = item },
                            onDecision = { decision ->
                                val next =
                                    if (item.userDecision == decision) null else decision
                                vm.setNotificationDecision(item.key, next)
                            },
                            onEditAppRule = { appRuleTarget = item }
                        )
                    }
                    if (filteredItems.isEmpty()) {
                        item {
                            EmptyNotificationsCard(
                                hasAnyNotifications = state.items.isNotEmpty(),
                                notificationAccessGranted = state.notificationAccessGranted,
                                onOpenListenerSettings = {
                                    context.startActivity(openListenerSettings)
                                }
                            )
                        }
                    }
                }

                HomeSection.AppRules -> {
                    item {
                        ClassificationStatsCard(
                            stats = state.classificationStats,
                            onReset = { showStatsResetConfirmation = true }
                        )
                    }
                    if (state.appRules.isEmpty()) {
                        item {
                            Card {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "アプリ別ルールはありません",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "通知履歴から「このアプリのルール」を選ぶと登録できます。"
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.appRules, key = AppRule::packageName) { rule ->
                            AppRuleCard(
                                rule = rule,
                                changeCount =
                                    state.classificationStats
                                        .appChangeCounts[rule.packageName] ?: 0,
                                onDecision = { decision ->
                                    vm.setAppRule(
                                        rule.packageName,
                                        rule.appLabel,
                                        decision
                                    )
                                },
                                onDelete = {
                                    vm.setAppRule(
                                        rule.packageName,
                                        rule.appLabel,
                                        null
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(
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
            Text(
                "通知を端末内で整理",
                style = MaterialTheme.typography.titleLarge
            )
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
                TextButton(
                    onClick = onClearAll,
                    enabled = hasItems
                ) {
                    Text("履歴をすべて削除")
                }
            }
        }
    }
}

@Composable
private fun SafetyNoticeCard() {
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
private fun NotificationCard(
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
                    Text(
                        item.appLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
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
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "履歴から削除"
                    )
                }
            }
            item.text?.let {
                Text(
                    text = it,
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
            Text(
                "この通知の分類",
                style = MaterialTheme.typography.labelLarge
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppRuleCard(
    rule: AppRule,
    changeCount: Long,
    onDecision: (NotificationDecision) -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(rule.packageName, rule.appLabel)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rule.appLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(rule.packageName, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "補正・設定: ${changeCount}回",
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
                Text("アプリ別設定を解除")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassificationStatsCard(
    stats: ClassificationStats,
    onReset: () -> Unit
) {
    val totalCorrections = stats.userOverrideChanges + stats.appRuleChanges
    val operationsPerNotification =
        if (stats.automaticallyClassified == 0L) {
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
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                            Text(
                                "${decision.displayName()} " +
                                    "${stats.automaticByDecision[decision] ?: 0}件"
                            )
                        }
                    )
                }
            }
            Text(
                "統計には通知本文やタイトルを保存せず、外部送信もしません。",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = onReset,
                enabled = stats != ClassificationStats()
            ) {
                Text("分類統計をリセット")
            }
        }
    }
}

@Composable
private fun AppRuleDialog(
    item: NotificationItem,
    onSelect: (NotificationDecision?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.appLabel}のルール") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "このアプリから届く通知全体に適用します。" +
                        "通知ごとの手動指定がある場合は、そちらを優先します。"
                )
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        }
    )
}

@Composable
private fun PrivacyInfoDialog(
    onDismiss: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("データと安全性") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("通知の送信元、タイトル、本文、時刻、分類結果を端末内だけに保存します。")
                Text("外部APIやクラウドへ送信せず、Androidバックアップも無効です。")
                Text("非アクティブでピン留めされていない7日超の履歴を整理し、履歴は原則500件を上限とします。")
                Text("このバージョンは元のOS通知を変更しません。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
        dismissButton = {
            TextButton(onClick = onShowOnboarding) { Text("初回説明を再表示") }
        }
    )
}

@Composable
private fun NotificationItem.displayReason(): String = when (decisionSource) {
    DecisionSource.Automatic -> automaticReason
    DecisionSource.AppRule -> stringResource(
        R.string.notification_reason_app_rule,
        appLabel,
        category.displayName()
    )
    DecisionSource.UserOverride -> stringResource(
        R.string.notification_reason_user_override,
        category.displayName()
    )
}

@Composable
private fun AppIcon(packageName: String, appLabel: String) {
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

@Composable
private fun EmptyNotificationsCard(
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
                Text(
                    "この分類の通知はありません",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("別の分類を選ぶと、保存済みの通知を確認できます。")
            }
        }
    }
}

private fun java.time.Instant.displayTimestamp(): String =
    DateTimeFormatter.ofPattern("M/d HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(this)

private fun ingestionHealthText(
    processed: Long,
    failed: Long,
    lastError: IngestionErrorCode?
): String =
    if (failed == 0L) {
        "通知取込は正常です（処理済み ${processed}件）"
    } else {
        "通知取込で累計 ${failed}件の問題を検出しました。最終状態: " +
            lastError.userMessage() +
            "。通知内容は診断情報へ保存しません。"
    }

private fun IngestionErrorCode?.userMessage(): String = when (this) {
    IngestionErrorCode.ACTIVE_SNAPSHOT_FAILED -> "現在の通知一覧を取得できませんでした"
    IngestionErrorCode.RECORD_MAPPING_FAILED -> "一部の通知を読み取れませんでした"
    IngestionErrorCode.REPOSITORY_OPERATION_FAILED -> "端末内への保存に失敗しました"
    IngestionErrorCode.COMMAND_QUEUE_CLOSED -> "通知取込の終了処理中に新しい通知を受け取りました"
    null -> "詳細不明"
}

private fun NotificationDecision.displayName(): String = when (this) {
    NotificationDecision.KeepNow -> "優先"
    NotificationDecision.HoldForDigest -> "あとで確認"
    NotificationDecision.Ignore -> "低優先"
}

private fun DecisionSource.displayName(): String = when (this) {
    DecisionSource.Automatic -> "自動分類"
    DecisionSource.AppRule -> "アプリ別ルール"
    DecisionSource.UserOverride -> "この通知の手動指定"
}
