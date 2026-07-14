package com.notificationbox.app.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidPermissionStatusProviderTest {

    private lateinit var fakePlatform: FakeNotificationPermissionPlatform
    private lateinit var provider: AndroidPermissionStatusProvider

    @Before
    fun setup() {
        fakePlatform = FakeNotificationPermissionPlatform()
        provider = AndroidPermissionStatusProvider(fakePlatform)
    }

    @Test
    fun `api32 runtime permission is treated as granted`() {
        fakePlatform.sdkInt = 32
        fakePlatform.hasPostNotificationsPermission = false

        assertTrue(provider.hasPostNotificationsRuntimePermission())
    }

    @Test
    fun `api33 runtime permission reflects platform grant`() {
        fakePlatform.sdkInt = 33
        fakePlatform.hasPostNotificationsPermission = false
        assertFalse(provider.hasPostNotificationsRuntimePermission())

        fakePlatform.hasPostNotificationsPermission = true
        assertTrue(provider.hasPostNotificationsRuntimePermission())
    }

    @Test
    fun `app notification setting is independent from runtime permission`() {
        fakePlatform.hasPostNotificationsPermission = true
        fakePlatform.areNotificationsEnabled = false
        assertTrue(provider.hasPostNotificationsRuntimePermission())
        assertFalse(provider.areAppNotificationsEnabled())

        fakePlatform.hasPostNotificationsPermission = false
        fakePlatform.areNotificationsEnabled = true
        assertFalse(provider.hasPostNotificationsRuntimePermission())
        assertTrue(provider.areAppNotificationsEnabled())
    }

    @Test
    fun `listener not registered returns false`() {
        fakePlatform.packageName = "com.test.app"
        fakePlatform.enabledListenerPackages = emptySet()

        assertFalse(provider.isNotificationListenerGranted())
    }

    @Test
    fun `listener registered returns true`() {
        fakePlatform.packageName = "com.test.app"
        fakePlatform.enabledListenerPackages = setOf("com.test.app")

        assertTrue(provider.isNotificationListenerGranted())
    }
}
