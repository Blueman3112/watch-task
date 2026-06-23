package com.example.test0512.service

import com.example.test0512.data.AppDatabase
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import com.example.test0512.model.TaskSource
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        
        for (event in dataEvents) {
            if (event.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == "/new_task") {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val dataMap = dataMapItem.dataMap
                    
                    val title = dataMap.getString("title") ?: "新任务"
                    val desc = dataMap.getString("description") ?: ""
                    val time = dataMap.getString("time") ?: "未指定时间"
                    val priorityStr = dataMap.getString("priority") ?: TaskPriority.REGULAR.name
                    
                    val priority = try {
                        TaskPriority.valueOf(priorityStr)
                    } catch (e: Exception) {
                        TaskPriority.REGULAR
                    }

                    // For simplicity, just use negative timestamp as sort order to place it at the top
                    val topSortOrder = -(System.currentTimeMillis() / 1000).toInt()

                    val newTask = RadarTask(
                        title = title,
                        description = desc,
                        time = time,
                        priority = priority,
                        source = TaskSource.WECHAT,
                        sortOrder = topSortOrder
                    )

                    scope.launch {
                        val db = AppDatabase.getDatabase(applicationContext)
                        db.taskDao().insertTask(newTask)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        val db = AppDatabase.getDatabase(applicationContext)
        if (messageEvent.path == "/request_sync") {
            val syncManager = com.example.test0512.data.WearTaskSyncManager(applicationContext, db.taskDao())
            syncManager.syncAllTasksToPhone()
        } else if (messageEvent.path == "/clear_all") {
            scope.launch {
                db.taskDao().deleteAllTasks()
            }
        }
    }
}
