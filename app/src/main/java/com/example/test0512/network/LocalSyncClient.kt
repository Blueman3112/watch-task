package com.example.test0512.network

import android.util.Log
import com.example.test0512.data.TaskDao
import com.example.test0512.model.RadarTask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocalSyncClient(private val taskDao: TaskDao) {
    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 15_000
        }
    }
    private var session: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private var connectJob: Job? = null
    private val _connectionState = MutableStateFlow(false)
    val connectionState = _connectionState.asStateFlow()

    fun connect(host: String, port: Int) {
        if (_connectionState.value) return
        connectJob?.cancel()
        connectJob = scope.launch {
            while (isActive && !_connectionState.value) {
                try {
                    Log.d("LocalSyncClient", "Attempting to connect to ws://$host:$port/sync")
                    client.webSocket(method = HttpMethod.Get, host = host, port = port, path = "/sync") {
                        _connectionState.value = true
                        session = this
                        Log.d("LocalSyncClient", "Connected successfully")
                        
                        // Listen for incoming messages
                        for (message in incoming) {
                            if (message is Frame.Text) {
                                val text = message.readText()
                                handleMessage(text)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocalSyncClient", "Connection error", e)
                } finally {
                    _connectionState.value = false
                    session = null
                    Log.d("LocalSyncClient", "Disconnected, retrying in 5 seconds...")
                    delay(5000)
                }
            }
        }
    }

    private suspend fun handleMessage(text: String) {
        try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val jsonMap: Map<String, Any> = gson.fromJson(text, mapType)
            
            when (jsonMap["type"] as? String) {
                "sync_tasks" -> {
                    val dataJson = gson.toJson(jsonMap["data"])
                    val listType = object : TypeToken<List<RadarTask>>() {}.type
                    val incomingTasks: List<RadarTask> = gson.fromJson(dataJson, listType)
                    
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
                    Log.d("LocalSyncClient", "Processed incoming tasks. Inserted: ${tasksToInsert.size}, Updated: ${tasksToUpdate.size}")
                }
                "clear_all" -> {
                    taskDao.deleteAllTasks()
                    Log.d("LocalSyncClient", "Cleared all tasks based on request")
                }
            }
        } catch (e: Exception) {
            Log.e("LocalSyncClient", "Error handling message", e)
        }
    }

    fun requestSync() {
        scope.launch {
            try {
                session?.send(Frame.Text("{\"type\":\"request_sync\"}"))
            } catch (e: Exception) {
                Log.e("LocalSyncClient", "Failed to send request_sync", e)
            }
        }
    }

    fun syncAllTasksToPhone() {
        scope.launch {
            try {
                val tasks = taskDao.getAllTasksSync()
                val message = "{\"type\":\"sync_tasks\", \"data\":${gson.toJson(tasks)}}"
                session?.send(Frame.Text(message))
            } catch (e: Exception) {
                Log.e("LocalSyncClient", "Failed to send tasks to phone", e)
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        scope.launch {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnected"))
            session = null
            _connectionState.value = false
        }
    }
}
