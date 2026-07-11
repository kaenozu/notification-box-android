package com.notificationbox.app.permission

class FakeNotificationPermissionPlatform : NotificationPermissionPlatform {
    override var sdkInt: Int = 33
    override var packageName: String = "com.test.app"
    var hasPostNotificationsPermission: Boolean = false
    var areNotificationsEnabled: Boolean = false
    var enabledListenerPackages: Set<String> = emptySet()

    override fun isNotificationListenerGranted(): Boolean = enabledListenerPackages.contains(packageName)
    override fun hasPostNotificationsPermission(): Boolean = hasPostNotificationsPermission
    override fun areNotificationsEnabled(): Boolean = areNotificationsEnabled
}