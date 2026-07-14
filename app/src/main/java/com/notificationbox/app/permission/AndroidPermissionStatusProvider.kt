package com.notificationbox.app.permission

class AndroidPermissionStatusProvider(
    private val platform: NotificationPermissionPlatform
) : PermissionStatusProvider {

    override fun isNotificationListenerGranted(): Boolean =
        platform.enabledListenerPackages().contains(platform.packageName)

    override fun hasPostNotificationsRuntimePermission(): Boolean =
        platform.sdkInt < 33 || platform.hasPostNotificationsPermission()

    override fun areAppNotificationsEnabled(): Boolean =
        platform.areNotificationsEnabled()
}
