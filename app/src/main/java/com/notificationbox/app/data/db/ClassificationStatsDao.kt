package com.notificationbox.app.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassificationStatsDao {
    @Query("SELECT * FROM classification_stats")
    fun observeAll(): Flow<List<ClassificationStatEntity>>

    @Query(
        """
        INSERT INTO classification_stats(`key`, count)
        VALUES(:key, 1)
        ON CONFLICT(`key`) DO UPDATE SET count = count + 1
        """
    )
    suspend fun increment(key: String)
}
