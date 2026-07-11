package com.notificationbox.app.permission

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AndroidNotificationPermissionPlatform(
    private val app: Application
) : NotificationPermissionPlatform {

    override val sdkInt: Int = Build.VERSION.SDK_INT

    override val packageName: String = app.packageName

    override fun isNotificationListenerGranted(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(app)
        return packages.contains(app.packageName)
    }

    override fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true // Android 12以下ではランタイム権限不要
    }

    override fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(app).areNotificationsEnabled()
    }
}