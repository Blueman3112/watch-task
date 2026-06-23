package com.example.test0512.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test0512.presentation.components.AddTaskScreen
import com.example.test0512.presentation.components.RadarSpiralScreen
import com.example.test0512.presentation.components.TaskDetailScreen
import com.example.test0512.presentation.components.TaskListScreen
import com.example.test0512.presentation.components.SettingsScreen
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.test0512.data.AppDatabase
import com.example.test0512.data.TaskRepository
import kotlinx.coroutines.delay
import com.example.test0512.data.WearTaskSyncManager

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(
            TaskRepository(AppDatabase.getDatabase(this.applicationContext).taskDao())
        )
    }

    private lateinit var syncManager: WearTaskSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        syncManager = WearTaskSyncManager(this, AppDatabase.getDatabase(this.applicationContext).taskDao())
        syncManager.startListening()

        viewModel.onDatabaseChanged = {
            syncManager.syncAllTasksToPhone()
        }

        setContent {
            val tasks by viewModel.tasks.collectAsState()
            val isSystemLocked by viewModel.isSystemLocked.collectAsState()
            val pinnedNotification = viewModel.pinnedNotification
            val navController = rememberSwipeDismissableNavController()

            LaunchedEffect(pinnedNotification) {
                if (pinnedNotification != null) {
                    delay(2000)
                    viewModel.clearNotification()
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        if (viewModel.isListViewEnabled) {
                            TaskListScreen(
                                tasks = tasks,
                                onTaskClick = { clickedTask ->
                                    navController.navigate("task_detail/${clickedTask.id}")
                                },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        } else {
                            RadarSpiralScreen(
                                rawTasks = tasks,
                                optimisticPinnedId = viewModel.optimisticPinnedId,
                                isSystemLocked = isSystemLocked,
                                onTaskClick = { clickedTask ->
                                    navController.navigate("task_detail/${clickedTask.id}")
                                },
                                onTopConfirm = { taskId ->
                                    viewModel.pinTaskToTop(taskId)
                                },
                                onAddTaskClick = { navController.navigate("add_task") },
                                onSettingsClick = { navController.navigate("settings") },
                                isShowSpiralLines = viewModel.isShowSpiralLines
                            )
                        }
                    }
                    composable("settings") {
                        SettingsScreen(
                            isListViewEnabled = viewModel.isListViewEnabled,
                            isShowSpiralLines = viewModel.isShowSpiralLines,
                            onViewModeToggle = { isList -> viewModel.toggleViewMode(isList) },
                            onToggleSpiralLines = { isShow -> viewModel.toggleSpiralLines(isShow) },
                            onUncompleteAll = { 
                                viewModel.uncompleteAllTasks()
                                navController.popBackStack()
                            },
                            onRestoreInitialData = {
                                viewModel.restoreInitialData()
                                navController.popBackStack()
                            },
                            onClearAllTasks = {
                                viewModel.clearAllTasks()
                                navController.popBackStack()
                            },
                            onGenerateSequenceData = {
                                viewModel.generateSequenceTasks()
                                navController.popBackStack()
                            },
                            onPerformTwoWaySync = {
                                syncManager.performTwoWaySync()
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("add_task") {
                        AddTaskScreen(
                            onDismiss = { navController.popBackStack() },
                            onSave = { title, desc, timeStr, dueDate, priority ->
                                val finalTimeStr = if (timeStr.contains("自动")) {
                                    if (dueDate != null) {
                                        val offset = if (priority == com.example.test0512.model.TaskPriority.EMERGENCY) 5L * 3600_000L else 1L * 3600_000L
                                        val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(dueDate - offset))
                                    } else {
                                        "无提醒"
                                    }
                                } else {
                                    timeStr
                                }
                                viewModel.addTask(title, desc, finalTimeStr, dueDate, priority)
                                navController.popBackStack()
                            },
                            onWechatImport = {
                                viewModel.importFromWechat()
                                navController.popBackStack()
                            },
                            onCalendarSync = {
                                syncManager.performTwoWaySync()
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("task_detail/{taskId}") { backStackEntry ->
                        val taskIdStr = backStackEntry.arguments?.getString("taskId")
                        val selectedTask = tasks.find { it.id == taskIdStr }

                        if (selectedTask != null) {
                            TaskDetailScreen(
                                task = selectedTask,
                                onClose = { navController.popBackStack() },
                                onComplete = {
                                    viewModel.completeTask(selectedTask)
                                    navController.popBackStack()
                                },
                                onPin = {
                                    viewModel.pinTaskToTop(selectedTask.id)
                                    navController.popBackStack()
                                },
                                onUpdatePriority = { newPriority ->
                                    viewModel.updateTaskPriority(selectedTask, newPriority)
                                },
                                onUpdateTime = { newTime ->
                                    viewModel.updateTaskTime(selectedTask, newTime)
                                }
                            )
                        }
                    }
                }

                if (pinnedNotification != null) {
                    PinnedNotificationView(pinnedNotification)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        syncManager.stopListening()
    }
}

@Composable
fun PinnedNotificationView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        BasicText(
            text = message,
            style = TextStyle(
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .background(Color(0xFF4DB6AC), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
