package com.kkaloai.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(entry: WaterEntry)

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE timestamp >= :startOfDay")
    fun getTotalForDay(startOfDay: Long): Flow<Int>

    @Query("DELETE FROM water_entries WHERE timestamp >= :startOfDay")
    suspend fun clearDay(startOfDay: Long)
}
