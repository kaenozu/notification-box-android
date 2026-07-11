package com.notificationbox.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NotificationBoxApp(vm: NotificationBoxViewModel = viewModel()) {
    MaterialTheme {
        NotificationBoxScreen(vm)
    }
}
