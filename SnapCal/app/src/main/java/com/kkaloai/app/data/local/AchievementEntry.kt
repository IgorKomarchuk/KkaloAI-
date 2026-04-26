package com.kkaloai.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntry(
    @PrimaryKey
    val code: String,
    val unlockedAt: Long,
    val progress: Int = 0
)
