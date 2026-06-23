package com.example.test0512.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.test0512.mobile.data.AppDatabase
import com.example.test0512.mobile.data.MobileTaskSyncManager
import com.example.test0512.mobile.data.TaskRepository
import com.example.test0512.mobile.model.RadarTask
import com.example.test0512.mobile.model.TaskPriority
import com.example.test0512.mobile.model.TaskSource
import com.example.test0512.mobile.ui.MobileTaskListScreen
import com.example.test0512.mobile.network.SyncProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: MobileTaskViewModel by viewModels {
        MobileTaskViewModel.Factory(
            TaskRepository(AppDatabase.getDatabase(this.applicationContext).taskDao())
        )
    }
    
    private lateinit var syncManager: MobileTaskSyncManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.READ_CALENDAR] == true) {
                importFromCalendar()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val taskDao = AppDatabase.getDatabase(this.applicationContext).taskDao()
        SyncProvider.init(this.applicationContext, taskDao)
        syncManager = SyncProvider.syncManager
        
        // Start Foreground Service
        com.example.test0512.mobile.network.LocalSyncForegroundService.start(this)

        val neededPermissions = mutableListOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missingPermissions = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            importFromCalendar()
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MobileTaskListScreen(
                    viewModel = viewModel,
                    syncManager = syncManager
                )
            }
        }
    }

    private fun importFromCalendar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val now = Calendar.getInstance()
            val beginTime = now.timeInMillis
            now.add(Calendar.DAY_OF_YEAR, 7)
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
            
            val taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
            val existingTasks = taskDao.getAllTasksSync()
            val existingExternalIds = existingTasks.mapNotNull { it.externalId }.toSet()
            val timeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            val newTasks = mutableListOf<RadarTask>()

            cursor?.use {
                val idIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = it.getColumnIndex(CalendarContract.Instances.END)
                val descIndex = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)

                while (it.moveToNext()) {
                    val eventId = it.getString(idIndex)
                    if (!existingExternalIds.contains(eventId)) {
                        var title = it.getString(titleIndex) ?: "无标题日程"
                        val begin = it.getLong(beginIndex)
                        val end = it.getLong(endIndex)
                        val desc = if (descIndex != -1) it.getString(descIndex) ?: "" else ""

                        var parsedPriority = TaskPriority.REGULAR
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
            }
            if (newTasks.isNotEmpty()) {
                taskDao.insertTasks(newTasks)
                syncManager.syncAllTasksToWatch()
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "已从日历导入 ${newTasks.size} 条任务", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
