package com.notificationbox.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NotificationEntity::class,
        AppRuleEntity::class,
        ClassificationStatEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun classificationStatsDao(): ClassificationStatsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE notifications ADD COLUMN userDecision TEXT"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_packageName " +
                        "ON notifications(packageName)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_rules (
                        packageName TEXT NOT NULL PRIMARY KEY,
                        appLabel TEXT NOT NULL,
                        decision TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS classification_stats (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        count INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE notifications " +
                        "ADD COLUMN contentAvailability TEXT NOT NULL DEFAULT 'AVAILABLE'"
                )
            }
        }

        fun create(context: Context): NotificationDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotificationDatabase::class.java,
                "notification-box.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
