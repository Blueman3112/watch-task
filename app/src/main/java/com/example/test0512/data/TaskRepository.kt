package com.example.test0512.data

import com.example.test0512.model.RadarTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<RadarTask>> = taskDao.getAllTasks()

    suspend fun insertTask(task: RadarTask) {
        withContext(Dispatchers.IO) {
            taskDao.insertTask(task)
        }
    }

    suspend fun uncompleteAllTasks() {
        withContext(Dispatchers.IO) {
            taskDao.uncompleteAllTasks()
        }
    }

    suspend fun restoreInitialData() {
        withContext(Dispatchers.IO) {
            taskDao.deleteAllTasks()
            AppDatabase.populateDatabase(taskDao)
        }
    }

    suspend fun pinTask(taskId: String) {
        withContext(Dispatchers.IO) {
            taskDao.unpinAllTasks()
            taskDao.pinTask(taskId)
        }
    }

    suspend fun updateTask(task: RadarTask) {
        withContext(Dispatchers.IO) {
            taskDao.updateTask(task)
        }
    }

    suspend fun deleteTask(task: RadarTask) {
        withContext(Dispatchers.IO) {
            taskDao.deleteTask(task)
        }
    }

    suspend fun markTaskAsCompleted(taskId: String) {
        withContext(Dispatchers.IO) {
            taskDao.markTaskAsCompleted(taskId, System.currentTimeMillis())
        }
    }
}
