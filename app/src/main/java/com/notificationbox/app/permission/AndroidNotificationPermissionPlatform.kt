package com.notificationbox.app.permission

import android.app.Application
import androidx.core.app.NotificationManagerCompat

class AndroidNotificationPermissionPlatform(
    private val app: Application
) : NotificationPermissionPlatform {

    override val packageName: String = app.packageName

    override fun enabledListenerPackages(): Set<String> =
        NotificationManagerCompat.getEnabledListenerPackages(app).toSet()
}
