package com.example.test0512.mobile.data

import androidx.room.TypeConverter
import com.example.test0512.mobile.model.TaskPriority
import com.example.test0512.mobile.model.TaskSource

class Converters {
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): Int {
        return when (priority) {
            TaskPriority.EMERGENCY -> 3
            TaskPriority.IMPORTANT -> 2
            TaskPriority.REGULAR -> 1
            TaskPriority.LONG_TERM -> 0
        }
    }

    @TypeConverter
    fun toTaskPriority(weight: Int): TaskPriority {
        return when (weight) {
            3 -> TaskPriority.EMERGENCY
            2 -> TaskPriority.IMPORTANT
            1 -> TaskPriority.REGULAR
            0 -> TaskPriority.LONG_TERM
            else -> TaskPriority.REGULAR
        }
    }

    @TypeConverter
    fun fromTaskSource(source: TaskSource): String {
        return source.name
    }

    @TypeConverter
    fun toTaskSource(source: String): TaskSource {
        return try {
            TaskSource.valueOf(source)
        } catch (e: Exception) {
            TaskSource.MANUAL
        }
    }
}
