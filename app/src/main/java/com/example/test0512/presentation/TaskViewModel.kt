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
        list.sortedWith(TaskSorter.getComparator(now))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isSystemLocked: StateFlow<Boolean> = tasks
        .map { list -> list.isNotEmpty() && TaskSorter.calculateUrgencyScore(list.first(), System.currentTimeMillis()) >= 500 }
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

    fun clearAllTasks() {
        optimisticPinnedId = null
        viewModelScope.launch {
            repository.deleteAllTasks()
        }
    }

    fun pinTaskToTop(taskId: String) {
        optimisticPinnedId = taskId
        viewModelScope.launch {
            repository.pinTask(taskId)
        }
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

    fun generateSequenceTasks() {
        viewModelScope.launch {
            repository.deleteAllTasks()
            val now = System.currentTimeMillis()
            val priorities = listOf(
                TaskPriority.EMERGENCY,
                TaskPriority.IMPORTANT,
                TaskPriority.REGULAR,
                TaskPriority.LONG_TERM
            )
            
            val random = java.util.Random()
            val generatedTasks = mutableListOf<RadarTask>()
            for (i in 0..7) {
                val priority = priorities[i % priorities.size]
                val offsetMs = random.nextInt(86400000).toLong() // Within 24h
                val dueDateMs = now + offsetMs
                
                val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                val timeStr = sdf.format(java.util.Date(dueDateMs))
                
                val newTask = RadarTask(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "", // Temporary
                    description = "",
                    time = timeStr,
                    dueDate = dueDateMs,
                    priority = priority,
                    source = com.example.test0512.model.TaskSource.MANUAL,
                    isPinned = false,
                    isCompleted = false,
                    createdAt = now + i,
                    updatedAt = now + i,
                    sortOrder = 0
                )
                generatedTasks.add(newTask)
            }

            // Sort them using the same logic as rendering
            val sortedTasks = generatedTasks.sortedWith(TaskSorter.getComparator(now))

            // Update title to be the direct sequence number and insert into DB
            sortedTasks.forEachIndexed { index, task ->
                repository.insertTask(task.copy(title = index.toString()))
            }
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
