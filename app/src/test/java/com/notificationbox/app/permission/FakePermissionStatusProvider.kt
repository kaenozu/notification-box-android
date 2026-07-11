package com.notificationbox.app.permission

class FakePermissionStatusProvider(
    private val platform: FakeNotificationPermissionPlatform = FakeNotificationPermissionPlatform()
) : PermissionStatusProvider {

    override fun isNotificationListenerGranted(): Boolean = platform.isNotificationListenerGranted()
    override fun canPostNotifications(): Boolean = platform.hasPostNotificationsPermission() && platform.areNotificationsEnabled()

    fun setListenerGranted(granted: Boolean) { platform.enabledListenerPackages = if (granted) setOf(platform.packageName) else emptySet() }
    fun setPostNotificationsPermission(granted: Boolean) { platform.hasPostNotificationsPermission = granted }
    fun setNotificationsEnabled(enabled: Boolean) { platform.areNotificationsEnabled = enabled }
}