package com.notificationbox.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NotificationBoxApp(
    factory: NotificationBoxViewModelFactory,
    vm: NotificationBoxViewModel = viewModel(factory = factory)
) {
    MaterialTheme {
        Phase1NotificationBoxScreen(vm)
    }
}
