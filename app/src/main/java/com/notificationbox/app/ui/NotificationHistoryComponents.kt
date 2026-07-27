package com.notificationbox.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.notificationbox.app.BuildConfig
import com.notificationbox.app.R
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
            Text(
                stringResource(R.string.notification_status_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(
                    if (notificationAccessGranted) {
                        R.string.notification_access_granted_body
                    } else {
                        R.string.notification_access_missing_body
                    }
                )
            )
            Text(
                ingestionHealthText(processed, failed, lastError),
                style = MaterialTheme.typography.bodySmall
            )
            if (!notificationAccessGranted) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    onClick = onOpenListenerSettings
                ) {
                    Text(stringResource(R.string.notification_access_setup))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (BuildConfig.DEBUG) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        onClick = onSeed
                    ) {
                        Text(stringResource(R.string.notification_demo_add))
                    }
                }
                TextButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    onClick = onClearAll,
                    enabled = hasItems
                ) {
                    Text(stringResource(R.string.notification_clear_all_action))
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
            Text(
                stringResource(R.string.notification_safety_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.notification_safety_body),
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
                        item.title ?: stringResource(R.string.notification_title_missing),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (expanded) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePinned) {
                    Icon(
                        imageVector =
                            if (item.userPinned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = stringResource(
                            if (item.userPinned) {
                                R.string.notification_unpin
                            } else {
                                R.string.notification_pin
                            }
                        )
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.notification_delete_history)
                    )
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
                    label = {
                        Text(
                            stringResource(
                                if (item.isActive) {
                                    R.string.notification_active
                                } else {
                                    R.string.notification_removed
                                }
                            )
                        )
                    }
                )
            }
            Text(
                stringResource(R.string.notification_classification_heading),
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationDecision.entries.forEach { decision ->
                    FilterChip(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        selected = item.userDecision == decision,
                        onClick = { onDecision(decision) },
                        label = { Text(decision.displayName()) }
                    )
                }
            }
            Text(
                stringResource(R.string.notification_classification_toggle_hint),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onEditAppRule) {
                    Text(stringResource(R.string.notification_edit_app_rule))
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                    Text(
                        stringResource(
                            if (expanded) {
                                R.string.notification_detail_close
                            } else {
                                R.string.notification_detail_open
                            }
                        )
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                Text(item.displayReason())
                Text(
                    stringResource(
                        R.string.notification_decision_source,
                        item.decisionSource.displayName()
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(
                        R.string.notification_automatic_result,
                        item.automaticDecision.displayName(),
                        item.automaticReason
                    ),
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
                Text(
                    stringResource(R.string.notification_empty_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(
                        if (notificationAccessGranted) {
                            R.string.notification_empty_granted_body
                        } else {
                            R.string.notification_empty_missing_permission_body
                        }
                    )
                )
                if (!notificationAccessGranted) {
                    Button(onClick = onOpenListenerSettings) {
                        Text(stringResource(R.string.notification_access_setup))
                    }
                }
            } else {
                Text(
                    stringResource(R.string.notification_filter_empty_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(stringResource(R.string.notification_filter_empty_body))
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
    val contentDescription = stringResource(
        R.string.notification_app_icon_description,
        appLabel
    )

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp)
        )
    }
}
