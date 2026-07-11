package com.notificationbox.app.permission

class FakePermissionStatusProvider : PermissionStatusProvider {
    var listenerGranted = false
    var postNotificationsGranted = false

    var listenerCallCount = 0
        private set

    var postCallCount = 0
        private set

    override fun isNotificationListenerGranted(): Boolean {
        listenerCallCount++
        return listenerGranted
    }

    override fun canPostNotifications(): Boolean {
        postCallCount++
        return postNotificationsGranted
    }

    fun resetCallCounts() {
        listenerCallCount = 0
        postCallCount = 0
    }
}