package com.example.test0512.mobile.data

import android.content.Context
import android.util.Log
import com.example.test0512.mobile.model.RadarTask
import com.example.test0512.mobile.network.LocalSyncServer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MobileTaskSyncManager(
    private val context: Context,
    private val taskDao: TaskDao,
    private val localSyncServer: LocalSyncServer
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    fun syncAllTasksToWatch() {
        scope.launch {
            try {
                val tasks = taskDao.getAllTasksSync()
                localSyncServer.broadcastTasks(tasks)
                Log.d("MobileTaskSyncManager", "Successfully sent ${tasks.size} tasks to watch via LAN")
            } catch (e: Exception) {
                Log.e("MobileTaskSyncManager", "Error syncing tasks", e)
            }
        }
    }

    fun sendClearAllToWatch() {
        localSyncServer.broadcastClearAll()
        Log.d("MobileTaskSyncManager", "Successfully sent clear_all request to watch via LAN")
    }

    private fun requestSyncFromWatch() {
        // With WebSocket, we might not need this if the connection maintains state,
        // but if we want to explicitly ask the watch for new data, we can send a request:
        // For simplicity, we just rely on the watch sending data when it connects or changes.
    }

    fun performTwoWaySync() {
        syncAllTasksToWatch()
        requestSyncFromWatch()
    }

    suspend fun processIncomingTasks(jsonStr: String) {
        try {
            val listType = object : TypeToken<List<RadarTask>>() {}.type
            val incomingTasks: List<RadarTask> = gson.fromJson(jsonStr, listType)
            
            val localTasks = taskDao.getAllTasksSync().associateBy { it.id }
            val tasksToUpdate = mutableListOf<RadarTask>()
            val tasksToInsert = mutableListOf<RadarTask>()

            for (incoming in incomingTasks) {
                val local = localTasks[incoming.id]
                if (local == null) {
                    tasksToInsert.add(incoming)
                } else if (incoming.updatedAt > local.updatedAt) {
                    tasksToUpdate.add(incoming)
                }
            }

            if (tasksToInsert.isNotEmpty()) {
                taskDao.insertTasks(tasksToInsert)
            }
            if (tasksToUpdate.isNotEmpty()) {
                tasksToUpdate.forEach { taskDao.updateTask(it) }
            }
            Log.d("MobileTaskSyncManager", "Processed incoming tasks. Inserted: ${tasksToInsert.size}, Updated: ${tasksToUpdate.size}")
        } catch (e: Exception) {
            Log.e("MobileTaskSyncManager", "Error parsing incoming tasks", e)
        }
    }
}
