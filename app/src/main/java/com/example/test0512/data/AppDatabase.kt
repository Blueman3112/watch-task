package com.example.test0512.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import com.example.test0512.model.TaskSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [RadarTask::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao



    private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
                var database = INSTANCE
                while (database == null) {
                    kotlinx.coroutines.delay(100)
                    database = INSTANCE
                }
                populateDatabase(database.taskDao())
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            scope.launch {
                var database = INSTANCE
                while (database == null) {
                    kotlinx.coroutines.delay(100)
                    database = INSTANCE
                }
                populateDatabase(database.taskDao())
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    .addCallback(AppDatabaseCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun populateDatabase(taskDao: TaskDao) {
            val baseCalendar = java.util.Calendar.getInstance().apply { 
                set(2026, 5, 23, 8, 0, 0) // 2026-06-23 08:00:00 (Month is 0-indexed)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val baseTime = baseCalendar.timeInMillis
            val hour = 3600_000L
            val day = 86400_000L

            val initialTasks = listOf(
                RadarTask(id = UUID.randomUUID().toString(), title = "提交《史纲》结课论文", time = "06/23 08:00", dueDate = baseTime, description = "字数还差 500，马上补完交到公教信箱里。", priority = TaskPriority.EMERGENCY, source = TaskSource.MANUAL, sortOrder = 10),
                RadarTask(id = UUID.randomUUID().toString(), title = "抢世纪馆羽毛球场", time = "06/23 10:00", dueDate = baseTime + 2 * hour, description = "带上校园卡，帮室友一起抢场地。", priority = TaskPriority.IMPORTANT, source = TaskSource.CALENDAR, sortOrder = 20),
                RadarTask(id = UUID.randomUUID().toString(), title = "回复导师微信", time = "06/23 12:00", dueDate = baseTime + 4 * hour, description = "导师问了下周开题报告的进度，需要马上汇报。", priority = TaskPriority.IMPORTANT, source = TaskSource.WECHAT, sortOrder = 30),
                RadarTask(id = UUID.randomUUID().toString(), title = "去品园取快递", time = "06/23 15:00", dueDate = baseTime + 7 * hour, description = "买的教材到了，赶在菜鸟驿站关门前去取。", priority = TaskPriority.REGULAR, source = TaskSource.CALENDAR, sortOrder = 40),
                RadarTask(id = UUID.randomUUID().toString(), title = "青年大学习打卡", time = "06/23 18:00", dueDate = baseTime + 10 * hour, description = "团支书催了三次了，截图发群里。", priority = TaskPriority.EMERGENCY, source = TaskSource.MANUAL, sortOrder = 50),
                RadarTask(id = UUID.randomUUID().toString(), title = "买瑞幸生椰拿铁", time = "06/23 20:00", dueDate = baseTime + 12 * hour, description = "去明德楼下面那家店，加冰不加糖。", priority = TaskPriority.REGULAR, source = TaskSource.MANUAL, sortOrder = 60),
                RadarTask(id = UUID.randomUUID().toString(), title = "中区食堂买烤鸭", time = "06/24 11:30", dueDate = baseTime + 1 * day + 3 * hour + 1800_000L, description = "记得早点去排队，晚了就卖光了。", priority = TaskPriority.REGULAR, source = TaskSource.WECHAT, sortOrder = 80),
                RadarTask(id = UUID.randomUUID().toString(), title = "准备“挑战杯”答辩", time = "06/24 15:00", dueDate = baseTime + 1 * day + 7 * hour, description = "完善路演 PPT，去求是楼空教室排练。", priority = TaskPriority.LONG_TERM, source = TaskSource.MANUAL, sortOrder = 90)
            )
            taskDao.insertTasks(initialTasks)
        }
    }
}
