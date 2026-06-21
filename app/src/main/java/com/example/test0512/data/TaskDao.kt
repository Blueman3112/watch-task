package com.example.test0512.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.test0512.model.RadarTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    fun getAllTasks(): Flow<List<RadarTask>>

    @Query("UPDATE tasks SET isCompleted = 1, updatedAt = :updateTime WHERE id = :taskId")
    fun markTaskAsCompleted(taskId: String, updateTime: Long)

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

    @Query("UPDATE tasks SET isPinned = 0")
    fun unpinAllTasks()

    @Query("UPDATE tasks SET isPinned = 1 WHERE id = :taskId")
    fun pinTask(taskId: String)

    @Delete
    fun deleteTask(task: RadarTask)
}
