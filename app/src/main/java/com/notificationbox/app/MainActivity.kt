package com.notificationbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.notificationbox.app.ui.NotificationBoxApp
import com.notificationbox.app.ui.NotificationBoxViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        val factory = NotificationBoxViewModelFactory(
            container.permissionStatusProvider,
            container.notificationRepository
        )
        setContent {
            NotificationBoxApp(factory)
        }
    }
}
