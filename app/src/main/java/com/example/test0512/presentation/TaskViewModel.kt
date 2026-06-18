package com.example.test0512.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority

class TaskViewModel : ViewModel() {
    var pinnedNotification by mutableStateOf<String?>(null)

    // 严格还原初始的 8 条数据，确保螺旋线视觉饱满
    val tasks = mutableStateListOf(
        RadarTask(1, "搞定毕业论文答辩", "今天 14:00", "准备好 PPT，重点陈述 PG-MoE 架构的创新点。记得带上打印好的论文原稿给评委老师。", TaskPriority.EMERGENCY, 10),
        RadarTask(2, "跑通模型训练", "今天 16:30", "检查服务器 GPU 占用，重新调整 batch size。", TaskPriority.IMPORTANT, 20),
        RadarTask(3, "操场跑步 5km", "晚上 20:00", "保持配速，戴上手表记录心率。", TaskPriority.REGULAR, 30),
        RadarTask(4, "阅读 Compose 源码", "明天上午", "深入理解 Recomposition 的底层触发机制。", TaskPriority.LONG_TERM, 40),
        RadarTask(5, "服务器续费", "今晚 24:00", "RackNerd VPS 马上到期了，赶紧去后台续费防失联！", TaskPriority.EMERGENCY, 50),
        RadarTask(6, "买咖啡", "随时", "冰美式，少冰。", TaskPriority.REGULAR, 60),
        RadarTask(7, "预定明天机票", "明天", "查看各大航司折扣，尽早锁定舱位。", TaskPriority.IMPORTANT, 70),
        RadarTask(8, "收取快递", "下班后", "丰巢柜取件码：8848。", TaskPriority.REGULAR, 80)
    )

    fun addTask(title: String, description: String, time: String, priority: TaskPriority) {
        val newId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        val minSortOrder = tasks.minOfOrNull { it.sortOrder } ?: 0
        val newTask = RadarTask(newId, title, time, description, priority, minSortOrder - 10)
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
