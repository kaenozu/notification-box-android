package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem

private enum class HomeSection {
    Notifications,
    AppRules
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationBoxScreen(vm: NotificationBoxViewModel) {
    val context = LocalContext.current
    val history by vm.historyState.collectAsStateWithLifecycle()
    val settingsRules by vm.settingsRulesState.collectAsStateWithLifecycle()
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showStatsResetConfirmation by rememberSaveable { mutableStateOf(false) }
    var showPrivacyInfo by rememberSaveable { mutableStateOf(false) }
    var selectedSectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var appRuleTarget by remember { mutableStateOf<NotificationItem?>(null) }
    var deleteTarget by remember { mutableStateOf<NotificationItem?>(null) }
    val selectedSection = HomeSection.entries[selectedSectionIndex]
    val openListenerSettings = remember(context) { notificationListenerSettingsIntent(context) }
    val filteredItems = remember(history.items, history.selectedFilter) {
        history.items.filter {
            history.selectedFilter == null || it.category == history.selectedFilter
        }
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
                    notificationAccessGranted = history.notificationAccessGranted,
                    processed = history.ingestionHealth.processedCommands,
                    failed = history.ingestionHealth.failedCommands,
                    lastError = history.ingestionHealth.lastError,
                    hasItems = history.items.isNotEmpty(),
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
                        label = { Text("通知履歴 (${history.items.size})") }
                    )
                    FilterChip(
                        selected = selectedSection == HomeSection.AppRules,
                        onClick = {
                            selectedSectionIndex = HomeSection.AppRules.ordinal
                        },
                        label = { Text("アプリ別ルール (${settingsRules.appRules.size})") }
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
                                selected = history.selectedFilter == null,
                                onClick = { vm.setFilter(null) },
                                label = { Text("すべて") }
                            )
                            NotificationDecision.entries.forEach { decision ->
                                FilterChip(
                                    selected = history.selectedFilter == decision,
                                    onClick = { vm.setFilter(decision) },
                                    label = { Text(decision.displayName()) }
                                )
                            }
                        }
                    }
                    items(filteredItems, key = NotificationItem::key) { item ->
                        NotificationCard(
                            item = item,
                            onTogglePinned = {
                                vm.togglePinned(item.key, !item.userPinned)
                            },
                            onDelete = { deleteTarget = item },
                            onDecision = { decision ->
                                val next = if (item.userDecision == decision) null else decision
                                vm.setNotificationDecision(item.key, next)
                            },
                            onEditAppRule = { appRuleTarget = item }
                        )
                    }
                    if (filteredItems.isEmpty()) {
                        item {
                            EmptyNotificationsCard(
                                hasAnyNotifications = history.items.isNotEmpty(),
                                notificationAccessGranted = history.notificationAccessGranted,
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
                            stats = settingsRules.classificationStats,
                            onReset = { showStatsResetConfirmation = true }
                        )
                    }
                    if (settingsRules.appRules.isEmpty()) {
                        item { EmptyAppRulesCard() }
                    } else {
                        items(settingsRules.appRules, key = AppRule::packageName) { rule ->
                            AppRuleCard(
                                rule = rule,
                                changeCount =
                                    settingsRules.classificationStats
                                        .appChangeCounts[rule.packageName] ?: 0,
                                onDecision = { decision ->
                                    vm.setAppRule(rule.packageName, rule.appLabel, decision)
                                },
                                onDelete = {
                                    vm.setAppRule(rule.packageName, rule.appLabel, null)
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
