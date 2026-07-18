package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notificationbox.app.R
import com.notificationbox.app.ui.summary.NotificationSummaryRoute
import com.notificationbox.app.ui.summary.NotificationSummaryViewModel

private enum class RootDestination {
    Notifications,
    Summary
}

@Composable
fun NotificationHomeScreen(
    notificationViewModel: NotificationBoxViewModel,
    summaryViewModel: NotificationSummaryViewModel
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selected = RootDestination.entries[selectedIndex]

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selected == RootDestination.Notifications,
                    onClick = { selectedIndex = RootDestination.Notifications.ordinal },
                    icon = {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.notification_root_notifications)) }
                )
                NavigationBarItem(
                    selected = selected == RootDestination.Summary,
                    onClick = { selectedIndex = RootDestination.Summary.ordinal },
                    icon = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.notification_root_summary)) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selected) {
                RootDestination.Notifications -> {
                    Phase1NotificationBoxScreen(notificationViewModel)
                }

                RootDestination.Summary -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            NotificationSummaryRoute(summaryViewModel)
                        }
                    }
                }
            }
        }
    }
}
