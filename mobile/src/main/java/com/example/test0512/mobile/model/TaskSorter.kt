package com.example.test0512.mobile.model

object TaskSorter {
    fun calculateUrgencyScore(task: RadarTask, now: Long): Int {
        val baseScore = when (task.priority) {
            TaskPriority.EMERGENCY -> 300
            TaskPriority.IMPORTANT -> 200
            TaskPriority.REGULAR -> 100
            TaskPriority.LONG_TERM -> 0
        }
        
        val timeFactor = if (task.dueDate != null) {
            val timeLeftHours = (task.dueDate - now) / 3600_000.0
            if (timeLeftHours < 0) {
                500 + (-timeLeftHours * 10).toInt()
            } else {
                if (timeLeftHours > 72) {
                    0
                } else {
                    ((72 - timeLeftHours) * (400.0 / 72.0)).toInt()
                }
            }
        } else {
            0
        }
        
        return baseScore + timeFactor
    }

    fun getComparator(now: Long): Comparator<RadarTask> {
        return compareByDescending<RadarTask> { it.isPinned }
            .thenByDescending { calculateUrgencyScore(it, now) }
            .thenByDescending { it.createdAt }
    }
}
