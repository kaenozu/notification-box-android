package com.notificationbox.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDatabaseMigrationInstrumentedTest {
    private val databaseName = "notification-migration-instrumented.db"

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotificationDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .deleteDatabase(databaseName)
    }

    @Test
    fun migrationOneToThreePreservesDataAndValidatesSchema() {
        migrationHelper.createDatabase(databaseName, 1).apply {
            execSQL(
                """
                INSERT INTO notifications(
                    `key`, packageName, appLabel, title, text, postTimeMillis,
                    notificationId, tag, channelId, category, reason,
                    userPinned, isActive, removedAtMillis
                ) VALUES(
                    'existing', 'com.example.app', 'Example', 'Title', 'Text', 1000,
                    1, NULL, 'channel', 'KeepNow', 'automatic reason', 1, 1, NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = migrationHelper.runMigrationsAndValidate(
            databaseName,
            3,
            true,
            NotificationDatabase.MIGRATION_1_2,
            NotificationDatabase.MIGRATION_2_3
        )

        try {
            migrated.query(
                """
                SELECT `key`, title, text, userPinned, userDecision, contentAvailability
                FROM notifications
                WHERE `key` = 'existing'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("existing", cursor.getString(cursor.getColumnIndexOrThrow("key")))
                assertEquals("Title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                assertEquals("Text", cursor.getString(cursor.getColumnIndexOrThrow("text")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("userPinned")))
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("userDecision")))
                assertEquals(
                    "AVAILABLE",
                    cursor.getString(cursor.getColumnIndexOrThrow("contentAvailability"))
                )
                assertFalse(cursor.moveToNext())
            }

            migrated.execSQL(
                """
                INSERT INTO app_rules(packageName, appLabel, decision, updatedAtMillis)
                VALUES('com.example.app', 'Example', 'Ignore', 2000)
                """.trimIndent()
            )
            migrated.query(
                "SELECT decision FROM app_rules WHERE packageName = 'com.example.app'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Ignore", cursor.getString(0))
            }

            migrated.execSQL(
                "INSERT INTO classification_stats(`key`, count) VALUES('automatic.total', 1)"
            )
            migrated.query(
                "SELECT count FROM classification_stats WHERE `key` = 'automatic.total'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
            }
        } finally {
            migrated.close()
        }
    }
}
