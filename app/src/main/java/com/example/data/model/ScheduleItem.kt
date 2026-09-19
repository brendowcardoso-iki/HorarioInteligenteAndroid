package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: String, // HH:mm
    val endTime: String,   // HH:mm
    val activity: String,
    val instructions: String = "",
    val category: String = "Outros",
    val sourceFile: String = "Protocolo Principal",
    val isCompleted: Boolean = false
) {
    val categoryEnum: ActivityCategory
        get() = ActivityCategory.fromString(category)

    val key: String
        get() = "$activity-$startTime-$endTime"
}
