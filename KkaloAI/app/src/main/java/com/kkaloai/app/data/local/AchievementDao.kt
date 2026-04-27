package com.kkaloai.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(entry: AchievementEntry): Long

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun getAllUnlocked(): Flow<List<AchievementEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM achievements WHERE code = :code)")
    suspend fun isUnlocked(code: String): Boolean

    @Query("SELECT COUNT(*) FROM achievements")
    fun count(): Flow<Int>
}
