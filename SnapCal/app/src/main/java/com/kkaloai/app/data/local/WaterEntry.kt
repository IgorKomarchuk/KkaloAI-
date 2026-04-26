package com.kkaloai.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amountMl: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
