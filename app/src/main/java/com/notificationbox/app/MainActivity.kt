package com.notificationbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.notificationbox.app.ui.NotificationBoxApp
import com.notificationbox.app.ui.NotificationBoxViewModelFactory
import com.notificationbox.app.ui.summary.NotificationSummaryViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        val factory = NotificationBoxViewModelFactory(
            permissionProvider = container.permissionStatusProvider,
            notificationRepository = container.notificationRepository
        )
        val summaryFactory = NotificationSummaryViewModelFactory(
            repository = container.notificationRepository
        )
        setContent {
            NotificationBoxApp(
                factory = factory,
                summaryFactory = summaryFactory
            )
        }
    }
}
