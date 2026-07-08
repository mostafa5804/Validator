package com.example.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM validation_history ORDER BY timestamp DESC LIMIT 5")
    fun getRecentHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HistoryItem)

    @Query("DELETE FROM validation_history WHERE id NOT IN (SELECT id FROM validation_history ORDER BY timestamp DESC LIMIT 5)")
    suspend fun cleanupOldEntries()
}
