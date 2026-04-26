package com.kkaloai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MealEntry::class, WaterEntry::class, BiofeedbackEntry::class, AchievementEntry::class],
    version = 4,
    exportSchema = false
)
abstract class KkaloAiDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun waterDao(): WaterDao
    abstract fun biofeedbackDao(): BiofeedbackDao
    abstract fun achievementDao(): AchievementDao
}
