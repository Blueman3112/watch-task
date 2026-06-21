package com.example.test0512.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.test0512.data.TaskRepository
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import com.example.test0512.model.TaskSource
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
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

    val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L) // Update every minute
        }
    }

    var optimisticPinnedId by mutableStateOf<String?>(null)
        private set

    val tasks: StateFlow<List<RadarTask>> = combine(repository.allTasks, tickerFlow) { list, now ->
        list.sortedWith(
            compareByDescending<RadarTask> { it.isPinned }
                .thenByDescending { calculateUrgencyScore(it, now) }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isSystemLocked: StateFlow<Boolean> = tasks
        .map { list -> list.isNotEmpty() && calculateUrgencyScore(list.first(), System.currentTimeMillis()) >= 500 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun uncompleteAllTasks() {
        optimisticPinnedId = null
        viewModelScope.launch {
            repository.uncompleteAllTasks()
        }
    }

    fun restoreInitialData() {
        optimisticPinnedId = null
        viewModelScope.launch {
            repository.restoreInitialData()
        }
    }

    fun pinTaskToTop(taskId: String) {
        optimisticPinnedId = taskId
        viewModelScope.launch {
            repository.pinTask(taskId)
        }
    }

    private fun calculateUrgencyScore(task: RadarTask, now: Long): Int {
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

    fun addTask(title: String, description: String, time: String, dueDate: Long?, priority: TaskPriority, source: TaskSource = TaskSource.MANUAL) {
        viewModelScope.launch {
            val newTask = RadarTask(
                id = UUID.randomUUID().toString(),
                title = title,
                time = time,
                dueDate = dueDate,
                description = description,
                priority = priority,
                source = source,
                sortOrder = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertTask(newTask)
        }
    }

    fun importFromWechat() {
        addTask(
            title = "【老板】修改周报",
            description = "把第三段的数据再核对一下，尽快发我！",
            time = "刚刚",
            dueDate = System.currentTimeMillis() + 3600_000L,
            priority = TaskPriority.EMERGENCY,
            source = TaskSource.WECHAT
        )
    }

    fun syncFromCalendar() {
        addTask(
            title = "部门周会",
            description = "参与季度目标对齐，地点：会议室 3A。",
            time = "明天 10:00",
            dueDate = System.currentTimeMillis() + 86400_000L,
            priority = TaskPriority.IMPORTANT,
            source = TaskSource.CALENDAR
        )
    }

    fun updateTaskPriority(task: RadarTask, newPriority: TaskPriority) {
        viewModelScope.launch {
            repository.updateTask(task.copy(priority = newPriority, updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateTaskTime(task: RadarTask, newTime: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(time = newTime, updatedAt = System.currentTimeMillis()))
        }
    }

    fun completeTask(task: RadarTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = true, updatedAt = System.currentTimeMillis()))
        }
    }

    fun clearNotification() {
        pinnedNotification = null
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
