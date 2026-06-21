package com.example.test0512.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

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
 * @property id 任务的分布式唯一标识符 (UUID)
 * @property title 任务标题
 * @property time 任务提醒时间 (旧版展示文本)
 * @property dueDate 任务真正的截止时间戳
 * @property description 任务详细描述
 * @property priority 任务优先级，默认为 REGULAR (待处理)
 * @property source 任务来源，默认为 MANUAL
 * @property sortOrder 用户手动置顶的权重排序
 * @property isCompleted 是否已完成（软删除标识）
 * @property createdAt 创建时间戳
 * @property updatedAt 最后修改时间戳
 */
@Entity(tableName = "tasks")
data class RadarTask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val time: String,
    val dueDate: Long? = null,
    val description: String,
    val priority: TaskPriority = TaskPriority.REGULAR,
    val source: TaskSource = TaskSource.MANUAL,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false,
    val externalId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
