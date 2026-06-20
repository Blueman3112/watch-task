package com.example.test0512.model

/**
 * 任务优先级枚举类
 * @property label 优先级的显示标签
 */
enum class TaskPriority(val label: String) {
    /** 紧急任务 */
    EMERGENCY("紧急"),
    /** 重要任务 */
    IMPORTANT("重要"),
    /** 普通待办任务 */
    REGULAR("待办"),
    /** 长远任务 */
    LONG_TERM("长远")
}

/**
 * 任务来源枚举类
 */
enum class TaskSource(val label: String) {
    MANUAL("手动输入"),
    CALENDAR("日历同步"),
    WECHAT("微信导入")
}

/**
 * 雷达图任务数据模型
 * @property id 任务唯一标识符
 * @property title 任务标题
 * @property time 任务提醒时间 (Reminder Time)
 * @property description 任务详细描述
 * @property priority 任务优先级，默认为 REGULAR (待处理)
 * @property source 任务来源，默认为 MANUAL
 */
data class RadarTask(
    val id: Int,
    val title: String,
    val time: String,
    val description: String,
    val priority: TaskPriority = TaskPriority.REGULAR,
    val source: TaskSource = TaskSource.MANUAL,
    val sortOrder: Int = 0
)
