package com.notificationbox.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY postTimeMillis DESC, `key` DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT
            COUNT(*) AS totalCount,
            COALESCE(SUM(CASE WHEN category = 'KeepNow' THEN 1 ELSE 0 END), 0) AS keepNowCount,
            COALESCE(SUM(CASE WHEN category = 'HoldForDigest' THEN 1 ELSE 0 END), 0) AS holdForDigestCount,
            COALESCE(SUM(CASE WHEN category = 'Ignore' THEN 1 ELSE 0 END), 0) AS ignoreCount
        FROM notifications
        WHERE postTimeMillis >= :sinceMillis
        """
    )
    fun observeSummarySince(sinceMillis: Long): Flow<NotificationSummaryRow>

    @Query("SELECT * FROM notifications WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): NotificationEntity?

    @Query("SELECT * FROM notifications ORDER BY postTimeMillis DESC, `key` DESC")
    suspend fun getAllOnce(): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entity: NotificationEntity)

    @Query("UPDATE notifications SET userDecision = :decision WHERE `key` = :key")
    suspend fun setUserDecision(key: String, decision: String?): Int

    @Query("UPDATE notifications SET userPinned = :pinned WHERE `key` = :key")
    suspend fun setPinned(key: String, pinned: Boolean): Int

    @Query("UPDATE notifications SET isActive = 0, removedAtMillis = :removedAtMillis WHERE `key` = :key")
    suspend fun markRemoved(key: String, removedAtMillis: Long): Int

    @Query("UPDATE notifications SET isActive = 0, removedAtMillis = :removedAtMillis WHERE isActive = 1")
    suspend fun markAllActiveRemoved(removedAtMillis: Long): Int

    @Query(
        """
        UPDATE notifications
        SET isActive = 0, removedAtMillis = :removedAtMillis
        WHERE isActive = 1 AND `key` NOT IN (:activeKeys)
        """
    )
    suspend fun markActiveMissing(activeKeys: List<String>, removedAtMillis: Long): Int

    @Query("DELETE FROM notifications WHERE `key` = :key")
    suspend fun deleteByKey(key: String): Int

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query(
        """
        DELETE FROM notifications
        WHERE userPinned = 0
          AND isActive = 0
          AND postTimeMillis < :cutoffMillis
        """
    )
    suspend fun deleteExpired(cutoffMillis: Long): Int

    @Query(
        """
        DELETE FROM notifications
        WHERE `key` IN (
            SELECT `key`
            FROM notifications
            WHERE userPinned = 0
            ORDER BY isActive ASC, postTimeMillis ASC, `key` ASC
            LIMIT MAX((SELECT COUNT(*) FROM notifications) - :maxCount, 0)
        )
        """
    )
    suspend fun pruneToMaximum(maxCount: Int): Int
}
