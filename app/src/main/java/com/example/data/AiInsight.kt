package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_insights")
data class AiInsight(
    @PrimaryKey val id: Int = 1,
    val overallAdvice: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
