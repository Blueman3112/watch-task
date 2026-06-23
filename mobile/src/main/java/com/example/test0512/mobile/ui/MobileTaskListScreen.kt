package com.example.test0512.mobile.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test0512.mobile.MobileTaskViewModel
import com.example.test0512.mobile.model.RadarTask
import com.example.test0512.mobile.model.TaskPriority
import com.example.test0512.mobile.data.MobileTaskSyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.google.android.gms.wearable.Wearable
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileTaskListScreen(
    viewModel: MobileTaskViewModel,
    syncManager: MobileTaskSyncManager
) {
    val tasks by viewModel.tasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    var isConnected by remember { mutableStateOf(false) }
    var showSyncPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val nodeClient = Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            isConnected = nodes.isNotEmpty()
            if (isConnected) {
                showSyncPrompt = true
            }
        }.addOnFailureListener {
            isConnected = false
        }
        
        // Auto-sync after local DB modifications
        viewModel.onDatabaseChanged = {
            syncManager.syncAllTasksToWatch()
        }
    }

    if (showSyncPrompt) {
        AlertDialog(
            onDismissRequest = { showSyncPrompt = false },
            title = { Text("已连接到手表") },
            text = { Text("是否立即同步最新数据？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        syncManager.performTwoWaySync()
                        showSyncPrompt = false
                        Toast.makeText(context, "已触发双向同步", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("同步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncPrompt = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("控制中心", fontWeight = FontWeight.Bold) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        // Connection Status Indicator
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Watch Status",
                            tint = if (isConnected) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Manual Sync Button
                        IconButton(onClick = { 
                            syncManager.performTwoWaySync()
                            Toast.makeText(context, "正在双向同步...", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync to Watch")
                        }
                        val scope = rememberCoroutineScope()
                        // Clear All Button (For Testing)
                        IconButton(onClick = { 
                            syncManager.sendClearAllToWatch()
                            viewModel.clearAllTasks()
                            Toast.makeText(context, "已清空本地数据库", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color(0xFFE53935))
                        }
                        // Import 11 dummy tasks
                        IconButton(onClick = {
                            scope.launch {
                                syncManager.sendClearAllToWatch()
                                kotlinx.coroutines.delay(500)
                                viewModel.importDummyTasks(11)
                                Toast.makeText(context, "已导入 11 条测试数据并同步", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.LibraryAdd, contentDescription = "Import Test Data", tint = Color(0xFF1E88E5))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onComplete = {
                        viewModel.completeTask(task.id)
                    },
                    onDelete = {
                        viewModel.deleteTask(task)
                    },
                    onPin = {
                        viewModel.pinTask(task.id)
                    }
                )
            }
        }

        if (showAddDialog) {
            AddTaskBottomSheet(
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, timeStr, dueDate, priority ->
                    viewModel.addTask(title, desc, timeStr, dueDate, priority)
                    showAddDialog = false
                    Toast.makeText(context, "已同步至手表", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: RadarTask,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.5f }, // Require swiping at least 50% to trigger
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirmDialog = true
                false // Bounce back, we handle deletion in dialog
            } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                showCompleteConfirmDialog = true
                false // Bounce back, we handle completion in dialog
            } else {
                true
            }
        }
    )
    
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除任务 \"${task.title}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text("删除", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCompleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmDialog = false },
            title = { Text("确认完成") },
            text = { Text("确定要将任务 \"${task.title}\" 标记为已完成吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompleteConfirmDialog = false
                        onComplete()
                    }
                ) {
                    Text("完成", color = Color(0xFF43A047))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF43A047) // Green for complete
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935) // Red for delete
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.Check, contentDescription = "Complete", tint = Color.White)
                } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    ) {
        val priorityColor = when (task.priority) {
            TaskPriority.EMERGENCY -> Color(0xFFE53935)
            TaskPriority.IMPORTANT -> Color(0xFFFB8C00)
            TaskPriority.REGULAR -> Color(0xFF43A047)
            TaskPriority.LONG_TERM -> Color(0xFF1E88E5)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(priorityColor, shape = RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = task.time,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onPin) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Top",
                        tint = if (task.isPinned) Color(0xFFFFB300) else Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long?, TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.REGULAR) }
    
    // Add 1 hour by default
    val defaultCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    var selectedTime by remember { mutableStateOf(defaultCal.timeInMillis) }
    
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("新增任务", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述 (可选)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TaskPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) }
                    )
                }
            }

            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedTime }
                    android.app.DatePickerDialog(context, { _, y, m, d ->
                        android.app.TimePickerDialog(context, { _, h, min ->
                            cal.set(y, m, d, h, min)
                            selectedTime = cal.timeInMillis
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("选择时间: ${dateFormat.format(Date(selectedTime))}")
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val timeStr = dateFormat.format(Date(selectedTime))
                        onSave(title, description, timeStr, selectedTime, priority)
                    } else {
                        Toast.makeText(context, "请输入标题", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存任务")
            }
        }
    }
}
