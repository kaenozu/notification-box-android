package com.notificationbox.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationDatabaseMigrationTest {
    private val databaseName = "notification-migration-test"

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotificationDatabase::class.java
    )

    @Test
    fun `migration one to two preserves notifications and creates rule tables`() {
        migrationHelper.createDatabase(databaseName, 1).apply {
            execSQL(
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
            close()
        }

        val migrated = migrationHelper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            NotificationDatabase.MIGRATION_1_2
        )

        migrated.query(
            "SELECT `key`, userDecision FROM notifications WHERE `key` = 'existing'"
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("existing", cursor.getString(0))
            assertNull(cursor.getString(1))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'app_rules'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'classification_stats'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migrated.close()
    }
}
