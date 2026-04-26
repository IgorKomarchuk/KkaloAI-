package com.kkaloai.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val calories: Int = 0,
    val proteins: Float = 0f,
    val carbs: Float = 0f,
    val fats: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val isPlanned: Boolean = false
)
