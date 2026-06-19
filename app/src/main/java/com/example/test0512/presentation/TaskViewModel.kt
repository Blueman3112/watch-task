package com.example.test0512.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import com.example.test0512.model.TaskSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskViewModel : ViewModel() {
    var pinnedNotification by mutableStateOf<String?>(null)

    // 启动时为空，Demo 序列会逐个添加任务
    val tasks = mutableStateListOf<RadarTask>()

    // Demo 演示序列：不同来源 → 不同节奏入场
    private data class DemoEntry(val task: RadarTask, val delayBeforeMs: Long)

    private val demoSequence = listOf(
        // ── 第一个任务：中心核心（紧急，1s后出现）──
        DemoEntry(RadarTask(1, "搞定毕业论文答辩", "今天 14:00",
            "准备好 PPT，重点陈述 PG-MoE 架构的创新点。记得带上打印好的论文原稿给评委老师。",
            TaskPriority.EMERGENCY, TaskSource.MANUAL, 10), 1000L),

        // ── 日历任务：平稳节奏（每1秒一个，均匀入场）──
        DemoEntry(RadarTask(3, "操场跑步 5km", "晚上 20:00",
            "保持配速，戴上手表记录心率。每日打卡第 47 天。",
            TaskPriority.REGULAR, TaskSource.CALENDAR, 20), 1000L),
        DemoEntry(RadarTask(5, "早起打卡", "每天 07:00",
            "坚持早起，保持作息规律。连续打卡中。",
            TaskPriority.REGULAR, TaskSource.CALENDAR, 30), 1000L),
        DemoEntry(RadarTask(6, "英语单词背诵", "每天 08:30",
            "今日任务：50 个考研核心词汇。",
            TaskPriority.REGULAR, TaskSource.CALENDAR, 40), 1000L),

        // ── 手动任务：中等节奏（每1.2秒一个）──
        DemoEntry(RadarTask(2, "跑通模型训练", "今天 16:30",
            "检查服务器 GPU 占用，重新调整 batch size。",
            TaskPriority.IMPORTANT, TaskSource.MANUAL, 50), 1200L),
        DemoEntry(RadarTask(8, "预定明天机票", "明天",
            "查看各大航司折扣，尽早锁定舱位。",
            TaskPriority.IMPORTANT, TaskSource.MANUAL, 60), 1200L),

        // ── 微信任务：突发快节奏（0.4秒，模拟突发消息）──
        DemoEntry(RadarTask(4, "服务器续费", "今晚 24:00",
            "RackNerd VPS 马上到期了，赶紧去后台续费防失联！",
            TaskPriority.EMERGENCY, TaskSource.WECHAT_IMPORT, 70), 400L),

        // ── 最后一个：从容的长远任务 ──
        DemoEntry(RadarTask(12, "阅读 Compose 源码", "明天上午",
            "深入理解 Recomposition 的底层触发机制。",
            TaskPriority.LONG_TERM, TaskSource.MANUAL, 80), 1500L)
    )

    init {
        startDemoSequence()
    }

    private fun startDemoSequence() {
        viewModelScope.launch {
            for (entry in demoSequence) {
                delay(entry.delayBeforeMs)
                tasks.add(entry.task)
            }
        }
    }

    fun addTask(title: String, description: String, time: String, priority: TaskPriority, source: TaskSource = TaskSource.MANUAL) {
        val newId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        val minSortOrder = tasks.minOfOrNull { it.sortOrder } ?: 0
        val newTask = RadarTask(newId, title, time, description, priority, source, minSortOrder - 10)
        tasks.add(0, newTask)
    }

    fun completeTask(task: RadarTask) {
        tasks.remove(task)
        val maxSortOrder = tasks.maxOfOrNull { it.sortOrder } ?: 0
        tasks.add(task.copy(sortOrder = maxSortOrder + 10))
    }

    fun pinToTop(task: RadarTask) {
        tasks.remove(task)
        val minSortOrder = tasks.minOfOrNull { it.sortOrder } ?: 0
        tasks.add(0, task.copy(sortOrder = minSortOrder - 10))
        pinnedNotification = "已置顶：${task.title}"
    }

    fun pinToTopByIndex(index: Int) {
        if (index in tasks.indices) {
            val task = tasks.removeAt(index)
            val minSortOrder = tasks.minOfOrNull { it.sortOrder } ?: 0
            tasks.add(0, task.copy(sortOrder = minSortOrder - 10))
            pinnedNotification = "已置顶：${task.title}"
        }
    }

    fun clearNotification() {
        pinnedNotification = null
    }
}
