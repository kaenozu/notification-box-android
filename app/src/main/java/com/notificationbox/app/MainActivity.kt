package com.notificationbox.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.notificationbox.app.ui.NotificationBoxApp
import com.notificationbox.app.ui.NotificationBoxViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val container = (application as App).container
        val factory = NotificationBoxViewModelFactory(
            permissionProvider = container.permissionStatusProvider,
            notificationRepository = container.notificationRepository
        )
        setContent {
            NotificationBoxApp(factory)
        }
    }
}
