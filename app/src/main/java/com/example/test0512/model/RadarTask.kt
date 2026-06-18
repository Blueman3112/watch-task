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
    /** 普通待处理任务 */
    REGULAR("待处理"),
    /** 长远任务 */
    LONG_TERM("长远")
}

/**
 * 任务来源分类枚举
 * @property label 来源的显示标签
 * @property icon 来源的图标字符（用于螺旋节点上的微标记）
 */
enum class TaskSource(val label: String, val icon: String) {
    /** 用户手动输入的任务 */
    MANUAL("手动", "✎"),
    /** 从外部日历自动同步的周期性任务 */
    CALENDAR("日历", "📅"),
    /** 从微信一键粘贴导入的任务（高级功能） */
    WECHAT_IMPORT("微信", "💬")
}

/**
 * 雷达图任务数据模型
 * @property id 任务唯一标识符
 * @property title 任务标题
 * @property time 任务时间描述
 * @property description 任务详细描述
 * @property priority 任务优先级，默认为 REGULAR (待处理)
 * @property source 任务来源分类，默认为 MANUAL (手动输入)
 * @property sortOrder 排序权重
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

