package com.notificationbox.app.permission

interface NotificationPermissionPlatform {
    val packageName: String
    fun enabledListenerPackages(): Set<String>
}
