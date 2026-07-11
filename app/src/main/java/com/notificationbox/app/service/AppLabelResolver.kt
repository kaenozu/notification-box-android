package com.notificationbox.app.service

import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap

fun interface AppLabelResolver {
    fun resolve(packageName: String): String
}

class CachingAppLabelResolver(
    private val packageManager: PackageManager
) : AppLabelResolver {
    private val cache = ConcurrentHashMap<String, String>()

    override fun resolve(packageName: String): String =
        cache.getOrPut(packageName) {
            runCatching {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(applicationInfo).toString()
            }.getOrElse {
                packageName.substringAfterLast('.').ifBlank { packageName }
            }
        }
}
