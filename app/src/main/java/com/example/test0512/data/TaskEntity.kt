package com.example.test0512.data

// 暂时清空以解决库冲突导致的编译错误
/*
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: String,
    val description: String,
    val priority: String, // Store enum as string
    val sortOrder: Int // To maintain the order on the spiral
)

fun TaskEntity.toDomain(): RadarTask {
    return RadarTask(
        id = id,
        title = title,
        time = time,
        description = description,
        priority = try { TaskPriority.valueOf(priority) } catch (e: Exception) { TaskPriority.REGULAR },
        sortOrder = sortOrder
    )
}

fun RadarTask.toEntity(sortOrder: Int = this.sortOrder): TaskEntity {
    return TaskEntity(
        id = if (id <= 0) 0 else id, // 0 for auto-generate
        title = title,
        time = time,
        description = description,
        priority = priority.name,
        sortOrder = sortOrder
    )
}
*/
