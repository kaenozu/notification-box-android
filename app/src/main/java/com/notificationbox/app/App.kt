package com.notificationbox.app

import android.app.Application
import com.notificationbox.app.data.NotificationPreferences
import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.data.db.NotificationDatabase
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.data.repository.RoomNotificationRepository
import com.notificationbox.app.permission.AndroidNotificationPermissionPlatform
import com.notificationbox.app.permission.AndroidPermissionStatusProvider
import com.notificationbox.app.permission.NotificationPermissionPlatform
import com.notificationbox.app.permission.PermissionStatusProvider
import com.notificationbox.app.ui.AndroidNotificationContentPresenter
import com.notificationbox.app.ui.NotificationContentPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(app: Application) {
    private val platform: NotificationPermissionPlatform = AndroidNotificationPermissionPlatform(app)
    private val database: NotificationDatabase = NotificationDatabase.create(app)

    val permissionStatusProvider: PermissionStatusProvider = AndroidPermissionStatusProvider(platform)
    val notificationRepository: NotificationRepository =
        RoomNotificationRepository(database = database)
    val notificationContentPresenter: NotificationContentPresenter =
        AndroidNotificationContentPresenter(app)
}

class App : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationPreferences.initialize(this)
        NotificationStore.initialize()
        container = AppContainer(this)
        applicationScope.launch {
            container.notificationRepository.pruneExpired()
        }
    }
}
