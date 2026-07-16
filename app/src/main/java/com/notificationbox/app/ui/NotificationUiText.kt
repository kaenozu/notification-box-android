package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notificationbox.app.R
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.IngestionErrorCode
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun NotificationItem.displayReason(): String = when (decisionSource) {
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
internal fun PrivacyInfoDialog(
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

internal fun Instant.displayTimestamp(): String =
    DateTimeFormatter.ofPattern("M/d HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(this)

internal fun ingestionHealthText(
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

internal fun NotificationDecision.displayName(): String = when (this) {
    NotificationDecision.KeepNow -> "優先"
    NotificationDecision.HoldForDigest -> "あとで確認"
    NotificationDecision.Ignore -> "低優先"
}

internal fun DecisionSource.displayName(): String = when (this) {
    DecisionSource.Automatic -> "自動分類"
    DecisionSource.AppRule -> "アプリ別ルール"
    DecisionSource.UserOverride -> "この通知の手動指定"
}
