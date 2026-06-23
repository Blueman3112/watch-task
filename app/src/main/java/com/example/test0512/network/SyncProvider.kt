package com.example.test0512.network

import android.content.Context
import com.example.test0512.data.TaskDao

object SyncProvider {
    lateinit var syncClient: LocalSyncClient
    
    fun init(context: Context, taskDao: TaskDao) {
        if (!this::syncClient.isInitialized) {
            syncClient = LocalSyncClient(taskDao)
        }
    }
}
