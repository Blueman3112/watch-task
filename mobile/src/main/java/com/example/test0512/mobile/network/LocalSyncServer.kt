package com.example.test0512.mobile.network

import android.util.Log
import com.example.test0512.mobile.data.TaskDao
import com.example.test0512.mobile.model.RadarTask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.Collections
import kotlin.collections.LinkedHashSet

class LocalSyncServer(private val taskDao: TaskDao) {
    private var server: NettyApplicationEngine? = null
    private val connections = Collections.synchronizedSet<DefaultWebSocketServerSession>(LinkedHashSet())
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // For letting the UI or manager know when we received tasks
    private val _incomingTasksFlow = MutableSharedFlow<List<RadarTask>>()
    val incomingTasksFlow = _incomingTasksFlow.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    fun startServer(port: Int = 8080) {
        if (server != null) return
        
        server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket("/sync") {
                    Log.d("LocalSyncServer", "Client connected: ${call.request.local.remoteAddress}")
                    connections.add(this)
                    _isConnected.value = connections.isNotEmpty()
                    
                    try {
                        // Immediately send all current tasks to the newly connected client
                        val allTasks = taskDao.getAllTasksSync()
                        send(Frame.Text("{\"type\":\"sync_tasks\", \"data\":${gson.toJson(allTasks)}}"))

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleMessage(this, text)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LocalSyncServer", "WebSocket error", e)
                    } finally {
                        Log.d("LocalSyncServer", "Client disconnected: ${call.request.local.remoteAddress}")
                        connections.remove(this)
                        _isConnected.value = connections.isNotEmpty()
                    }
                }
            }
        }.start(wait = false)
        Log.d("LocalSyncServer", "Server started on port $port")
    }

    private suspend fun handleMessage(session: DefaultWebSocketServerSession, text: String) {
        try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val jsonMap: Map<String, Any> = gson.fromJson(text, mapType)
            
            when (jsonMap["type"] as? String) {
                "request_sync" -> {
                    val allTasks = taskDao.getAllTasksSync()
                    session.send(Frame.Text("{\"type\":\"sync_tasks\", \"data\":${gson.toJson(allTasks)}}"))
                }
                "sync_tasks" -> {
                    // Watch sends tasks to phone
                    val dataJson = gson.toJson(jsonMap["data"])
                    val listType = object : TypeToken<List<RadarTask>>() {}.type
                    val tasks: List<RadarTask> = gson.fromJson(dataJson, listType)
                    _incomingTasksFlow.emit(tasks)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalSyncServer", "Error handling message", e)
        }
    }

    fun broadcastTasks(tasks: List<RadarTask>) {
        val message = "{\"type\":\"sync_tasks\", \"data\":${gson.toJson(tasks)}}"
        scope.launch {
            connections.forEach {
                try {
                    it.send(Frame.Text(message))
                } catch (e: Exception) {
                    Log.e("LocalSyncServer", "Failed to send to client", e)
                }
            }
        }
    }

    fun broadcastClearAll() {
        val message = "{\"type\":\"clear_all\"}"
        scope.launch {
            connections.forEach {
                try {
                    it.send(Frame.Text(message))
                } catch (e: Exception) {
                    Log.e("LocalSyncServer", "Failed to send clear_all to client", e)
                }
            }
        }
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
}
