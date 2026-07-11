package com.notificationbox.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY postTimeMillis DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE key = :key")
    suspend fun getByKey(key: String): NotificationEntity?

    @Query("UPDATE notifications SET userPinned = :pinned WHERE key = :key")
    suspend fun setPinned(key: String, pinned: Boolean)

    @Query("UPDATE notifications SET isActive = 0, removedAtMillis = :removedAtMillis WHERE key = :key")
    suspend fun markRemoved(key: String, removedAtMillis: Long)

    @Query("DELETE FROM notifications WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM notifications WHERE userPinned = 0 AND postTimeMillis < :cutoffMillis")
    suspend fun deleteOldUnpinned(cutoffMillis: Long): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE userPinned = 0")
    suspend fun countUnpinned(): Int

    @Query("DELETE FROM notifications WHERE key IN (SELECT key FROM notifications WHERE userPinned = 0 ORDER BY postTimeMillis ASC LIMIT :excess)")
    suspend fun deleteOldestUnpinned(excess: Int): Int
}
