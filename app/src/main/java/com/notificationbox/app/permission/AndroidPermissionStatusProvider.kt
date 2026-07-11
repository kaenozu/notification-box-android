package com.notificationbox.app.permission

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AndroidPermissionStatusProvider(
    private val app: Application
) : PermissionStatusProvider {

    override fun isNotificationListenerGranted(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(app)
        return packages.contains(app.packageName)
    }

    override fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val hasRuntimePermission = ContextCompat.checkSelfPermission(
                app, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            val appNotificationsEnabled = NotificationManagerCompat.from(app).areNotificationsEnabled()
            return hasRuntimePermission && appNotificationsEnabled
        }
        return NotificationManagerCompat.from(app).areNotificationsEnabled()
    }
}
