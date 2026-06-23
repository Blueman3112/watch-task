package com.example.test0512.mobile.network

import android.content.Context
import com.example.test0512.mobile.data.MobileTaskSyncManager
import com.example.test0512.mobile.data.TaskDao

object SyncProvider {
    lateinit var syncServer: LocalSyncServer
    lateinit var syncManager: MobileTaskSyncManager
    
    fun init(context: Context, taskDao: TaskDao) {
        if (!this::syncServer.isInitialized) {
            syncServer = LocalSyncServer(taskDao)
            syncManager = MobileTaskSyncManager(context, taskDao, syncServer)
        }
    }
}
