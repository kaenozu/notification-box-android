package com.notificationbox.app.permission

class AndroidPermissionStatusProvider(
    private val platform: NotificationPermissionPlatform
) : PermissionStatusProvider {

    override fun isNotificationListenerGranted(): Boolean {
        return platform.enabledListenerPackages().contains(platform.packageName)
    }

    override fun canPostNotifications(): Boolean {
        if (platform.sdkInt < 33) {
            // Android 12以下: ランタイム権限なし、通知有効のみで判定
            return platform.areNotificationsEnabled()
        }
        // Android 13以上: POST_NOTIFICATIONS ランタイム権限 + 通知有効 両方必須
        return platform.hasPostNotificationsPermission() && platform.areNotificationsEnabled()
    }
}