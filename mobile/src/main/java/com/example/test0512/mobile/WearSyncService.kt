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

class WearSyncService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == "/sync_calendar") {
            Log.d("WearSyncService", "Received sync request from watch")
            syncCalendarToWatch()
        }
    }

    private fun syncCalendarToWatch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w("WearSyncService", "Calendar permission not granted")
            return
        }

        val eventsArray = JSONArray()
        
        val now = Calendar.getInstance()
        val beginTime = now.timeInMillis
        now.add(Calendar.DAY_OF_YEAR, 7) // Sync next 7 days
        val endTime = now.timeInMillis

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.ALL_DAY
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, beginTime)
        android.content.ContentUris.appendId(builder, endTime)

        val selection = "${CalendarContract.Instances.ALL_DAY} = ?"
        val selectionArgs = arrayOf("0")

        val cursor = contentResolver.query(
            builder.build(),
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Instances.BEGIN} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIndex = it.getColumnIndex(CalendarContract.Instances.END)
            val descIndex = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)

            while (it.moveToNext()) {
                val eventId = it.getString(idIndex)
                val title = it.getString(titleIndex)
                val begin = it.getLong(beginIndex)
                val end = it.getLong(endIndex)
                val desc = if (descIndex != -1) it.getString(descIndex) else ""

                val eventObj = JSONObject().apply {
                    put("id", eventId)
                    put("title", title ?: "无标题日程")
                    put("begin", begin)
                    put("end", end)
                    put("description", desc ?: "")
                }
                eventsArray.put(eventObj)
            }
        }

        // Send via DataLayer
        val dataClient: DataClient = Wearable.getDataClient(this)
        val putDataMapReq = PutDataMapRequest.create("/calendar_events")
        putDataMapReq.dataMap.putString("events_json", eventsArray.toString())
        putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis()) // Ensure onDataChanged triggers
        
        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()
        
        dataClient.putDataItem(putDataReq).addOnSuccessListener {
            Log.d("WearSyncService", "Successfully sent ${eventsArray.length()} events to watch")
        }.addOnFailureListener {
            Log.e("WearSyncService", "Failed to send events", it)
        }
    }
}
