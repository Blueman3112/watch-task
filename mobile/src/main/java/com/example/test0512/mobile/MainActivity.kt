package com.example.test0512.mobile

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Permissions handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val neededPermissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        val missingPermissions = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }

        setContent {
            FacadeAppUI()
        }
    }
}

fun getDefaultCalendarId(context: android.content.Context): Long {
    val projection = arrayOf(CalendarContract.Calendars._ID)
    // Try to get a primary/visible calendar
    context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        "${CalendarContract.Calendars.VISIBLE} = 1",
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getLong(0)
        }
    }
    return 1 // Fallback
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacadeAppUI() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }) }
    var priority by remember { mutableStateOf("重要") }

    val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())

    val priorities = listOf(
        "紧急" to Color(0xFFE53935), 
        "重要" to Color(0xFFFB8C00), 
        "常规" to Color(0xFF43A047), 
        "长远" to Color(0xFF1E88E5)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch-Task 控制中心", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务标题", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4DB6AC),
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("任务描述 (可选)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4DB6AC),
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            Text("紧急程度", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                priorities.forEach { (label, color) ->
                    val isSelected = priority == label
                    Surface(
                        color = if (isSelected) color else Color.DarkGray,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable { priority = label }
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Text("执行时间 (雷达牵引锚点)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Surface(
                color = Color.DarkGray,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    val currentCal = selectedCalendar
                    DatePickerDialog(context, { _, year, month, day ->
                        TimePickerDialog(context, { _, hour, minute ->
                            val newCal = Calendar.getInstance().apply {
                                set(year, month, day, hour, minute, 0)
                            }
                            selectedCalendar = newCal
                        }, currentCal.get(Calendar.HOUR_OF_DAY), currentCal.get(Calendar.MINUTE), true).show()
                    }, currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH)).show()
                }
            ) {
                Text(
                    text = dateFormat.format(selectedCalendar.time),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "请输入标题", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalTitle = "!$priority $title"
                    
                    val timeInMillis = selectedCalendar.timeInMillis
                    val calId = getDefaultCalendarId(context)
                    
                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTSTART, timeInMillis)
                        put(CalendarContract.Events.DTEND, timeInMillis + 3600_000) // +1小时
                        put(CalendarContract.Events.TITLE, finalTitle)
                        put(CalendarContract.Events.DESCRIPTION, description)
                        put(CalendarContract.Events.CALENDAR_ID, calId)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    }
                    try {
                        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                        
                        // Auto-push to watch
                        val intent = android.content.Intent(context, WearSyncService::class.java)
                        intent.action = "ACTION_FORCE_SYNC"
                        context.startService(intent)
                        
                        Toast.makeText(context, "保存成功！已自动向手表推送更新", Toast.LENGTH_LONG).show()
                        title = ""
                        description = ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "保存失败，请检查日历读写权限", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DB6AC))
            ) {
                Text("保存并同步至手表", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
