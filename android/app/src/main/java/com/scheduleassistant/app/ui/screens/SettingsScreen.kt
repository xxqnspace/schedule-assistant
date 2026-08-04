package com.scheduleassistant.app.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.scheduleassistant.app.data.model.Countdown
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.ui.MainViewModel
import com.scheduleassistant.app.ui.components.CardSurface
import com.scheduleassistant.app.ui.components.ChipGroup
import com.scheduleassistant.app.ui.components.FormSectionTitle
import com.scheduleassistant.app.ui.components.LabeledTextField
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.isValidDate
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.ui.components.showDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onEditSection: (Section) -> Unit,
    onAddSection: () -> Unit,
    onEditCountdown: (Countdown) -> Unit,
    onAddCountdown: () -> Unit,
    onEditOverride: (Override) -> Unit,
    onAddOverride: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val meta by vm.meta.collectAsState()
    val settings by vm.settings.collectAsState()
    val sections by vm.sections.collectAsState()
    val overrides by vm.overrides.collectAsState()
    val overrideCourses by vm.overrideCourses.collectAsState()
    val countdowns by vm.countdowns.collectAsState()

    // 修复（M10）：meta 变化（如导入）后同步本地表单状态
    var semesterName by remember { mutableStateOf(meta.semesterName) }
    var semesterStart by remember { mutableStateOf(meta.semesterStart) }
    var userName by remember { mutableStateOf(meta.userName) }
    LaunchedEffect(meta) {
        semesterName = meta.semesterName
        semesterStart = meta.semesterStart
        userName = meta.userName
    }

    var showReset by remember { mutableStateOf(false) }

    // 修复（H1/H2）：通知权限与精确闹钟权限状态展示（从设置页返回时自动刷新）
    var notifEnabled by remember { mutableStateOf(false) }
    var exactAlarmOk by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        exactAlarmOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        onPauseOrDispose { }
    }

    // 导出：让用户选择保存位置
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching {
                val json = vm.exportJson()
                context.contentResolver.openOutputStream(uri)?.writer()?.use { it.write(json) }
                ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "已导出数据", Toast.LENGTH_SHORT).show() }
            }.onFailure {
                ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "导出失败：${it.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // 导入：让用户选择文件
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                vm.importJson(text)
                ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "已导入数据", Toast.LENGTH_SHORT).show() }
            }.onFailure {
                // 修复（L6）：展示真实失败原因
                ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "导入失败：${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- 学期信息 ----
        item {
            CardSurface {
                FormSectionTitle("学期信息")
                LabeledTextField("学期名称", semesterName, { semesterName = it }, placeholder = "如：2025-2026 学年第二学期")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("学期起始日（建议为周一）")
                        LabeledTextField("yyyy-MM-dd", semesterStart, { semesterStart = it }, placeholder = "如：2026-02-16")
                    }
                    IconButton(onClick = { showDatePicker(context, semesterStart.ifBlank { nowDateStr() }) { semesterStart = it } }) {
                        Icon(Icons.Filled.Edit, "选择日期")
                    }
                }
                LabeledTextField("姓名 / 称呼", userName, { userName = it }, placeholder = "可选")
                Button(
                    onClick = {
                        // 修复（M9）：学期起始日格式校验，非法提示且不保存
                        val start = semesterStart.trim()
                        if (start.isNotBlank() && !isValidDate(start)) {
                            Toast.makeText(context, "学期起始日格式应为 yyyy-MM-dd", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        vm.saveMeta(semesterName.trim(), start, userName.trim())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存学期信息") }
            }
        }

        // ---- 提醒与外观 ----
        item {
            CardSurface {
                FormSectionTitle("提醒设置")
                Text("默认提前提醒（分钟）", style = MaterialTheme.typography.labelMedium)
                ChipGroup(
                    listOf("5" to "5", "10" to "10", "15" to "15", "30" to "30", "60" to "60"),
                    settings.defaultReminder.toString()
                ) { vm.updateSettings { copy(defaultReminder = it.toIntOrNull() ?: 10) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("提醒声音", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = settings.enableSound, onCheckedChange = { vm.updateSettings { copy(enableSound = it) } })
                }

                // 修复（H2）：通知权限状态 + 跳转引导
                FormSectionTitle("通知权限")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (notifEnabled) "通知：已开启" else "通知：未开启（收不到提醒）",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (notifEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notifEnabled) {
                        OutlinedButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        }) { Text("去开启") }
                    }
                }

                // 修复（H1）：精确闹钟权限状态 + 跳转引导（Android 12+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (exactAlarmOk) "精确闹钟：已开启" else "精确闹钟：未开启（提醒可能延迟）",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (exactAlarmOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        if (!exactAlarmOk) {
                            OutlinedButton(onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                            .setData(Uri.parse("package:${context.packageName}"))
                                    )
                                }
                            }) { Text("去开启") }
                        }
                    }
                }

                FormSectionTitle("外观")
                Text("主题", style = MaterialTheme.typography.labelMedium)
                ChipGroup(
                    listOf("light" to "浅色", "dark" to "深色"),
                    settings.theme
                ) { vm.updateSettings { copy(theme = it) } }
            }
        }

        // ---- 节次管理 ----
        item {
            CardSurface {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FormSectionTitle("节次管理")
                    Box(Modifier.weight(1f))
                    IconButton(onClick = onAddSection) { Icon(Icons.Filled.Add, "添加节次") }
                }
                if (sections.isEmpty()) {
                    Text("尚未设置节次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                for (s in sections) {
                    Row(Modifier.fillMaxWidth().clickable { onEditSection(s) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${s.start} - ${s.end}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onEditSection(s) }) { Icon(Icons.Filled.Edit, "编辑") }
                    }
                }
            }
        }

        // ---- 调课 / 调休 ----
        item {
            CardSurface {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FormSectionTitle("调课 / 调休")
                    Box(Modifier.weight(1f))
                    IconButton(onClick = onAddOverride) { Icon(Icons.Filled.Add, "添加") }
                }
                if (overrides.isEmpty()) {
                    Text("暂无调课/调休安排", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                for (o in overrides.sortedBy { it.date }) {
                    val label = when (o.mode) {
                        "cancel" -> "停课"
                        "copyWeekday" -> "按${WEEKDAY_NAMES[(o.copyWeekday ?: 1) - 1]}课表"
                        "custom" -> "自定义课表（${overrideCourses.count { it.overrideId == o.id }} 节）"
                        else -> o.mode
                    }
                    Row(Modifier.fillMaxWidth().clickable { onEditOverride(o) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(o.date, style = MaterialTheme.typography.bodyLarge)
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onEditOverride(o) }) { Icon(Icons.Filled.Edit, "编辑") }
                    }
                }
            }
        }

        // ---- 倒计时 ----
        item {
            CardSurface {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FormSectionTitle("首页倒计时（最多 3 个）")
                    Box(Modifier.weight(1f))
                    IconButton(onClick = onAddCountdown, enabled = countdowns.size < 3) { Icon(Icons.Filled.Add, "添加倒计时") }
                }
                if (countdowns.isEmpty()) {
                    Text("尚未添加倒计时", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                for (cd in countdowns) {
                    val accent = runCatching { Color(android.graphics.Color.parseColor(cd.color)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth().clickable { onEditCountdown(cd) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(accent).padding(end = 8.dp))
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(cd.title.ifBlank { "(无标题)" }, style = MaterialTheme.typography.bodyLarge)
                            Text(cd.target.replace("T", " "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onEditCountdown(cd) }) { Icon(Icons.Filled.Edit, "编辑") }
                    }
                }
            }
        }

        // ---- 数据 ----
        item {
            CardSurface {
                FormSectionTitle("数据备份")
                Text("导出 / 导入与网页版 schedule-data.json 完全兼容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("schedule-data.json") }, Modifier.weight(1f)) {
                        Icon(Icons.Filled.FileDownload, null)
                        Text(" 导出")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, Modifier.weight(1f)) {
                        Icon(Icons.Filled.FileUpload, null)
                        Text(" 导入")
                    }
                }
                Button(
                    onClick = { showReset = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Restore, null)
                    Text(" 重置全部数据")
                }
            }
        }

        item { Box(Modifier.fillMaxWidth().padding(8.dp)) }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("确认重置？") },
            text = { Text("将清空全部课程、日程、倒计时与设置，并恢复默认数据。此操作不可撤销。") },
            confirmButton = {
                Button(onClick = { showReset = false; vm.resetAll() }) { Text("重置") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReset = false }) { Text("取消") }
            }
        )
    }
}
