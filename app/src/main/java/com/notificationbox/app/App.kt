package com.notificationbox.app

import android.app.Application
import com.notificationbox.app.data.NotificationPreferences
import com.notificationbox.app.data.db.NotificationDatabase
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.data.repository.RoomNotificationRepository
import com.notificationbox.app.permission.AndroidNotificationPermissionPlatform
import com.notificationbox.app.permission.AndroidPermissionStatusProvider
import com.notificationbox.app.permission.NotificationPermissionPlatform
import com.notificationbox.app.permission.PermissionStatusProvider

class AppContainer(app: Application) {
    private val platform: NotificationPermissionPlatform = AndroidNotificationPermissionPlatform(app)
    private val database: NotificationDatabase = NotificationDatabase.create(app)

    val permissionStatusProvider: PermissionStatusProvider = AndroidPermissionStatusProvider(platform)
    val notificationRepository: NotificationRepository =
        RoomNotificationRepository(database.notificationDao())
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
