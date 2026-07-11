package com.notificationbox.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationDatabaseMigrationTest {
    private val databaseName = "notification-migration-test.db"
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun teardown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `migration one to two preserves notifications and creates rule tables`() = runTest {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL(
                """
                CREATE TABLE IF NOT EXISTS notifications (
                    `key` TEXT NOT NULL,
                    packageName TEXT NOT NULL,
                    appLabel TEXT NOT NULL,
                    title TEXT,
                    text TEXT,
                    postTimeMillis INTEGER NOT NULL,
                    notificationId INTEGER NOT NULL,
                    tag TEXT,
                    channelId TEXT,
                    category TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    userPinned INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    removedAtMillis INTEGER,
                    PRIMARY KEY(`key`)
                )
                """.trimIndent()
            )
            legacy.execSQL(
                "CREATE INDEX index_notifications_postTimeMillis ON notifications(postTimeMillis)"
            )
            legacy.execSQL(
                "CREATE INDEX index_notifications_userPinned ON notifications(userPinned)"
            )
            legacy.execSQL(
                "CREATE INDEX index_notifications_isActive ON notifications(isActive)"
            )
            legacy.execSQL(
                """
                INSERT INTO notifications(
                    `key`, packageName, appLabel, title, text, postTimeMillis,
                    notificationId, tag, channelId, category, reason,
                    userPinned, isActive, removedAtMillis
                ) VALUES(
                    'existing', 'com.example.app', 'Example', 'Title', 'Text', 1000,
                    1, NULL, 'channel', 'KeepNow', 'test', 0, 1, NULL
                )
                """.trimIndent()
            )
            legacy.version = 1
        }

        val migrated = Room.databaseBuilder(
            context,
            NotificationDatabase::class.java,
            databaseName
        )
            .addMigrations(NotificationDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            val notification = migrated.notificationDao().getByKey("existing")
            assertEquals("existing", notification?.key)
            assertEquals("Title", notification?.title)
            assertNull(notification?.userDecision)

            migrated.appRuleDao().upsert(
                AppRuleEntity(
                    packageName = "com.example.app",
                    appLabel = "Example",
                    decision = "Ignore",
                    updatedAtMillis = 2000
                )
            )
            assertEquals(
                "Ignore",
                migrated.appRuleDao().getByPackageName("com.example.app")?.decision
            )

            migrated.classificationStatsDao().increment("automatic.total")
            assertEquals(
                1L,
                migrated.classificationStatsDao().observeAll()
                    .let { flow -> kotlinx.coroutines.flow.first(flow) }
                    .single()
                    .count
            )
        } finally {
            migrated.close()
        }
    }
}
