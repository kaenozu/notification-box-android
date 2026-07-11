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

    // Android 12以下: ランタイム権限は無視され、通知有効のみで判定

    @Test
    fun `api32_enabled_true`() {
        fakePlatform.sdkInt = 32
        fakePlatform.areNotificationsEnabled = true
        fakePlatform.hasPostNotificationsPermission = false

        assertTrue("API 32で通知有効ならtrue", provider.canPostNotifications())
    }

    @Test
    fun `api32_disabled_false`() {
        fakePlatform.sdkInt = 32
        fakePlatform.areNotificationsEnabled = false
        fakePlatform.hasPostNotificationsPermission = true

        assertFalse("API 32で通知無効ならfalse", provider.canPostNotifications())
    }

    @Test
    fun `api33_runtimeDenied_enabled_false`() {
        fakePlatform.sdkInt = 33
        fakePlatform.hasPostNotificationsPermission = false
        fakePlatform.areNotificationsEnabled = true

        assertFalse("API 33で拒否かつ有効ならfalse", provider.canPostNotifications())
    }

    @Test
    fun `api33_runtimeGranted_disabled_false`() {
        fakePlatform.sdkInt = 33
        fakePlatform.hasPostNotificationsPermission = true
        fakePlatform.areNotificationsEnabled = false

        assertFalse("API 33で許可かつ無効ならfalse", provider.canPostNotifications())
    }

    @Test
    fun `api33_runtimeGranted_enabled_true`() {
        fakePlatform.sdkInt = 33
        fakePlatform.hasPostNotificationsPermission = true
        fakePlatform.areNotificationsEnabled = true

        assertTrue("API 33で許可かつ有効ならtrue", provider.canPostNotifications())
    }

    @Test
    fun `api33_runtimeDenied_disabled_false`() {
        fakePlatform.sdkInt = 33
        fakePlatform.hasPostNotificationsPermission = false
        fakePlatform.areNotificationsEnabled = false

        assertFalse("API 33で拒否かつ無効ならfalse", provider.canPostNotifications())
    }

    @Test
    fun `listener_notRegistered_false`() {
        fakePlatform.packageName = "com.test.app"
        fakePlatform.enabledListenerPackages = emptySet()

        assertFalse("自アプリ含まないならfalse", provider.isNotificationListenerGranted())
    }

    @Test
    fun `listener_registered_true`() {
        fakePlatform.packageName = "com.test.app"
        fakePlatform.enabledListenerPackages = setOf("com.test.app")

        assertTrue("自アプリ含むならtrue", provider.isNotificationListenerGranted())
    }
}