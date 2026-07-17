package com.notificationbox.app.permission

class FakePermissionStatusProvider : PermissionStatusProvider {
    var listenerGranted = false

    var listenerCallCount = 0
        private set

    override fun isNotificationListenerGranted(): Boolean {
        listenerCallCount++
        return listenerGranted
    }

    fun resetCallCounts() {
        listenerCallCount = 0
    }
}
