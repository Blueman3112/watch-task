package com.example.test0512.mobile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.test0512.mobile.model.RadarTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isDeleted = 0")
    fun getAllTasks(): Flow<List<RadarTask>>

    @Query("SELECT * FROM tasks")
    fun getAllTasksSync(): List<RadarTask>

    @Query("UPDATE tasks SET isCompleted = 1, updatedAt = MAX(:updateTime, updatedAt + 1) WHERE id = :taskId")
    fun markTaskAsCompleted(taskId: String, updateTime: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: RadarTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTasks(tasks: List<RadarTask>)

    @Update
    fun updateTask(task: RadarTask)

    @Query("UPDATE tasks SET isCompleted = 0")
    fun uncompleteAllTasks()

    @Query("DELETE FROM tasks")
    fun deleteAllTasks()

    @Query("UPDATE tasks SET isPinned = 0, updatedAt = MAX(:updateTime, updatedAt + 1) WHERE isPinned = 1")
    fun unpinAllTasks(updateTime: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isPinned = 1, updatedAt = MAX(:updateTime, updatedAt + 1) WHERE id = :taskId")
    fun pinTask(taskId: String, updateTime: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = MAX(:updateTime, updatedAt + 1) WHERE id = :taskId")
    fun softDeleteTask(taskId: String, updateTime: Long = System.currentTimeMillis())

    @Delete
    fun deleteTask(task: RadarTask)
}
