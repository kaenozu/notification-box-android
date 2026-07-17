package com.notificationbox.app.permission

interface PermissionStatusProvider {
    fun isNotificationListenerGranted(): Boolean
}
