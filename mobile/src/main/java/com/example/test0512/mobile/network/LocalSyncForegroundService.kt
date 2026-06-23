package com.example.test0512.mobile.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.test0512.mobile.data.AppDatabase
import com.example.test0512.mobile.data.MobileTaskSyncManager

class LocalSyncForegroundService : Service() {

    private lateinit var syncServer: LocalSyncServer
    private lateinit var nsdManager: NsdServerManager
    lateinit var syncManager: MobileTaskSyncManager
        private set

    companion object {
        private const val CHANNEL_ID = "LocalSyncChannel"
        private const val NOTIFICATION_ID = 1

        var instance: LocalSyncForegroundService? = null
            private set
            
        fun start(context: Context) {
            val intent = Intent(context, LocalSyncForegroundService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, LocalSyncForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        val taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
        SyncProvider.init(applicationContext, taskDao)
        
        syncServer = SyncProvider.syncServer
        syncManager = SyncProvider.syncManager
        nsdManager = NsdServerManager(this)

        val port = 8080
        syncServer.startServer(port)
        nsdManager.registerService(port)
        
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdManager.tearDown()
        syncServer.stopServer()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Local Sync Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WatchTask Bridge")
            .setContentText("局域网同步服务运行中")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .build()
    }
}
