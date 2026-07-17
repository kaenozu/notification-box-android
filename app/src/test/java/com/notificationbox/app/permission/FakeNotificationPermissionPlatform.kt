package com.notificationbox.app.permission

class FakeNotificationPermissionPlatform : NotificationPermissionPlatform {
    override var packageName: String = "com.test.app"
    var enabledListenerPackages: Set<String> = emptySet()

    override fun enabledListenerPackages(): Set<String> = enabledListenerPackages
}
