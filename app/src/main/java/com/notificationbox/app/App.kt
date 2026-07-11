package com.notificationbox.app

import android.app.Application
import com.notificationbox.app.data.NotificationPreferences
import com.notificationbox.app.permission.AndroidPermissionStatusProvider
import com.notificationbox.app.permission.PermissionStatusProvider

class AppContainer(app: Application) {
    val permissionStatusProvider: PermissionStatusProvider =
        AndroidPermissionStatusProvider(app)
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationPreferences.initialize(this)
    }
}
