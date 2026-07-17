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
