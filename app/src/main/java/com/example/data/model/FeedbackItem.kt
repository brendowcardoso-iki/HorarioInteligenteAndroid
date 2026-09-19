package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedbacks")
data class FeedbackItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityName: String,
    val startTime: String,
    val endTime: String,
    val feedbackText: String,
    val rating: Int,
    val createdAt: Long = System.currentTimeMillis()
)
