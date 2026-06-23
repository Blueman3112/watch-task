package com.example.test0512.mobile.data

import android.content.Context
import android.util.Log
import com.example.test0512.mobile.model.RadarTask
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MobileTaskSyncManager(
    private val context: Context,
    private val taskDao: TaskDao
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    fun syncAllTasksToWatch() {
        scope.launch {
            try {
                val tasks = taskDao.getAllTasksSync()
                val jsonStr = gson.toJson(tasks)
                
                val putDataMapReq = PutDataMapRequest.create("/sync_tasks")
                putDataMapReq.dataMap.putString("tasks_json", jsonStr)
                putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
                
                val putDataReq = putDataMapReq.asPutDataRequest()
                putDataReq.setUrgent()
                
                dataClient.putDataItem(putDataReq).addOnSuccessListener {
                    Log.d("MobileTaskSyncManager", "Successfully sent ${tasks.size} tasks to watch")
                }.addOnFailureListener {
                    Log.e("MobileTaskSyncManager", "Failed to send tasks to watch", it)
                }
            } catch (e: Exception) {
                Log.e("MobileTaskSyncManager", "Error syncing tasks", e)
            }
        }
    }

    fun sendClearAllToWatch() {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, "/clear_all", ByteArray(0))
                    .addOnSuccessListener {
                        Log.d("MobileTaskSyncManager", "Successfully sent clear_all request to watch: ${node.id}")
                    }.addOnFailureListener {
                        Log.e("MobileTaskSyncManager", "Failed to send clear_all request", it)
                    }
            }
        }
    }

    private fun requestSyncFromWatch() {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, "/request_sync", ByteArray(0))
                    .addOnSuccessListener {
                        Log.d("MobileTaskSyncManager", "Successfully sent sync request to watch: ${node.id}")
                    }.addOnFailureListener {
                        Log.e("MobileTaskSyncManager", "Failed to send sync request", it)
                    }
            }
        }
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
