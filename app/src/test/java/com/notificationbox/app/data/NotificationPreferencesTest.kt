package com.notificationbox.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationPreferencesTest {
    private lateinit var preferences: NotificationPreferences

    @Before
    fun createPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = NotificationPreferences(context)
    }

    @Test
    fun `digest schedule restores the saved number of daily times`() = runTest {
        preferences.saveDigestHours(listOf(20))
        assertEquals(
            listOf(20),
            preferences.observeState().first().digestSchedule.hours
        )

        preferences.saveDigestHours(listOf(9, 18))
        assertEquals(
            listOf(9, 18),
            preferences.observeState().first().digestSchedule.hours
        )
    }

    @Test
    fun `invalid empty digest schedule restores defaults`() = runTest {
        preferences.saveDigestHours(emptyList())

        assertEquals(
            listOf(8, 12, 18, 21),
            preferences.observeState().first().digestSchedule.hours
        )
    }

    @Test
    fun `onboarding acknowledgement persists explicitly`() = runTest {
        preferences.saveOnboardingCompleted(false)
        assertFalse(preferences.observeState().first().onboardingCompleted)

        preferences.saveOnboardingCompleted(true)
        assertTrue(preferences.observeState().first().onboardingCompleted)
    }
}
