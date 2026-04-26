package com.kkaloai.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BiofeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BiofeedbackEntry)

    @Query("SELECT * FROM biofeedback_entries WHERE timestamp >= :startOfDay ORDER BY timestamp DESC LIMIT 1")
    fun getLatestForDay(startOfDay: Long): Flow<BiofeedbackEntry?>

    @Query("SELECT * FROM biofeedback_entries WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun getBetweenSync(start: Long, end: Long): List<BiofeedbackEntry>
}
