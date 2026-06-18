package com.example.test0512.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import com.example.test0512.model.TaskSource

class TaskViewModel : ViewModel() {
    var pinnedNotification by mutableStateOf<String?>(null)

    // 预设 12 条任务，覆盖三种来源（手动/日历/微信），确保 Demo 演示系统性
    val tasks = mutableStateListOf(
        // ── 手动输入任务（5条）──
        RadarTask(1, "搞定毕业论文答辩", "今天 14:00",
            "准备好 PPT，重点陈述 PG-MoE 架构的创新点。记得带上打印好的论文原稿给评委老师。",
            TaskPriority.EMERGENCY, TaskSource.MANUAL, 10),
        RadarTask(2, "跑通模型训练", "今天 16:30",
            "检查服务器 GPU 占用，重新调整 batch size。",
            TaskPriority.IMPORTANT, TaskSource.MANUAL, 20),
        RadarTask(8, "预定明天机票", "明天",
            "查看各大航司折扣，尽早锁定舱位。",
            TaskPriority.IMPORTANT, TaskSource.MANUAL, 30),
        RadarTask(11, "购买实验耗材", "下午",
            "采购电极片和导电胶，实验室库存不够了。",
            TaskPriority.REGULAR, TaskSource.MANUAL, 40),
        RadarTask(12, "阅读 Compose 源码", "明天上午",
            "深入理解 Recomposition 的底层触发机制。",
            TaskPriority.LONG_TERM, TaskSource.MANUAL, 50),

        // ── 日历自动生成任务（4条）──
        RadarTask(3, "操场跑步 5km", "晚上 20:00",
            "保持配速，戴上手表记录心率。每日打卡第 47 天。",
            TaskPriority.REGULAR, TaskSource.CALENDAR, 60),
        RadarTask(5, "早起打卡", "每天 07:00",
            "坚持早起，保持作息规律。连续打卡中。",
            TaskPriority.REGULAR, TaskSource.CALENDAR, 70),
        RadarTask(6, "英语单词背诵", "每天 08:30",
            "今日任务：50 个考研核心词汇。",
            TaskPriority.REGULAR, TaskSource.CALENDAR, 80),
        RadarTask(7, "吃药提醒", "每天 21:00",
            "饭后服用维生素 D，不要忘记。",
            TaskPriority.IMPORTANT, TaskSource.CALENDAR, 90),

        // ── 微信导入任务（3条）──
        RadarTask(4, "服务器续费", "今晚 24:00",
            "RackNerd VPS 马上到期了，赶紧去后台续费防失联！",
            TaskPriority.EMERGENCY, TaskSource.WECHAT_IMPORT, 100),
        RadarTask(9, "组会准备材料", "周三 10:00",
            "导师在群里通知：本周组会每人汇报进展，准备 PPT。",
            TaskPriority.IMPORTANT, TaskSource.WECHAT_IMPORT, 110),
        RadarTask(10, "项目进度汇报", "明天 15:00",
            "项目组群通知：提交本周工作进展报告，抄送负责人。",
            TaskPriority.EMERGENCY, TaskSource.WECHAT_IMPORT, 120)
    )

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

