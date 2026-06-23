package com.example.test0512.service

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
import com.example.test0512.data.AppDatabase
import com.example.test0512.network.LocalSyncClient
import com.example.test0512.network.NsdClientDiscovery

class TaskDataListenerService : Service() {

    private lateinit var syncClient: LocalSyncClient
    private lateinit var nsdDiscovery: NsdClientDiscovery

    companion object {
        private const val CHANNEL_ID = "WatchSyncChannel"
        private const val NOTIFICATION_ID = 2

        fun start(context: Context) {
            val intent = Intent(context, TaskDataListenerService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TaskDataListenerService::class.java)
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
        com.example.test0512.network.SyncProvider.init(applicationContext, taskDao)
        syncClient = com.example.test0512.network.SyncProvider.syncClient
        nsdDiscovery = NsdClientDiscovery(applicationContext)

        nsdDiscovery.discoverServices { host, port ->
            syncClient.connect(host, port)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdDiscovery.stopDiscovery()
        syncClient.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Watch Sync Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WatchTask")
            .setContentText("局域网同步服务运行中")
            // Use a default icon since we don't know if specific icons exist
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .build()
    }
}
