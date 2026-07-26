package com.notificationbox.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.notificationbox.app.ui.NotificationBoxApp
import com.notificationbox.app.ui.NotificationBoxViewModelFactory
import com.notificationbox.app.ui.payment.PaymentViewModelFactory
import com.notificationbox.app.ui.summary.NotificationSummaryViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val container = (application as App).container
        val notificationFactory = NotificationBoxViewModelFactory(
            permissionProvider = container.permissionStatusProvider,
            notificationRepository = container.notificationRepository,
            settingsRepository = container.settingsRepository,
            notificationContentPresenter = container.notificationContentPresenter
        )
        val summaryFactory = NotificationSummaryViewModelFactory(
            repository = container.notificationRepository
        )
        val paymentFactory = PaymentViewModelFactory(
            repository = container.paymentRepository
        )
        setContent {
            NotificationBoxApp(
                notificationFactory = notificationFactory,
                summaryFactory = summaryFactory,
                paymentFactory = paymentFactory
            )
        }
    }
}
