package com.example.test0512.data

import android.content.Context
import android.util.Log
import com.example.test0512.model.RadarTask
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearTaskSyncManager(
    private val context: Context,
    private val taskDao: TaskDao
) : DataClient.OnDataChangedListener {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    fun startListening() {
        dataClient.addListener(this)
    }

    fun stopListening() {
        dataClient.removeListener(this)
    }

    fun syncAllTasksToPhone() {
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
                    Log.d("WearTaskSyncManager", "Successfully sent ${tasks.size} tasks to phone")
                }.addOnFailureListener {
                    Log.e("WearTaskSyncManager", "Failed to send tasks to phone", it)
                }
            } catch (e: Exception) {
                Log.e("WearTaskSyncManager", "Error syncing tasks", e)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/sync_tasks") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val jsonStr = dataMapItem.dataMap.getString("tasks_json")
                if (jsonStr != null) {
                    processIncomingTasks(jsonStr)
                }
            }
        }
    }

    private fun processIncomingTasks(jsonStr: String) {
        scope.launch {
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
                Log.d("WearTaskSyncManager", "Processed incoming tasks. Inserted: ${tasksToInsert.size}, Updated: ${tasksToUpdate.size}")
            } catch (e: Exception) {
                Log.e("WearTaskSyncManager", "Error parsing incoming tasks", e)
            }
        }
    }
}
