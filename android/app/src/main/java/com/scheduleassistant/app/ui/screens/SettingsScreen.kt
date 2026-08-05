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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scheduleassistant.app.data.model.Countdown
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.ui.MainViewModel
import com.scheduleassistant.app.ui.components.ChipGroup
import com.scheduleassistant.app.ui.components.FormSectionTitle
import com.scheduleassistant.app.ui.components.LabeledTextField
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.backgroundImageModel
import com.scheduleassistant.app.util.backgroundImageSource
import com.scheduleassistant.app.util.isValidDate
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.ui.components.showDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ⑦ 设置页：二级菜单结构（分组卡片可展开），分组标题带图标/加粗主色。
 * ⑧ 背景图：外观分组内支持 URL 图片与本地相册上传。
 */
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

    // ⑧ 背景 URL 输入（初始取当前 bgImage，若是 http 开头）
    var bgUrlText by remember { mutableStateOf(settings.bgImage.takeIf { it.startsWith("http") } ?: "") }

    // ⑦ 分组展开状态（默认全部收起，点击展开）
    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    fun toggleSection(key: String) {
        expandedSections = if (key in expandedSections) expandedSections - key else expandedSections + key
    }

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
                ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "导入失败：${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    // ③ 背景图仅支持 URL（已去掉本地相册上传，避免相册/文件选择器兼容问题）

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- 学期信息 ----
        item {
            SettingsMenuSection(
                title = "学期信息",
                desc = "学期名称 / 起始日 / 姓名",
                icon = Icons.Filled.School,
                expanded = "semester" in expandedSections,
                onToggle = { toggleSection("semester") }
            ) {
                LabeledTextField("学期名称", semesterName, { semesterName = it }, placeholder = "如：2025-2026 学年第二学期")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("学期起始日（不必是周一）")
                        LabeledTextField("yyyy-MM-dd", semesterStart, { semesterStart = it }, placeholder = "如：2026-02-16")
                    }
                    IconButton(onClick = { showDatePicker(context, semesterStart.ifBlank { nowDateStr() }) { semesterStart = it } }) {
                        Icon(Icons.Filled.Edit, "选择日期")
                    }
                }
                LabeledTextField("姓名 / 称呼", userName, { userName = it }, placeholder = "可选")
                Button(
                    onClick = {
                        // ① 保存流程兜底：任何异常不崩溃，Toast 提示
                        runCatching {
                            val start = semesterStart.trim()
                            if (start.isNotBlank() && !isValidDate(start)) {
                                Toast.makeText(context, "学期起始日格式应为 yyyy-MM-dd", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            vm.saveMeta(semesterName.trim(), start, userName.trim())
                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "保存失败：${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存学期信息") }
            }
        }

        // ---- 提醒设置 ----
        item {
            SettingsMenuSection(
                title = "提醒设置",
                desc = "提前提醒 / 声音 / 权限",
                icon = Icons.Filled.Notifications,
                expanded = "reminder" in expandedSections,
                onToggle = { toggleSection("reminder") }
            ) {
                FormSectionTitle("默认提前提醒（分钟）")
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
                        style = MaterialTheme.typography.bodyMedium,
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
                    FormSectionTitle("精确闹钟")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (exactAlarmOk) "已开启" else "未开启（提醒可能延迟）",
                            style = MaterialTheme.typography.bodyMedium,
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
            }
        }

        // ---- 外观（含背景图） ----
        item {
            SettingsMenuSection(
                title = "外观",
                desc = "主题 / 背景图",
                icon = Icons.Filled.Palette,
                expanded = "appearance" in expandedSections,
                onToggle = { toggleSection("appearance") }
            ) {
                FormSectionTitle("主题")
                ChipGroup(
                    listOf("light" to "浅色", "dark" to "深色"),
                    settings.theme
                ) { vm.updateSettings { copy(theme = it) } }

                // ⑧ 背景图
                FormSectionTitle("背景")
                ChipGroup(
                    listOf("solid" to "纯色", "image" to "图片"),
                    settings.background
                ) { vm.updateSettings { copy(background = it) } }
                if (settings.background == "image") {
                    // ⑤ 来源标识（URL / 本地相册）
                    val source = backgroundImageSource(settings.bgImage)
                    if (source.isNotBlank()) {
                        Text(
                            source,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 预览（model 安全构造：本地文件不存在/为空时不渲染，不崩溃；限尺寸防大图 OOM）
                    val previewModel = backgroundImageModel(settings.bgImage)
                    if (previewModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(previewModel).size(800).build(),
                            contentDescription = "背景图预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (settings.bgImage.isNotBlank()) {
                        Text(
                            "图片不可用（本地文件不存在或加载失败），请重新选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledTextField(
                            "图片 URL",
                            bgUrlText,
                            { bgUrlText = it },
                            placeholder = "https://...",
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            val url = bgUrlText.trim()
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                vm.updateSettings { copy(background = "image", bgImage = url) }
                                Toast.makeText(context, "背景图已应用", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "请输入 http(s):// 开头的图片地址", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("应用") }
                    }
                    OutlinedButton(
                        onClick = { vm.updateSettings { copy(background = "solid", bgImage = "") } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("清除背景图") }
                }
            }
        }

        // ---- 节次管理 ----
        item {
            SettingsMenuSection(
                title = "节次管理",
                desc = "上课节次与时间",
                icon = Icons.Filled.Schedule,
                expanded = "sections" in expandedSections,
                onToggle = { toggleSection("sections") }
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("共 ${sections.size} 个节次", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onAddSection) { Icon(Icons.Filled.Add, "添加节次") }
                }
                if (sections.isEmpty()) {
                    Text("尚未设置节次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                for (s in sections) {
                    Row(Modifier.fillMaxWidth().clickable { onEditSection(s) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
            SettingsMenuSection(
                title = "调课 / 调休",
                desc = "停课 / 按某天课表 / 单日自定义",
                icon = Icons.Filled.SwapHoriz,
                expanded = "overrides" in expandedSections,
                onToggle = { toggleSection("overrides") }
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("共 ${overrides.size} 条", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                    Row(Modifier.fillMaxWidth().clickable { onEditOverride(o) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
            SettingsMenuSection(
                title = "首页倒计时",
                desc = "最多 3 个",
                icon = Icons.Filled.Timer,
                expanded = "countdowns" in expandedSections,
                onToggle = { toggleSection("countdowns") }
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("共 ${countdowns.size}/3 个", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onAddCountdown, enabled = countdowns.size < 3) { Icon(Icons.Filled.Add, "添加倒计时") }
                }
                if (countdowns.isEmpty()) {
                    Text("尚未添加倒计时", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                for (cd in countdowns) {
                    val accent = runCatching { Color(android.graphics.Color.parseColor(cd.color)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth().clickable { onEditCountdown(cd) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
            SettingsMenuSection(
                title = "数据备份",
                desc = "导入 / 导出 / 重置",
                icon = Icons.Filled.Backup,
                expanded = "data" in expandedSections,
                onToggle = { toggleSection("data") }
            ) {
                Text("导出 / 导入与网页版 schedule-data.json 完全兼容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("schedule-data.json") }, Modifier.weight(1f)) {
                        Icon(Icons.Filled.Backup, null)
                        Text(" 导出")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, Modifier.weight(1f)) {
                        Icon(Icons.Filled.Restore, null)
                        Text(" 导入")
                    }
                }
                Button(
                    onClick = { showReset = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Text(" 重置全部数据")
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
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

/** ⑦ 设置分组卡片：图标 + 加粗主色标题 + 描述，点击展开/收起二级内容 */
@Composable
private fun SettingsMenuSection(
    title: String,
    desc: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
        }
    }
}
