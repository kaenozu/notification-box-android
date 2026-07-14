package com.notificationbox.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationPreferencesTest {
    @Before
    fun initializePreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationPreferences.initialize(context)
    }

    @Test
    fun `digest schedule restores the saved number of daily times`() = runTest {
        NotificationPreferences.saveDigestHours(listOf(20))
        assertEquals(
            listOf(20),
            NotificationPreferences.observeState().first().digestSchedule.hours
        )

        NotificationPreferences.saveDigestHours(listOf(9, 18))
        assertEquals(
            listOf(9, 18),
            NotificationPreferences.observeState().first().digestSchedule.hours
        )
    }

    @Test
    fun `invalid empty digest schedule restores defaults`() = runTest {
        NotificationPreferences.saveDigestHours(emptyList())

        assertEquals(
            listOf(8, 12, 18, 21),
            NotificationPreferences.observeState().first().digestSchedule.hours
        )
    }
}
