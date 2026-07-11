package com.notificationbox.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.notificationbox.app.BuildConfig
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.NotificationDecision

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationBoxScreen(vm: NotificationBoxViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    val openListenerSettings = remember(context) { notificationListenerSettingsIntent(context) }
    val openAppNotificationSettings = remember(context) { appNotificationSettingsIntent(context) }

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
            text = { Text("ピン留めを含む端末内の履歴が削除されます。この操作は取り消せません。") },
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("観察から始めて、通知の傾向を確認します", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "状態: ${state.mode.displayName()} / " +
                                "通知アクセス: ${if (state.notificationAccessGranted) "許可済み" else "未許可"} / " +
                                "通知送信: ${if (state.postNotificationsGranted) "許可済み" else "未許可"}"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { context.startActivity(openListenerSettings) }) {
                                Text("通知アクセス")
                            }
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT >= 33 && !state.postNotificationsGranted) {
                                    postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    context.startActivity(openAppNotificationSettings)
                                }
                            }) {
                                Text("通知許可")
                            }
                            if (BuildConfig.DEBUG) {
                                Button(onClick = vm::seed) { Text("デモ追加") }
                            }
                            Button(
                                onClick = { showClearConfirmation = true },
                                enabled = state.items.isNotEmpty()
                            ) {
                                Text("全消去")
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { vm.setMode(AppMode.Observation) },
                            label = { Text("観察") },
                            leadingIcon = { Icon(Icons.Filled.Notifications, "観察モード") }
                        )
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("整理（準備中）") },
                            leadingIcon = { Icon(Icons.Filled.Security, "整理モードは準備中") }
                        )
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("一時停止（準備中）") },
                            leadingIcon = { Icon(Icons.Filled.Schedule, "一時停止は準備中") }
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
                    FilterChip(selected = state.selectedFilter == null, onClick = { vm.setFilter(null) }, label = { Text("すべて") })
                    FilterChip(selected = state.selectedFilter == NotificationDecision.KeepNow, onClick = { vm.setFilter(NotificationDecision.KeepNow) }, label = { Text("即時") })
                    FilterChip(selected = state.selectedFilter == NotificationDecision.HoldForDigest, onClick = { vm.setFilter(NotificationDecision.HoldForDigest) }, label = { Text("あとで確認") })
                    FilterChip(selected = state.selectedFilter == NotificationDecision.Ignore, onClick = { vm.setFilter(NotificationDecision.Ignore) }, label = { Text("低優先") })
                }
            }
            items(
                items = state.items.filter { state.selectedFilter == null || it.category == state.selectedFilter },
                key = { it.key }
            ) { item ->
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.fillMaxWidth(0.76f)) {
                                Text(item.appLabel, style = MaterialTheme.typography.titleMedium)
                                Text(item.title ?: "タイトルなし")
                            }
                            IconButton(onClick = { vm.togglePinned(item.key, !item.userPinned) }) {
                                Icon(
                                    imageVector = if (item.userPinned) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = if (item.userPinned) "ピン留めを解除" else "ピン留め"
                                )
                            }
                            IconButton(onClick = { vm.delete(item.key) }) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = "履歴から削除")
                            }
                        }
                        item.text?.let { Text(it) }
                        Text(item.reason)
                        Text(
                            "判定: ${item.category.displayName()} / 固定: ${if (item.userPinned) "あり" else "なし"} / " +
                                if (item.isActive) "端末に表示中" else "端末から消去済み"
                        )
                    }
                }
            }
            if (state.items.isEmpty()) {
                item {
                    Card {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("通知がありません", style = MaterialTheme.typography.titleMedium)
                            Text("通知アクセスを許可すると、ここに履歴が表示されます")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
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
