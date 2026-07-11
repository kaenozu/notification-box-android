package com.notificationbox.app

import android.app.Application
import com.notificationbox.app.data.NotificationDatabase
import com.notificationbox.app.data.NotificationPreferences
import com.notificationbox.app.data.RoomNotificationRepository
import com.notificationbox.app.domain.NotificationRepository
import com.notificationbox.app.permission.AndroidNotificationPermissionPlatform
import com.notificationbox.app.permission.AndroidPermissionStatusProvider
import com.notificationbox.app.permission.NotificationPermissionPlatform
import com.notificationbox.app.permission.PermissionStatusProvider

class AppContainer(app: Application) {
    private val platform: NotificationPermissionPlatform = AndroidNotificationPermissionPlatform(app)
    val permissionStatusProvider: PermissionStatusProvider = AndroidPermissionStatusProvider(platform)
    private val notificationDb: NotificationDatabase = NotificationDatabase.getInstance(app)
    val notificationRepository: NotificationRepository = RoomNotificationRepository(notificationDb)
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
