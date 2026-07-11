package com.notificationbox.app.permission

interface NotificationPermissionPlatform {
    val sdkInt: Int
    val packageName: String
    fun isNotificationListenerGranted(): Boolean
    fun hasPostNotificationsPermission(): Boolean
    fun areNotificationsEnabled(): Boolean
}