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
        title = { Text(stringResource(R.string.notification_data_safety)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.notification_privacy_storage))
                Text(stringResource(R.string.notification_privacy_network))
                Text(stringResource(R.string.notification_privacy_retention))
                Text(stringResource(R.string.notification_privacy_os_boundary))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
        dismissButton = {
            TextButton(onClick = onShowOnboarding) {
                Text(stringResource(R.string.notification_show_onboarding))
            }
        }
    )
}

internal fun Instant.displayTimestamp(): String =
    DateTimeFormatter.ofPattern("M/d HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(this)

@Composable
internal fun ingestionHealthText(
    processed: Long,
    failed: Long,
    lastError: IngestionErrorCode?
): String =
    if (failed == 0L) {
        stringResource(R.string.notification_ingestion_healthy, processed)
    } else {
        stringResource(
            R.string.notification_ingestion_failed,
            failed,
            lastError.userMessage()
        )
    }

@Composable
private fun IngestionErrorCode?.userMessage(): String = when (this) {
    IngestionErrorCode.ACTIVE_SNAPSHOT_FAILED ->
        stringResource(R.string.notification_error_snapshot)

    IngestionErrorCode.RECORD_MAPPING_FAILED ->
        stringResource(R.string.notification_error_mapping)

    IngestionErrorCode.REPOSITORY_OPERATION_FAILED ->
        stringResource(R.string.notification_error_repository)

    IngestionErrorCode.COMMAND_QUEUE_CLOSED ->
        stringResource(R.string.notification_error_queue_closed)

    null -> stringResource(R.string.notification_error_unknown)
}

@Composable
internal fun NotificationDecision.displayName(): String = when (this) {
    NotificationDecision.KeepNow -> stringResource(R.string.notification_decision_keep)
    NotificationDecision.HoldForDigest -> stringResource(R.string.notification_decision_digest)
    NotificationDecision.Ignore -> stringResource(R.string.notification_decision_ignore)
}

@Composable
internal fun DecisionSource.displayName(): String = when (this) {
    DecisionSource.Automatic -> stringResource(R.string.notification_source_automatic)
    DecisionSource.AppRule -> stringResource(R.string.notification_source_app_rule)
    DecisionSource.UserOverride -> stringResource(R.string.notification_source_user_override)
}
