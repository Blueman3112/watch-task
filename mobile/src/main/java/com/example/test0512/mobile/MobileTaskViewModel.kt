package com.example.test0512.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.test0512.mobile.data.TaskRepository
import com.example.test0512.mobile.model.RadarTask
import com.example.test0512.mobile.model.TaskPriority
import com.example.test0512.mobile.model.TaskSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MobileTaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<RadarTask>> = repository.allTasks.stateIn(
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
        }
    }

    fun updateTask(task: RadarTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun pinTask(taskId: String) {
        viewModelScope.launch {
            repository.pinTask(taskId)
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.markTaskAsCompleted(taskId)
        }
    }

    fun deleteTask(task: RadarTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
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
