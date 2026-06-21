package com.example.test0512.data

import android.content.Context
import android.util.Log
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import com.example.test0512.model.TaskSource
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CalendarSyncManager(
    private val context: Context,
    private val taskDao: TaskDao
) : DataClient.OnDataChangedListener {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    fun startListening() {
        dataClient.addListener(this)
    }

    fun stopListening() {
        dataClient.removeListener(this)
    }

    fun requestSync() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, "/sync_calendar", ByteArray(0)).addOnSuccessListener {
                    Log.d("CalendarSyncManager", "Sent sync request to node ${node.id}")
                }.addOnFailureListener { e ->
                    Log.e("CalendarSyncManager", "Failed to send sync request to node ${node.id}", e)
                }
            }
        }.addOnFailureListener { e ->
            Log.e("CalendarSyncManager", "Failed to get connected nodes", e)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/calendar_events") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val jsonStr = dataMapItem.dataMap.getString("events_json")
                if (jsonStr != null) {
                    processEventsJson(jsonStr)
                }
            }
        }
    }

    private fun processEventsJson(jsonStr: String) {
        scope.launch {
            try {
                val jsonArray = JSONArray(jsonStr)
                val existingTasks = taskDao.getAllTasksSync() // Need to add this or query by externalId
                val existingExternalIds = existingTasks.mapNotNull { it.externalId }.toSet()

                val newTasks = mutableListOf<RadarTask>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val eventId = jsonObj.getString("id")

                    if (!existingExternalIds.contains(eventId)) {
                        var title = jsonObj.getString("title")
                        val begin = jsonObj.getLong("begin")
                        val end = jsonObj.getLong("end")
                        val desc = jsonObj.getString("description")

                        var parsedPriority = TaskPriority.IMPORTANT
                        val tags = listOf(
                            "!紧急" to TaskPriority.EMERGENCY, "！紧急" to TaskPriority.EMERGENCY,
                            "!常规" to TaskPriority.REGULAR, "！常规" to TaskPriority.REGULAR,
                            "!长远" to TaskPriority.LONG_TERM, "！长远" to TaskPriority.LONG_TERM,
                            "!重要" to TaskPriority.IMPORTANT, "！重要" to TaskPriority.IMPORTANT
                        )
                        for ((tag, prio) in tags) {
                            if (title.contains(tag)) {
                                parsedPriority = prio
                                title = title.replace(tag, "").trim()
                                break
                            }
                        }

                        val timeStr = timeFormat.format(Date(begin))

                        val task = RadarTask(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            time = timeStr,
                            dueDate = begin,
                            description = desc,
                            priority = parsedPriority,
                            source = TaskSource.CALENDAR,
                            externalId = eventId
                        )
                        newTasks.add(task)
                    }
                }

                if (newTasks.isNotEmpty()) {
                    taskDao.insertTasks(newTasks)
                    Log.d("CalendarSyncManager", "Inserted ${newTasks.size} new calendar tasks")
                }

            } catch (e: Exception) {
                Log.e("CalendarSyncManager", "Error parsing events json", e)
            }
        }
    }
}
