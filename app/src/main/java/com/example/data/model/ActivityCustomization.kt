package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_customizations")
data class ActivityCustomization(
    @PrimaryKey
    val activityKey: String,
    val objective: String = "",
    val stepsJson: String = "[]"
)
