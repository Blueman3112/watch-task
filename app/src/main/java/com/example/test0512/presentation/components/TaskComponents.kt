package com.example.test0512.presentation.components

import android.view.HapticFeedbackConstants
import android.app.TimePickerDialog
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import com.example.test0512.model.TaskSource
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import android.graphics.Path as NativePath
import android.graphics.Paint as NativePaint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.text.drawText
import java.util.Calendar
import java.util.Locale

@Composable
fun RadarSpiralScreen(
    tasks: List<RadarTask>,
    onTaskClick: (RadarTask) -> Unit,
    onTopConfirm: (Int) -> Unit,
    onAddTaskClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    isShowSpiralLines: Boolean = true
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val scrollOffset = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()

    val focusRequester = remember { FocusRequester() }

    val entranceProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceProgress.animateTo(1f, tween(1600, easing = LinearEasing))
        focusRequester.requestFocus()
    }

    var isTopMode by remember { mutableStateOf(false) }
    var topCursorIndex by remember { mutableIntStateOf(1) }

    val MAX_TASK_CAPACITY = 12
    var showCapacityWarning by remember { mutableStateOf(false) }

    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }

    val taskPositions = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    LaunchedEffect(tasks.toList()) {
        val currentIds = tasks.map { it.id }.toSet()
        taskPositions.keys.retainAll(currentIds)
        
        tasks.forEachIndexed { index, task ->
            if (!taskPositions.containsKey(task.id)) {
                taskPositions[task.id] = Animatable(index.toFloat() + 1f)
            }
            launch {
                taskPositions[task.id]?.animateTo(
                    targetValue = index.toFloat(),
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val baseTeal = Color(0xFF4DB6AC)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    val breathingBase by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathing_base"
    )

    val breathingUrgent by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathing_urgent"
    )

    val spiralA = 42f
    val spiralB = 14f
    val thetaMultiplier = 1.1f

    fun adjustCameraForCursor(targetIdx: Int) {
        coroutineScope.launch {
            val vIdx = targetIdx - scrollOffset.value
            if (vIdx > 4.5f) {
                scrollOffset.animateTo((targetIdx - 4.5f).coerceAtLeast(0f), tween(250, easing = FastOutSlowInEasing))
            }
            else if (vIdx < 1.2f) {
                scrollOffset.animateTo(max(0f, targetIdx - 1.5f), tween(250, easing = FastOutSlowInEasing))
            }
        }
    }

    val unifiedScrollState = rememberScrollableState { delta ->
        if (entranceProgress.value < 1f) return@rememberScrollableState 0f
        
        val isSystemLocked = tasks.isNotEmpty() && tasks.first().priority == TaskPriority.EMERGENCY
        if (isSystemLocked && !isTopMode) {
            return@rememberScrollableState 0f
        }

        if (isTopMode) {
            rotaryAccumulator += delta
            val threshold = 40f
            if (rotaryAccumulator < -threshold && topCursorIndex < tasks.size - 1) {
                topCursorIndex++
                rotaryAccumulator = 0f
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                adjustCameraForCursor(topCursorIndex)
            } else if (rotaryAccumulator > threshold && topCursorIndex > 1) {
                topCursorIndex--
                rotaryAccumulator = 0f
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                adjustCameraForCursor(topCursorIndex)
            }
            delta
        } else {
            val pixelsPerItem = 80f
            val newProgress = (scrollOffset.value - delta / pixelsPerItem).coerceIn(0f, (tasks.size - 1).toFloat())
            val consumedProgress = scrollOffset.value - newProgress
            coroutineScope.launch {
                scrollOffset.snapTo(newProgress)
            }
            consumedProgress * pixelsPerItem
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(unifiedScrollState),
                focusRequester = focusRequester
            )
            .scrollable(
                state = unifiedScrollState,
                orientation = Orientation.Vertical
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { focusRequester.requestFocus() },
                    onDoubleTap = { onSettingsClick() },
                    onLongPress = { tapOffset ->
                        if (entranceProgress.value < 1f) return@detectTapGestures
                        
                        val isSystemLocked = tasks.isNotEmpty() && tasks.first().priority == TaskPriority.EMERGENCY
                        if (isSystemLocked) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            return@detectTapGestures
                        }
                        
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val centerHitRadius = if (isTopMode) 45f else 35f
                        
                        if ((tapOffset - Offset(centerX, centerY)).getDistance() < centerHitRadius) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            if (tasks.size >= MAX_TASK_CAPACITY) {
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                showCapacityWarning = true
                            } else {
                                onAddTaskClick()
                            }
                        } else {
                            isTopMode = !isTopMode
                            if (isTopMode && tasks.size > 1) {
                                topCursorIndex = (scrollOffset.value + 1f).toInt().coerceIn(1, tasks.size - 1)
                            }
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    },
                    onTap = { tapOffset ->
                        if (entranceProgress.value < 1f) return@detectTapGestures
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val progress = scrollOffset.value
                        val centerHitRadius = if (isTopMode) 45f else 35f

                        if ((tapOffset - Offset(centerX, centerY)).getDistance() < centerHitRadius) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            if (isTopMode && tasks.isNotEmpty()) {
                                onTopConfirm(topCursorIndex)
                                isTopMode = false
                                coroutineScope.launch {
                                    scrollOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
                                }
                            } else if (tasks.isNotEmpty()) {
                                onTaskClick(tasks[0])
                            }
                            return@detectTapGestures
                        }

                        if (!isTopMode) {
                            for (i in 1 until tasks.size) {
                                val logicalIndex = taskPositions[tasks[i].id]?.value ?: i.toFloat()
                                val virtualIndex = logicalIndex - progress
                                if (virtualIndex <= 0f) continue
                                val theta = virtualIndex * thetaMultiplier
                                val r = spiralA + spiralB * theta
                                val pos = Offset(centerX + r * cos(theta), centerY + r * sin(theta))
                                if ((tapOffset - pos).getDistance() < 25f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    onTaskClick(tasks[i])
                                    break
                                }
                            }
                        }
                    }
                )
            }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val progress = scrollOffset.value
        val maxScreenRadius = size.width / 2f
        val edgeFadeZone = 50f
        val eProgress = entranceProgress.value
        val nativeCanvas = drawContext.canvas.nativeCanvas

        if (isShowSpiralLines) {
            val helperPath = androidx.compose.ui.graphics.Path()
            val steps = 100
            val maxTheta = (tasks.size.toFloat() + 1f) * thetaMultiplier
            for (s in 0..steps) {
                val t = (s.toFloat() / steps) * maxTheta
                val r = spiralA + spiralB * t
                val x = centerX + r * cos(t).toFloat()
                val y = centerY + r * sin(t).toFloat()
                if (s == 0) helperPath.moveTo(x, y) else helperPath.lineTo(x, y)
            }
            drawPath(helperPath, color = baseTeal.copy(alpha = 0.08f), style = Stroke(width = 1f))
        }

        if (tasks.isEmpty()) return@Canvas

        for (i in (tasks.size - 1) downTo 1) {
            val currentTask = tasks[i]
            val logicalIndex = taskPositions[currentTask.id]?.value ?: i.toFloat()
            val virtualIndex = logicalIndex - progress
            if (virtualIndex <= 0f) continue
            
            val entranceFactor = if (eProgress >= 0.99f) {
                1f
            } else {
                val delayIndex = minOf(logicalIndex - 1f, 15f)
                val delayI = delayIndex * 0.05f
                val rawP = (eProgress - delayI) / 0.25f
                FastOutSlowInEasing.transform(rawP.coerceIn(0f, 1f))
            }
            if (entranceFactor <= 0f) continue

            val targetTheta = virtualIndex * thetaMultiplier
            val currentTheta = entranceFactor * targetTheta
            val r = entranceFactor * spiralA + spiralB * currentTheta
            val edgeFadeFactor = if (r > maxScreenRadius - edgeFadeZone) (maxScreenRadius - r) / edgeFadeZone else 1f
            if (edgeFadeFactor <= 0f) continue

            val x = centerX + r * cos(currentTheta).toFloat()
            val y = centerY + r * sin(currentTheta).toFloat()
            val pos = Offset(x, y)
            val isHovered = isTopMode && i == topCursorIndex
            
            val isEmergency = currentTask.priority == TaskPriority.EMERGENCY
            val currentBreathing = if (isEmergency) breathingUrgent else breathingBase

            val priorityScale = when (currentTask.priority) {
                TaskPriority.EMERGENCY -> 1.5f
                TaskPriority.IMPORTANT -> 1.2f
                TaskPriority.REGULAR -> 1.0f
                TaskPriority.LONG_TERM -> 0.8f
            }
            val baseSize = 11f
            val posScale = (1.2f - (virtualIndex * 0.08f)).coerceIn(0.5f, 1.2f)
            val finalScale = posScale * priorityScale
            val nodeRadius = (if (isHovered) 16f * priorityScale else baseSize * finalScale) * (if (isHovered) 1f else currentBreathing)

            val distanceAlpha = (0.8f - (virtualIndex * 0.06f)).coerceIn(0.1f, 0.8f)
            val finalAlpha = if (isTopMode) (if (isHovered) 1f else 0.05f) else distanceAlpha * edgeFadeFactor * entranceFactor

            val nodeColor = if (isHovered) Color.White else baseTeal.copy(alpha = finalAlpha)

            if (isEmergency) {
                drawCircle(
                    color = (if (isHovered) Color.White else baseTeal).copy(alpha = finalAlpha * 0.4f * pulseScale), 
                    radius = nodeRadius * 1.8f, 
                    center = pos,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            drawCircle(color = nodeColor, radius = nodeRadius, center = pos)
        }

        if (isTopMode) {
            tasks.getOrNull(topCursorIndex)?.let { hoveredTask ->
                val priorityColor = if (hoveredTask.priority == TaskPriority.EMERGENCY) Color(0xFFFF5252) else baseTeal
                val prioLayout = textMeasurer.measure("· ${hoveredTask.priority.label} ·", TextStyle(color = priorityColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                drawText(prioLayout, topLeft = Offset(centerX - prioLayout.size.width / 2f, 24f))

                val rArc = maxScreenRadius - 32f
                val bottomPath = NativePath().apply { addArc(RectF(centerX - rArc, centerY - rArc, centerX + rArc, centerY + rArc), 180f, -180f) }
                val titlePaint = NativePaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 15.sp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val displayTitle = if (hoveredTask.title.length > 10) hoveredTask.title.take(9) + ".." else hoveredTask.title
                nativeCanvas.drawTextOnPath(displayTitle, bottomPath, ( (Math.PI * rArc).toFloat() - titlePaint.measureText(displayTitle) ) / 2f, 0f, titlePaint)

                drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = 32f * breathingBase, center = Offset(centerX, centerY))
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 32f * breathingBase, center = Offset(centerX, centerY), style = Stroke(width = 2f))
                val topTextLayout = textMeasurer.measure("TOP", TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Black))
                drawText(topTextLayout, topLeft = Offset(centerX - topTextLayout.size.width / 2f, centerY - topTextLayout.size.height / 2f))
            }
        } else {
            val isSystemLocked = tasks.isNotEmpty() && tasks.first().priority == TaskPriority.EMERGENCY
            if (isSystemLocked) {
                val lockRed = Color(0xFFFF5252)
                drawCircle(
                    color = lockRed.copy(alpha = 0.3f * pulseScale),
                    radius = maxScreenRadius - 10f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 15f)
                )
                val textLayout = textMeasurer.measure("LOCKED", TextStyle(color = lockRed.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Black))
                drawText(textLayout, topLeft = Offset(centerX - textLayout.size.width / 2f, centerY - 60f))
                
                drawCircle(color = lockRed.copy(alpha = 0.2f * pulseScale), radius = 28f, center = Offset(centerX, centerY))
                drawCircle(color = Color.Black, radius = 22f, center = Offset(centerX, centerY))
                drawCircle(color = lockRed.copy(alpha = minOf(eProgress * 2f, 1f)), radius = 18f * (0.8f + 0.4f * breathingUrgent), center = Offset(centerX, centerY))
            } else {
                val coreRed = Color(0xFFFF5252)
                drawCircle(color = coreRed.copy(alpha = 0.2f * pulseScale), radius = 28f, center = Offset(centerX, centerY))
                drawCircle(color = Color.Black, radius = 22f, center = Offset(centerX, centerY))
                drawCircle(color = coreRed.copy(alpha = minOf(eProgress * 2f, 1f)), radius = 18f * (0.8f + 0.2f * breathingUrgent), center = Offset(centerX, centerY))
            }
        }
    } // End of Canvas

    if (showCapacityWarning) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.wear.compose.material3.Text(
                    text = "容量已满",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                )
                Spacer(modifier = Modifier.height(2.dp))
                androidx.wear.compose.material3.Text(
                    text = "请先专注并清理已积压的任务",
                    style = TextStyle(color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Normal)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TaskPriority.values().forEach { prio ->
                        val count = tasks.count { it.priority == prio }
                        if (count > 0) {
                            val color = when (prio) {
                                TaskPriority.EMERGENCY -> Color(0xFFFF5252)
                                TaskPriority.IMPORTANT -> Color(0xFFFFAB40)
                                TaskPriority.REGULAR -> Color(0xFF4DB6AC)
                                TaskPriority.LONG_TERM -> Color(0xFF90A4AE)
                            }
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
                                Spacer(modifier = Modifier.width(3.dp))
                                androidx.wear.compose.material3.Text(text = "$count", style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                IconActionButton(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    bgColor = Color(0xFF00796B),
                    fgColor = Color.White
                ) {
                    showCapacityWarning = false
                    isTopMode = true
                    if (tasks.size > 1) {
                        topCursorIndex = 1
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskScreen(
    onDismiss: () -> Unit,
    onSave: (String, String, String, TaskPriority) -> Unit,
    onWechatImport: () -> Unit,
    onCalendarSync: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var time by remember { mutableStateOf(java.time.LocalTime.now().run { String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute) }) }
    var priority by remember { mutableStateOf(TaskPriority.REGULAR) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    CompactActionButton(text = "微信导入", bgColor = Color(0xFF07C160), fgColor = Color.White, modifier = Modifier.weight(1f)) { onWechatImport() }
                    CompactActionButton(text = "日历同步", bgColor = Color(0xFF4285F4), fgColor = Color.White, modifier = Modifier.weight(1f)) { onCalendarSync() }
                }
            }

            item {
                TaskInputField(
                    value = title,
                    onValueChange = { title = it },
                    label = "任务名称"
                )
            }

            item {
                TaskInputField(
                    value = description,
                    onValueChange = { description = it },
                    label = "描述"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true }
                    ) {
                        Text("日期", fontSize = 10.sp, color = Color(0xFF4DB6AC))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayDate = date.substring(5) // e.g., "06-09"
                            Text(displayDate, color = Color.White, fontSize = 14.sp)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f).clickable { showTimePicker = true }
                    ) {
                        Text("时间", fontSize = 10.sp, color = Color(0xFF4DB6AC))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(time, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("紧急程度", fontSize = 10.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TaskPriority.entries.forEach { p ->
                            PriorityButton(priority = p, isSelected = priority == p, onClick = { priority = p })
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    IconActionButton(
                        imageVector = Icons.Default.Close,
                        bgColor = Color(0xFF222222),
                        fgColor = Color.LightGray
                    ) {
                        onDismiss()
                    }
                    IconActionButton(
                        imageVector = Icons.Default.Check,
                        bgColor = Color(0xFF00796B),
                        fgColor = Color.White
                    ) {
                        if (title.isNotBlank()) onSave(title, description, "$date $time", priority)
                    }
                }
            }
        }

        if (showDatePicker) {
            val dateParts = date.split("-")
            val initYear = dateParts.getOrNull(0)?.toIntOrNull() ?: java.time.LocalDate.now().year
            val initMonth = dateParts.getOrNull(1)?.toIntOrNull() ?: java.time.LocalDate.now().monthValue
            val initDay = dateParts.getOrNull(2)?.toIntOrNull() ?: java.time.LocalDate.now().dayOfMonth
            
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                androidx.wear.compose.material3.DatePicker(
                    initialDate = java.time.LocalDate.of(initYear, initMonth, initDay),
                    onDatePicked = { pickedDate ->
                        date = pickedDate.toString()
                        showDatePicker = false
                    }
                )
            }
        }

        if (showTimePicker) {
            val parts = time.split(":")
            val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
            val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                androidx.wear.compose.material3.TimePicker(
                    initialTime = java.time.LocalTime.of(initHour, initMinute),
                    onTimePicked = { pickedTime ->
                        time = String.format(Locale.getDefault(), "%02d:%02d", pickedTime.hour, pickedTime.minute)
                        showTimePicker = false
                    }
                )
            }
        }
    }
}

@Composable
fun TaskInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val results = android.app.RemoteInput.getResultsFromIntent(result.data)
            val input = results?.getCharSequence("result_key")?.toString()
            if (input != null) {
                onValueChange(input)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clickable {
                val remoteInput = android.app.RemoteInput.Builder("result_key").setLabel(label).build()
                val intent = android.content.Intent("android.support.wearable.input.action.REMOTE_INPUT").apply {
                    putExtra("android.support.wearable.input.extra.REMOTE_INPUTS", arrayOf(remoteInput))
                }
                try {
                    launcher.launch(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            }
    ) {
        Text(label, fontSize = 10.sp, color = Color(0xFF4DB6AC))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (value.isEmpty()) {
                Text("点击键入...", color = Color.DarkGray, fontSize = 14.sp)
            } else {
                Text(value, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun TaskDetailScreen(
    task: RadarTask,
    isAlreadyTop: Boolean,
    onClose: () -> Unit,
    onComplete: () -> Unit,
    onPinToTop: () -> Unit,
    onUpdatePriority: (TaskPriority) -> Unit = {},
    onUpdateTime: (String) -> Unit = {}
) {
    val view = LocalView.current
    var showPriorityPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(task.time, task.priority) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            
                            if (distance > centerX - 45.dp.toPx()) {
                                val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle in -106f..-74f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    showTimePicker = true
                                } else if (angle in -62f..-34f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    showPriorityPicker = true
                                }
                            }
                        }
                    )
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = size.width / 2f
            val arcRadius = maxRadius - 10.dp.toPx()
            val rectF = android.graphics.RectF(
                centerX - arcRadius, centerY - arcRadius, 
                centerX + arcRadius, centerY + arcRadius
            )
            val strokeWidth = 14.dp.toPx()
            
            val topLeftOffset = Offset(centerX - arcRadius, centerY - arcRadius)
            val arcSize = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)
            
            // Source Arc
            val capsuleBgColor = Color(0xFF2C2C2C)
            drawArc(
                color = capsuleBgColor,
                startAngle = -146f,
                sweepAngle = 28f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Time Arc
            drawArc(
                color = capsuleBgColor,
                startAngle = -106f,
                sweepAngle = 32f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Priority Arc
            drawArc(
                color = capsuleBgColor,
                startAngle = -62f,
                sweepAngle = 28f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            val nativeCanvas = drawContext.canvas.nativeCanvas
            fun drawCurvedText(text: String, startAngle: Float, sweepAngle: Float, color: Int, sizeSp: Float = 9.5f, bold: Boolean = true) {
                val path = android.graphics.Path()
                path.addArc(rectF, startAngle, sweepAngle)
                val paint = android.graphics.Paint().apply {
                    this.color = color
                    textSize = sizeSp.sp.toPx()
                    isAntiAlias = true
                    if (bold) typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                val textWidth = paint.measureText(text)
                val arcLength = (2.0 * Math.PI * arcRadius * sweepAngle / 360.0).toFloat()
                val hOffset = (arcLength - textWidth) / 2f
                val vOffset = (paint.textSize / 3f)
                nativeCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
            }
            
            drawCurvedText(task.source.label, -146f, 28f, android.graphics.Color.LTGRAY)
            drawCurvedText(task.time, -106f, 32f, android.graphics.Color.WHITE)
            
            val prioTextColor = when (task.priority) {
                TaskPriority.EMERGENCY -> android.graphics.Color.parseColor("#FF5252") // 红色高亮
                TaskPriority.IMPORTANT -> android.graphics.Color.parseColor("#4CAF50") // 绿色高亮
                else -> android.graphics.Color.LTGRAY
            }
            drawCurvedText(task.priority.label, -62f, 28f, prioTextColor)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            BasicText(
                text = task.title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            BasicText(
                text = task.description,
                style = TextStyle(
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconActionButton(Icons.Default.Close, Color(0xFF222222), Color.LightGray) { onClose() }
                
                if (!isAlreadyTop) {
                    IconActionButton(Icons.Default.KeyboardArrowUp, Color(0xFF5D4037), Color(0xFFFFCCBC)) { onPinToTop() }
                }

                IconActionButton(Icons.Default.Check, Color(0xFF00796B), Color.White) { onComplete() }
            }
        }

        if (showPriorityPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f))
                    .pointerInput(Unit) { detectTapGestures(onTap = { showPriorityPicker = false }) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("修改紧急程度", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TaskPriority.entries.forEach { p ->
                            PriorityButton(priority = p, isSelected = task.priority == p, onClick = { 
                                onUpdatePriority(p)
                                showPriorityPicker = false 
                            })
                        }
                    }
                }
            }
        }

        if (showTimePicker) {
            val parts = task.time.split(":")
            val initHour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: java.time.LocalTime.now().hour
            val initMinute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: java.time.LocalTime.now().minute
            
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                androidx.wear.compose.material3.TimePicker(
                    initialTime = java.time.LocalTime.of(initHour.coerceIn(0, 23), initMinute.coerceIn(0, 59)),
                    onTimePicked = { pickedTime ->
                        val newTime = String.format(Locale.getDefault(), "%02d:%02d", pickedTime.hour, pickedTime.minute)
                        onUpdateTime(newTime)
                        showTimePicker = false
                    }
                )
            }
        }
    }
}

@Composable
fun PriorityButton(priority: TaskPriority, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = when (priority) {
        TaskPriority.EMERGENCY -> if (isSelected) Color(0xFFFF5252) else Color(0xFF3E2723)
        TaskPriority.IMPORTANT -> if (isSelected) Color(0xFFFFAB40) else Color(0xFF3E2723)
        TaskPriority.REGULAR -> if (isSelected) Color(0xFF4DB6AC) else Color(0xFF1A1A1A)
        TaskPriority.LONG_TERM -> if (isSelected) Color(0xFF90A4AE) else Color(0xFF1A1A1A)
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priority.label.take(1),
            color = if (isSelected) Color.Black else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CircularActionButton(text: String, bgColor: Color, fgColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = fgColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        )
    }
}

@Composable
fun CompactActionButton(text: String, bgColor: Color, fgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = fgColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        )
    }
}

@Composable
fun IconActionButton(imageVector: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, fgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun TaskListScreen(
    tasks: List<RadarTask>,
    onTaskClick: (RadarTask) -> Unit,
    onSettingsClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(listState),
                focusRequester = focusRequester
            )
            .focusRequester(focusRequester)
            .focusable(),
        state = listState,
        contentPadding = PaddingValues(top = 32.dp, bottom = 48.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks.size) { index ->
            TaskListItem(task = tasks[index], onClick = { onTaskClick(tasks[index]) })
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            CompactActionButton(
                text = "系统设置",
                bgColor = Color(0xFF333333),
                fgColor = Color.White,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                onClick = onSettingsClick
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun TaskListItem(task: RadarTask, onClick: () -> Unit) {
    val priorityColor = when (task.priority) {
        TaskPriority.EMERGENCY -> Color(0xFFFF5252)
        TaskPriority.IMPORTANT -> Color(0xFFFFAB40)
        TaskPriority.REGULAR -> Color(0xFF4DB6AC)
        TaskPriority.LONG_TERM -> Color(0xFF90A4AE)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(priorityColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = task.time,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SettingsScreen(
    isListViewEnabled: Boolean,
    isShowSpiralLines: Boolean,
    onViewModeToggle: (Boolean) -> Unit,
    onToggleSpiralLines: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(listState),
                focusRequester = focusRequester
            )
            .focusRequester(focusRequester)
            .focusable(),
        state = listState,
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "系统设置",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ToggleChip(
                modifier = Modifier.fillMaxWidth(),
                checked = isListViewEnabled,
                onCheckedChange = { onViewModeToggle(!isListViewEnabled) },
                label = { 
                    Text(
                        text = "原生列表视图", 
                        color = Color.White, 
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                toggleControl = {
                    Switch(
                        checked = isListViewEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = Color(0xFF333333),
                    checkedEndBackgroundColor = Color(0xFF333333),
                    uncheckedStartBackgroundColor = Color(0xFF333333),
                    uncheckedEndBackgroundColor = Color(0xFF333333)
                )
            )
        }
        item {
            ToggleChip(
                modifier = Modifier.fillMaxWidth(),
                checked = isShowSpiralLines,
                onCheckedChange = { onToggleSpiralLines(!isShowSpiralLines) },
                label = { 
                    Text(
                        text = "显示螺旋线", 
                        color = Color.White, 
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                toggleControl = {
                    Switch(
                        checked = isShowSpiralLines,
                        onCheckedChange = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = Color(0xFF333333),
                    checkedEndBackgroundColor = Color(0xFF333333),
                    uncheckedStartBackgroundColor = Color(0xFF333333),
                    uncheckedEndBackgroundColor = Color(0xFF333333)
                )
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            CompactActionButton(
                text = "返回",
                bgColor = Color(0xFF1A1A1A),
                fgColor = Color.White,
                modifier = Modifier.width(80.dp),
                onClick = onBack
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
