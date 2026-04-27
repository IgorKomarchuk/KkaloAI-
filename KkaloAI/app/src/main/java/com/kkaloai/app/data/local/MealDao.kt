package com.kkaloai.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntry)

    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startOfDay AND isPlanned = 0 ORDER BY timestamp DESC")
    fun getMealsForDay(startOfDay: Long): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startOfDay AND isPlanned = 1 ORDER BY timestamp ASC")
    fun getPlannedForDay(startOfDay: Long): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getMealsBetween(start: Long, end: Long): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE timestamp >= :start AND timestamp <= :end AND isPlanned = 0 ORDER BY timestamp ASC")
    suspend fun getMealsBetweenSync(start: Long, end: Long): List<MealEntry>

    @Query("SELECT SUM(calories) FROM meal_entries WHERE timestamp >= :startOfDay AND isPlanned = 0")
    fun getTotalCaloriesForDay(startOfDay: Long): Flow<Int?>

    @Query("DELETE FROM meal_entries WHERE id = :mealId")
    suspend fun deleteMeal(mealId: Int)

    @Query("UPDATE meal_entries SET isFavorite = :favorite WHERE id = :mealId")
    suspend fun setFavorite(mealId: Int, favorite: Boolean)

    @Query("UPDATE meal_entries SET isPlanned = 0, timestamp = :now WHERE id = :mealId")
    suspend fun markPlannedEaten(mealId: Int, now: Long)

    @Query("SELECT * FROM meal_entries WHERE isFavorite = 1 GROUP BY name ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE timestamp >= :start AND timestamp <= :end AND isPlanned = 0")
    suspend fun getRangeForStats(start: Long, end: Long): List<MealEntry>

    @Query("SELECT COUNT(*) FROM meal_entries WHERE isPlanned = 0")
    suspend fun countAll(): Int
}
