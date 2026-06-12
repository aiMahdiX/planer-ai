package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val category: String = "شخصی", // e.g. کار (Work), شخصی (Personal), آموزش (Education), سلامتی (Health), سایر (Other)
    val dueDate: Long = System.currentTimeMillis(),
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val aiPriorityScore: Int = 0,    // 0 to 100. Lower number means lower AI priority, higher means high.
    val aiReasoning: String = "",    // Persian explanation of why it is prioritized
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 30,  // Estimated duration in minutes
    val createdAt: Long = System.currentTimeMillis()
)
