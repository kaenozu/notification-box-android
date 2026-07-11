package com.notificationbox.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NotificationDatabase : RoomDatabase() {

    abstract fun dao(): NotificationDao

    companion object {
        @Volatile private var instance: NotificationDatabase? = null

        fun getInstance(context: Context): NotificationDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_box_db"
                ).build().also { instance = it }
            }
        }
    }
}
