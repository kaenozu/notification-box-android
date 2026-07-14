package com.notificationbox.app.permission

interface PermissionStatusProvider {
    fun isNotificationListenerGranted(): Boolean
    fun hasPostNotificationsRuntimePermission(): Boolean
    fun areAppNotificationsEnabled(): Boolean
}
