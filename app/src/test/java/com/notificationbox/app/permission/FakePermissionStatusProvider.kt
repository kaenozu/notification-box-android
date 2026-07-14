package com.notificationbox.app.permission

class FakePermissionStatusProvider : PermissionStatusProvider {
    var listenerGranted = false
    var postNotificationsRuntimeGranted = false
    var appNotificationsEnabled = false

    var listenerCallCount = 0
        private set
    var runtimePermissionCallCount = 0
        private set
    var appNotificationsCallCount = 0
        private set

    override fun isNotificationListenerGranted(): Boolean {
        listenerCallCount++
        return listenerGranted
    }

    override fun hasPostNotificationsRuntimePermission(): Boolean {
        runtimePermissionCallCount++
        return postNotificationsRuntimeGranted
    }

    override fun areAppNotificationsEnabled(): Boolean {
        appNotificationsCallCount++
        return appNotificationsEnabled
    }

    fun resetCallCounts() {
        listenerCallCount = 0
        runtimePermissionCallCount = 0
        appNotificationsCallCount = 0
    }
}
