package com.notificationbox.app.permission

class AndroidPermissionStatusProvider(
    private val platform: NotificationPermissionPlatform
) : PermissionStatusProvider {

    override fun isNotificationListenerGranted(): Boolean =
        platform.enabledListenerPackages().contains(platform.packageName)
}
