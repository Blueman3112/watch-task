package com.example.test0512.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import com.example.test0512.mobile.data.AppDatabase
import com.example.test0512.mobile.data.MobileTaskSyncManager
import kotlinx.coroutines.launch

class WearSyncService : WearableListenerService() {

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_FORCE_SYNC") {
            Log.d("WearSyncService", "Force sync triggered from phone UI")
            val taskDao = AppDatabase.getDatabase(this.applicationContext).taskDao()
            MobileTaskSyncManager(this, taskDao).syncAllTasksToWatch()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDataChanged(dataEvents: com.google.android.gms.wearable.DataEventBuffer) {
        val taskDao = AppDatabase.getDatabase(this.applicationContext).taskDao()
        val syncManager = MobileTaskSyncManager(this, taskDao)
        
        dataEvents.forEach { event ->
            if (event.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/sync_tasks") {
                val dataMapItem = com.google.android.gms.wearable.DataMapItem.fromDataItem(event.dataItem)
                val jsonStr = dataMapItem.dataMap.getString("tasks_json")
                if (jsonStr != null) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        syncManager.processIncomingTasks(jsonStr)
                    }
                }
            }
        }
    }
}
