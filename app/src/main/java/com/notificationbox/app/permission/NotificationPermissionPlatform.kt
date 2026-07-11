package com.notificationbox.app.permission

interface NotificationPermissionPlatform {
    val sdkInt: Int
    val packageName: String
    fun hasPostNotificationsPermission(): Boolean
    fun areNotificationsEnabled(): Boolean
    fun enabledListenerPackages(): Set<String>
}