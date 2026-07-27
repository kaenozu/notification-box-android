package com.notificationbox.app.ui

import androidx.compose.runtime.Composable

/**
 * Notification tab entry point.
 *
 * The dry-run preview is intentionally not shown here because it permanently
 * consumed part of the notification list and did not perform an OS action.
 */
@Composable
fun Phase1NotificationBoxScreen(vm: NotificationBoxViewModel) {
    NotificationBoxScreen(vm)
}
