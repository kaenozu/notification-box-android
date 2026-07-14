package com.notificationbox.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.BuildConfig
import com.notificationbox.app.R
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.IngestionErrorCode
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
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
    var selectedSectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var appRuleTarget by remember { mutableStateOf<NotificationItem?>(null) }
    val selectedSection = HomeSection.entries[selectedSectionIndex]
    val openListenerSettings = remember(context) { notificationListenerSettingsIntent(context) }
    val openAppNotificationSettings = remember(context) { appNotificationSettingsIntent(context) }
    val filteredItems = remember(state.items, state.selectedFilter) {
        state.items.filter { state.selectedFilter == null || it.category == state.selectedFilter }
    }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        vm.refreshPermissions()
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
            title = { Text("通知履歴を全て削除しますか？") },
            text = { Text("ピン留めを含む端末内の履歴が削除されます。アプリ別ルールと分類統計は残ります。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        vm.clearAll()
                    }
                ) {
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
        topBar = { TopAppBar(title = { Text("通知箱") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
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
                            "観察から始めて、自分に合うルールを作ります",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "状態: ${state.mode.displayName()} / " +
                                "通知アクセス: ${state.notificationAccessGranted.statusText()} / " +
                                "送信権限: ${state.postNotificationsRuntimeGranted.statusText()} / " +
                                "通知設定: ${state.appNotificationsEnabled.statusText()}"
                        )
                        Text(
                            ingestionHealthText(
                                processed = state.ingestionHealth.processedCommands,
                                failed = state.ingestionHealth.failedCommands,
                                lastError = state.ingestionHealth.lastError
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { context.startActivity(openListenerSettings) }) {
                                Text("通知アクセス")
                            }
                            Button(
                                onClick = {
                                    val needsRuntimePermission =
                                        Build.VERSION.SDK_INT >= 33 &&
                                            !state.postNotificationsRuntimeGranted
                                    if (needsRuntimePermission) {
                                        postNotificationLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    } else {
                                        context.startActivity(openAppNotificationSettings)
                                    }
                                }
                            ) {
                                val label =
                                    if (
                                        Build.VERSION.SDK_INT >= 33 &&
                                        !state.postNotificationsRuntimeGranted
                                    ) {
                                        "送信権限を許可"
                                    } else {
                                        "通知設定"
                                    }
                                Text(label)
                            }
                            if (BuildConfig.DEBUG) {
                                Button(onClick = vm::seed) { Text("デモ追加") }
                            }
                            Button(
                                onClick = { showClearConfirmation = true },
                                enabled = state.items.isNotEmpty()
                            ) {
                                Text("履歴を全消去")
                            }
                        }
                    }
                }
            }

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
                        label = { Text("通知履歴") }
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
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { vm.setMode(AppMode.Observation) },
                                    label = { Text("観察") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Notifications, "観察モード")
                                    }
                                )
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text("整理（準備中）") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Security,
                                            "整理モードは準備中"
                                        )
                                    }
                                )
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text("一時停止（準備中）") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Schedule,
                                            "一時停止は準備中"
                                        )
                                    }
                                )
                            }
                            Text(
                                "OS通知の抑止・スヌーズ・ダイジェスト配信はまだ行いません。",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
                            onDelete = { vm.delete(item.key) },
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
                                hasAnyNotifications = state.items.isNotEmpty()
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
private fun NotificationCard(
    item: NotificationItem,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit,
    onDecision: (NotificationDecision) -> Unit,
    onEditAppRule: () -> Unit
) {
    Card {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(0.72f)) {
                    AppIcon(item.packageName, item.appLabel)
                    Column {
                        Text(
                            item.appLabel,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(item.title ?: "タイトルなし")
                    }
                }
                IconButton(onClick = onTogglePinned) {
                    Icon(
                        imageVector =
                            if (item.userPinned) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.Star
                            },
                        contentDescription =
                            if (item.userPinned) {
                                "ピン留めを解除"
                            } else {
                                "ピン留め"
                            }
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteForever,
                        contentDescription = "履歴から削除"
                    )
                }
            }
            item.text?.let { Text(it) }
            Text(item.displayReason())
            Text(
                "判定: ${item.category.displayName()} / " +
                    "${item.decisionSource.displayName()} / " +
                    if (item.isActive) "端末に表示中" else "端末から消去済み",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "自動判定: ${item.automaticDecision.displayName()} — " +
                    item.automaticReason,
                style = MaterialTheme.typography.bodySmall
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
                "選択中の項目をもう一度押すと、この通知だけ自動判定へ戻ります。",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onEditAppRule) {
                Text("このアプリのルールを設定")
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
            Row {
                AppIcon(rule.packageName, rule.appLabel)
                Column {
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
private fun EmptyNotificationsCard(hasAnyNotifications: Boolean) {
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
                Text("通知がありません", style = MaterialTheme.typography.titleMedium)
                Text("通知アクセスを許可すると、ここに履歴が表示されます")
            } else {
                Text(
                    "この分類の通知はありません",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("別の分類を選ぶと、保存済みの通知を確認できます")
            }
        }
    }
}

private fun Boolean.statusText(): String = if (this) "許可済み" else "未許可"

private fun ingestionHealthText(
    processed: Long,
    failed: Long,
    lastError: IngestionErrorCode?
): String =
    if (failed == 0L) {
        "通知取込: 正常（処理済み ${processed}件）"
    } else {
        "通知取込: エラー累計 ${failed}件 / 最終コード " +
            (lastError?.name ?: "UNKNOWN") +
            "（通知内容はログへ保存しません）"
    }

private fun AppMode.displayName(): String = when (this) {
    AppMode.Observation -> "観察"
    AppMode.Active -> "整理"
}

private fun NotificationDecision.displayName(): String = when (this) {
    NotificationDecision.KeepNow -> "即時"
    NotificationDecision.HoldForDigest -> "あとで確認"
    NotificationDecision.Ignore -> "低優先"
}

private fun DecisionSource.displayName(): String = when (this) {
    DecisionSource.Automatic -> "自動分類"
    DecisionSource.AppRule -> "アプリ別ルール"
    DecisionSource.UserOverride -> "この通知の手動指定"
}
