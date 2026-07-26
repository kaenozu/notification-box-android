package com.notificationbox.app

import android.app.Application
import com.notificationbox.app.data.NotificationPreferences
import com.notificationbox.app.data.db.NotificationDatabase
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.data.repository.PaymentRepository
import com.notificationbox.app.data.repository.RoomNotificationRepository
import com.notificationbox.app.data.repository.RoomPaymentRepository
import com.notificationbox.app.data.settings.DataStoreSettingsRepository
import com.notificationbox.app.data.settings.SettingsRepository
import com.notificationbox.app.permission.AndroidNotificationPermissionPlatform
import com.notificationbox.app.permission.AndroidPermissionStatusProvider
import com.notificationbox.app.permission.NotificationPermissionPlatform
import com.notificationbox.app.permission.PermissionStatusProvider
import com.notificationbox.app.service.PaymentNotificationIngestor
import com.notificationbox.app.service.PaymentNotificationSink
import com.notificationbox.app.ui.AndroidNotificationContentPresenter
import com.notificationbox.app.ui.NotificationContentPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(
    app: Application,
    applicationScope: CoroutineScope
) {
    private val platform: NotificationPermissionPlatform = AndroidNotificationPermissionPlatform(app)
    private val database: NotificationDatabase = NotificationDatabase.create(app)
    private val notificationPreferences = NotificationPreferences(app)

    val permissionStatusProvider: PermissionStatusProvider = AndroidPermissionStatusProvider(platform)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(
        preferences = notificationPreferences,
        scope = applicationScope
    )
    val notificationRepository: NotificationRepository =
        RoomNotificationRepository(database = database)
    val paymentRepository: PaymentRepository =
        RoomPaymentRepository(database = database)
    val paymentNotificationSink: PaymentNotificationSink =
        PaymentNotificationIngestor(repository = paymentRepository)
    val notificationContentPresenter: NotificationContentPresenter =
        AndroidNotificationContentPresenter(app)
}

class App : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, applicationScope)
        applicationScope.launch {
            container.notificationRepository.pruneExpired()
        }
    }
}
