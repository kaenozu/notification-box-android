package com.notificationbox.app.permission

class FakePermissionStatusProvider : PermissionStatusProvider {
    var listenerGranted: Boolean = false
    var postGranted: Boolean = false

    override fun isNotificationListenerGranted(): Boolean = listenerGranted
    override fun canPostNotifications(): Boolean = postGranted
}
