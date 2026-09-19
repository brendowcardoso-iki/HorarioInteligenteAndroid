package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivityCustomization
import com.example.data.model.FeedbackItem
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_items ORDER BY startTime ASC, endTime ASC")
    fun getAllScheduleItems(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items ORDER BY startTime ASC, endTime ASC")
    suspend fun getAllScheduleItemsSync(): List<ScheduleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItem(item: ScheduleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllScheduleItems(items: List<ScheduleItem>)

    @Update
    suspend fun updateScheduleItem(item: ScheduleItem)

    @Delete
    suspend fun deleteScheduleItem(item: ScheduleItem)

    @Query("DELETE FROM schedule_items")
    suspend fun clearAllScheduleItems()

    @Query("DELETE FROM schedule_items WHERE category = :name OR sourceFile = :name")
    suspend fun deleteItemsBySourceOrCategory(name: String)

    @Query("SELECT * FROM activity_customizations WHERE activityKey = :key LIMIT 1")
    fun getCustomization(key: String): Flow<ActivityCustomization?>

    @Query("SELECT * FROM activity_customizations")
    fun getAllCustomizations(): Flow<List<ActivityCustomization>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomization(customization: ActivityCustomization)

    @Query("SELECT * FROM feedbacks ORDER BY createdAt DESC")
    fun getAllFeedbacks(): Flow<List<FeedbackItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackItem): Long

    @Query("DELETE FROM feedbacks WHERE id = :id")
    suspend fun deleteFeedback(id: Long)
}
