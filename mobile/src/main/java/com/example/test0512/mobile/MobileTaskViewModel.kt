package com.example.test0512.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.test0512.mobile.data.TaskRepository
import com.example.test0512.mobile.model.RadarTask
import com.example.test0512.mobile.model.TaskPriority
import com.example.test0512.mobile.model.TaskSource
import com.example.test0512.mobile.model.TaskSorter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MobileTaskViewModel(private val repository: TaskRepository) : ViewModel() {

    var onDatabaseChanged: (() -> Unit)? = null

    val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L) // Update every minute
        }
    }

    val tasks: StateFlow<List<RadarTask>> = combine(repository.allTasks, tickerFlow) { list, now ->
        list.sortedWith(TaskSorter.getComparator(now))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTask(title: String, description: String, time: String, dueDate: Long?, priority: TaskPriority, externalId: String? = null) {
        viewModelScope.launch {
            val newTask = RadarTask(
                id = UUID.randomUUID().toString(),
                title = title,
                time = time,
                dueDate = dueDate,
                description = description,
                priority = priority,
                source = if (externalId != null) TaskSource.CALENDAR else TaskSource.MANUAL,
                externalId = externalId,
                sortOrder = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertTask(newTask)
            withContext(Dispatchers.Main) { onDatabaseChanged?.invoke() }
        }
    }

    fun updateTask(task: RadarTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(updatedAt = maxOf(System.currentTimeMillis(), task.updatedAt + 1)))
            withContext(Dispatchers.Main) { onDatabaseChanged?.invoke() }
        }
    }

    fun pinTask(taskId: String) {
        viewModelScope.launch {
            repository.pinTask(taskId)
            withContext(Dispatchers.Main) { onDatabaseChanged?.invoke() }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.markTaskAsCompleted(taskId)
            withContext(Dispatchers.Main) { onDatabaseChanged?.invoke() }
        }
    }

    fun deleteTask(task: RadarTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            withContext(Dispatchers.Main) { onDatabaseChanged?.invoke() }
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllTasks()
            withContext(Dispatchers.Main) { onDatabaseChanged?.invoke() }
        }
    }

    fun importDummyTasks(count: Int) {
        viewModelScope.launch {
            repository.clearAllTasks()
            val now = System.currentTimeMillis()
            
            val titles = listOf(
                "归还图书馆借书", "毕业论文终期答辩", "宿舍散伙饭", "打包行李寄顺丰",
                "领取毕业证和学位证", "办理党团组织关系转接", "结清校园网余额",
                "退还宿舍钥匙", "找辅导员签离校循环单", "去天安门看升旗", "参加学院毕业典礼"
            )
            val descriptions = listOf(
                "西区图书馆一楼，逾期扣费", "九教204，带五份打印版", "南门外串串香", "被子和冬装",
                "学院教务科", "学生活动中心", "网信办办理",
                "宿管阿姨处", "思源楼", "最后一次集体活动", "天佑会堂拨穗"
            )
            val priorities = TaskPriority.entries.toTypedArray()
            
            // Set base date to June 24, 2026
            val calendar = java.util.Calendar.getInstance()
            calendar.set(2026, java.util.Calendar.JUNE, 24, 9, 0, 0)
            val baseTime = calendar.timeInMillis

            val limit = minOf(count, titles.size)
            for (i in 0 until limit) {
                val dueDate = baseTime + (i * 86400_000L) + (Math.random() * 3600_000L).toLong()
                
                val newTask = RadarTask(
                    id = UUID.randomUUID().toString(),
                    title = titles[i],
                    time = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(dueDate)),
                    dueDate = dueDate,
                    description = descriptions[i],
                    priority = priorities.random(),
                    source = TaskSource.MANUAL,
                    sortOrder = 0,
                    createdAt = now + i,
                    updatedAt = now + i
                )
                repository.insertTask(newTask)
            }
            withContext(Dispatchers.Main) { onDatabaseChanged?.invoke() }
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MobileTaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MobileTaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
