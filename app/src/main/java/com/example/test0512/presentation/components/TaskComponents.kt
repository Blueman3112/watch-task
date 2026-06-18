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
import androidx.wear.compose.material3.Text
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
    onAddTaskClick: () -> Unit
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

    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }

    // ── 双色状态系统 ──
    val colorNormal = Color(0xFF4CAF50)   // 绿色：正常状态（待处理/长远）
    val colorAlert = Color(0xFFFF5252)    // 红色：警戒状态（紧急/重要）
    val colorNeutral = Color(0xFF78909C)  // 中性灰：引导线等辅助元素

    // 根据优先级返回对应颜色
    fun priorityColor(priority: TaskPriority): Color = when (priority) {
        TaskPriority.EMERGENCY, TaskPriority.IMPORTANT -> colorAlert
        TaskPriority.REGULAR, TaskPriority.LONG_TERM -> colorNormal
    }

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
                    onLongPress = { tapOffset ->
                        if (entranceProgress.value < 1f) return@detectTapGestures
                        
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val centerHitRadius = if (isTopMode) 45f else 35f
                        
                        if ((tapOffset - Offset(centerX, centerY)).getDistance() < centerHitRadius) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onAddTaskClick()
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
                                val virtualIndex = i - progress
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
        drawPath(helperPath, color = colorNeutral.copy(alpha = 0.06f), style = Stroke(width = 1f))

        if (tasks.isEmpty()) return@Canvas

        for (i in (tasks.size - 1) downTo 1) {
            val virtualIndex = i - progress
            if (virtualIndex <= 0f) continue
            val delayI = (i - 1) * 0.08f
            val rawP = (eProgress - delayI) / 0.45f
            val entranceFactor = FastOutSlowInEasing.transform(rawP.coerceIn(0f, 1f))
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
            
            val currentTask = tasks[i]
            val isAlertTask = currentTask.priority == TaskPriority.EMERGENCY || currentTask.priority == TaskPriority.IMPORTANT
            val taskColor = priorityColor(currentTask.priority)
            val currentBreathing = if (isAlertTask) breathingUrgent else breathingBase

            val baseSize = 11f
            val sizeScale = (1.2f - (virtualIndex * 0.08f)).coerceIn(0.5f, 1.2f)
            val nodeRadius = (if (isHovered) 16f else baseSize * sizeScale) * (if (isHovered) 1f else currentBreathing)

            val distanceAlpha = (0.8f - (virtualIndex * 0.06f)).coerceIn(0.1f, 0.8f)
            val finalAlpha = if (isTopMode) (if (isHovered) 1f else 0.05f) else distanceAlpha * edgeFadeFactor * entranceFactor

            val nodeColor = if (isHovered) Color.White else taskColor.copy(alpha = finalAlpha)

            // 警戒任务（紧急/重要）显示红色脉冲环
            if (isAlertTask) {
                drawCircle(
                    color = (if (isHovered) Color.White else colorAlert).copy(alpha = finalAlpha * 0.4f * pulseScale),
                    radius = nodeRadius * 2.2f,
                    center = pos,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 来源微标记：日历任务加细外环，微信任务加小脉冲点
            when (currentTask.source) {
                TaskSource.CALENDAR -> {
                    drawCircle(
                        color = taskColor.copy(alpha = finalAlpha * 0.5f),
                        radius = nodeRadius * 1.6f,
                        center = pos,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                TaskSource.WECHAT_IMPORT -> {
                    drawCircle(
                        color = taskColor.copy(alpha = finalAlpha * 0.8f),
                        radius = 3f,
                        center = Offset(pos.x + nodeRadius + 4f, pos.y - nodeRadius + 2f)
                    )
                }
                else -> { /* MANUAL: 无额外标记 */ }
            }

            drawCircle(color = nodeColor, radius = nodeRadius, center = pos)
        }

        if (isTopMode) {
            tasks.getOrNull(topCursorIndex)?.let { hoveredTask ->
                val hoveredColor = priorityColor(hoveredTask.priority)
                val sourceLabel = " ${hoveredTask.source.icon}"
                val prioLayout = textMeasurer.measure("· ${hoveredTask.priority.label}$sourceLabel ·", TextStyle(color = hoveredColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
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
            // 中心核心球颜色：跟随最高优先级任务联动
            val topTask = tasks.firstOrNull()
            val coreColor = if (topTask != null) priorityColor(topTask.priority) else colorNormal
            drawCircle(color = coreColor.copy(alpha = 0.2f * pulseScale), radius = 28f, center = Offset(centerX, centerY))
            drawCircle(color = Color.Black, radius = 22f, center = Offset(centerX, centerY))
            val coreBreathing = if (topTask?.priority == TaskPriority.EMERGENCY || topTask?.priority == TaskPriority.IMPORTANT) breathingUrgent else breathingBase
            drawCircle(color = coreColor.copy(alpha = minOf(eProgress * 2f, 1f)), radius = 18f * (0.8f + 0.2f * coreBreathing), center = Offset(centerX, centerY))
        }
    }
}

@Composable
fun AddTaskScreen(
    onDismiss: () -> Unit,
    onSave: (String, String, String, TaskPriority) -> Unit
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
                Text("新建任务", color = Color(0xFF4DB6AC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    CompactActionButton(
                        text = "取消",
                        bgColor = Color(0xFF222222),
                        fgColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        onDismiss()
                    }
                    CompactActionButton(
                        text = "保存",
                        bgColor = Color(0xFF00796B),
                        fgColor = Color.White,
                        modifier = Modifier.weight(1f)
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
    onPinToTop: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isAlert = task.priority == TaskPriority.EMERGENCY || task.priority == TaskPriority.IMPORTANT
            val detailBgColor = if (isAlert) Color(0xFF6A1B1A) else Color(0xFF1B5E20).copy(alpha = 0.6f)
            val detailFgColor = if (isAlert) Color(0xFFFFCDD2) else Color(0xFFA5D6A7)

            Box(
                modifier = Modifier
                    .background(color = detailBgColor, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                BasicText(
                    text = "${task.source.icon} ${task.priority.label} · ${task.time}",
                    style = TextStyle(color = detailFgColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

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
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactActionButton("关闭", Color(0xFF222222), Color.White, Modifier.weight(1f)) { onClose() }
                
                if (!isAlreadyTop) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularActionButton("Top", Color(0xFF5D4037), Color(0xFFFFCCBC)) { onPinToTop() }
                    }
                }

                CompactActionButton("完成", if (isAlert) Color(0xFFC62828) else Color(0xFF2E7D32), Color.White, Modifier.weight(1f)) { onComplete() }
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
