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
    
    var isListViewEnabled by mutableStateOf(false)
        private set

    var isShowSpiralLines by mutableStateOf(true)

    fun toggleViewMode(isList: Boolean) {
        isListViewEnabled = isList
    }

    fun toggleSpiralLines(isShow: Boolean) {
        isShowSpiralLines = isShow
    }

    // 严格还原初始的 8 条数据，确保螺旋线视觉饱满
    val tasks = mutableStateListOf(
        RadarTask(1, "搞定毕业论文答辩", "今天 14:00", "准备好 PPT，重点陈述 PG-MoE 架构的创新点。", TaskPriority.EMERGENCY, TaskSource.MANUAL, 10),
        RadarTask(2, "组会汇报准备", "今天 16:30", "整理本周实验数据，生成 loss 曲线图表。", TaskPriority.IMPORTANT, TaskSource.CALENDAR, 20),
        RadarTask(3, "回复导师邮件", "晚上 19:00", "关于下周开题报告的修改意见确认。", TaskPriority.IMPORTANT, TaskSource.WECHAT, 30),
        RadarTask(4, "操场跑步 5km", "晚上 20:00", "保持配速，戴上手表记录心率。", TaskPriority.REGULAR, TaskSource.CALENDAR, 40),
        RadarTask(5, "服务器续费", "今晚 24:00", "VPS 马上到期了，赶紧去后台续费防失联！", TaskPriority.EMERGENCY, TaskSource.MANUAL, 50),
        RadarTask(6, "买咖啡", "随时", "冰美式，少冰。", TaskPriority.REGULAR, TaskSource.MANUAL, 60),
        RadarTask(7, "预定明天机票", "明天上午", "查看各大航司折扣，尽早锁定舱位。", TaskPriority.IMPORTANT, TaskSource.MANUAL, 70),
        RadarTask(8, "收取快递", "下班后", "丰巢柜取件码：8848。", TaskPriority.REGULAR, TaskSource.WECHAT, 80),
        RadarTask(9, "阅读 Compose 源码", "明天下午", "深入理解 Recomposition 的底层机制。", TaskPriority.LONG_TERM, TaskSource.MANUAL, 90),
        RadarTask(10, "英语口语练习", "每天 21:00", "在 App 上完成 30 分钟跟读打卡。", TaskPriority.REGULAR, TaskSource.CALENDAR, 100),
        RadarTask(11, "家庭聚餐买菜", "本周末", "记得买新鲜的鲈鱼和排骨。", TaskPriority.LONG_TERM, TaskSource.WECHAT, 110),
        RadarTask(12, "整理桌面", "有空时", "清理旧文件，擦拭显示器。", TaskPriority.LONG_TERM, TaskSource.MANUAL, 120)
    )

    val isSystemLocked: Boolean
        get() = tasks.isNotEmpty() && tasks.first().priority == TaskPriority.EMERGENCY

    fun addTask(title: String, description: String, time: String, priority: TaskPriority, source: TaskSource = TaskSource.MANUAL) {
        val newId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        val minSortOrder = tasks.minOfOrNull { it.sortOrder } ?: 0
        val newTask = RadarTask(newId, title, time, description, priority, source, minSortOrder - 10)
        tasks.add(0, newTask)
    }

    fun importFromWechat() {
        addTask(
            title = "【老板】修改周报",
            description = "把第三段的数据再核对一下，尽快发我！",
            time = "刚刚",
            priority = TaskPriority.EMERGENCY,
            source = TaskSource.WECHAT
        )
    }

    fun syncFromCalendar() {
        addTask(
            title = "部门周会",
            description = "参与季度目标对齐，地点：会议室 3A。",
            time = "明天 10:00",
            priority = TaskPriority.IMPORTANT,
            source = TaskSource.CALENDAR
        )
    }

    fun completeTask(task: RadarTask) {
        tasks.remove(task)
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
