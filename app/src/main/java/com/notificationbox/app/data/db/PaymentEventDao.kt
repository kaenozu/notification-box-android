package com.notificationbox.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentEventDao {
    @Query(
        """
        SELECT * FROM payment_events
        ORDER BY occurredAtMillis DESC, sourceNotificationKey DESC
        LIMIT 500
        """
    )
    fun observeAll(): Flow<List<PaymentEventEntity>>

    @Query(
        """
        SELECT
            COUNT(*) AS eventCount,
            COALESCE(
                SUM(CASE WHEN transactionType = 'PURCHASE' THEN amountYen ELSE 0 END),
                0
            ) AS purchaseTotalYen,
            COALESCE(
                SUM(CASE WHEN transactionType = 'REFUND' THEN amountYen ELSE 0 END),
                0
            ) AS refundTotalYen,
            COALESCE(
                SUM(CASE WHEN transactionType = 'UNKNOWN' THEN 1 ELSE 0 END),
                0
            ) AS needsReviewCount
        FROM payment_events
        WHERE occurredAtMillis >= :sinceMillis
        """
    )
    fun observeSummarySince(sinceMillis: Long): Flow<PaymentSummaryRow>

    @Upsert
    suspend fun upsert(entity: PaymentEventEntity)

    @Query("DELETE FROM payment_events")
    suspend fun clearAll()
}
