package com.example.test0512.mobile.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TaskPriority(val label: String) {
    EMERGENCY("紧急"),
    IMPORTANT("重要"),
    REGULAR("待办"),
    LONG_TERM("长远")
}

@Serializable
enum class TaskSource(val label: String) {
    MANUAL("手动输入"),
    CALENDAR("日历同步"),
    WECHAT("微信导入")
}

@Entity(tableName = "tasks")
@Serializable
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
    val isDeleted: Boolean = false,
    val externalId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
