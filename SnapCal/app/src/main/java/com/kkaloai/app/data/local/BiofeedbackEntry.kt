package com.kkaloai.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biofeedback_entries")
data class BiofeedbackEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val energyLevel: Int = 3,
    val mood: String = "ok",
    val symptoms: String = ""
)
