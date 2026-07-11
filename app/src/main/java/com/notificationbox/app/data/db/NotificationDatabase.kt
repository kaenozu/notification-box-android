package com.notificationbox.app.data.db

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
    abstract fun notificationDao(): NotificationDao

    companion object {
        fun create(context: Context): NotificationDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotificationDatabase::class.java,
                "notification-box.db"
            ).build()
    }
}
