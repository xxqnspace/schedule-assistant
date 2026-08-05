package com.scheduleassistant.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scheduleassistant.app.util.backgroundImageModel
import com.scheduleassistant.app.data.COURSE_COLORS
import com.scheduleassistant.app.data.model.Countdown
import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.ui.components.CountdownFormSheet
import com.scheduleassistant.app.ui.components.CourseFormSheet
import com.scheduleassistant.app.ui.components.EventFormSheet
import com.scheduleassistant.app.ui.components.OverrideFormSheet
import com.scheduleassistant.app.ui.components.SectionFormSheet
import com.scheduleassistant.app.ui.designsystem.theme.GlassMeshBackground
import com.scheduleassistant.app.ui.designsystem.theme.LocalGlassTokens
import com.scheduleassistant.app.ui.designsystem.theme.glassConvex
import com.scheduleassistant.app.ui.screens.EventsScreen
import com.scheduleassistant.app.ui.screens.SettingsScreen
import com.scheduleassistant.app.ui.screens.TimetableScreen
import com.scheduleassistant.app.ui.screens.TodayScreen
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.getDayOfWeek
import com.scheduleassistant.app.util.millisUntilNext
import com.scheduleassistant.app.util.nowDateStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime

/** ③ 数字等宽字体 */
private val MonoFont = FontFamily.Monospace

/** ④ 按小时返回问候语 */
private fun greetingByHour(hour: Int): String = when (hour) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

/** ④ 励志一言 API（hitokoto，失败返回空串则不显示） */
private suspend fun fetchMotto(): String = withContext(Dispatchers.IO) {
    runCatching {
        val conn = (URL("https://v1.hitokoto.cn/?c=d&encode=json").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("User-Agent", "schedule-assistant")
        }
        try {
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(body)
                obj.optString("hitokoto", "")
            } else ""
        } finally {
            conn.disconnect()
        }
    }.getOrDefault("")
}

/** 当前打开的表单（底部弹窗） */
sealed interface OpenForm {
    data object None : OpenForm
    data class Course(val initial: com.scheduleassistant.app.data.model.Course?) : OpenForm
    data class Event(val initial: com.scheduleassistant.app.data.model.Event?) : OpenForm
    data class Override(val date: String, val initial: com.scheduleassistant.app.data.model.Override?) : OpenForm
    data class Section(val initial: com.scheduleassistant.app.data.model.Section?) : OpenForm
    data class Countdown(val initial: com.scheduleassistant.app.data.model.Countdown?) : OpenForm
}

@Composable
fun MainScreen(vm: MainViewModel) {
    var route by rememberSaveable { mutableStateOf("today") }
    var form by remember { mutableStateOf<OpenForm>(OpenForm.None) }
    val meta by vm.meta.collectAsState()
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current

    // 修复（M11）：跨午夜自动刷新顶栏日期与周次
    var dateStr by remember { mutableStateOf(nowDateStr()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNext(0, 1))
            dateStr = nowDateStr()
        }
    }

    // ④ 顶栏问候语 + 励志一言：每小时刷新
    var greeting by remember { mutableStateOf(greetingByHour(LocalTime.now().hour)) }
    var motto by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            greeting = greetingByHour(LocalTime.now().hour)
            motto = fetchMotto()
            delay(60 * 60 * 1000L)
        }
    }

    val wd = getDayOfWeek(dateStr)
    val idx = vm.currentWeekIndex
    val weekText = when {
        meta.semesterStart.isBlank() -> "未设置学期起始日"
        idx == null -> "放假中~"
        else -> "第 $idx 周 · ${if (idx % 2 == 1) "单周" else "双周"}"
    }

    // ⑧ 背景图 + 半透明遮罩，内容浮于其上（⑤ model 安全构造；限尺寸防大图 OOM 闪退）
    // 玻璃态：始终绘制 mesh 背景（无图时全屏显示；有图时作为兜底底层）
    val tokens = LocalGlassTokens.current
    Box(Modifier.fillMaxSize()) {
        GlassMeshBackground(tokens)
        val bgModel = backgroundImageModel(settings.bgImage)
        if (settings.background == "image" && bgModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(bgModel).size(1440).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    if (settings.theme == "dark") Color.Black.copy(alpha = 0.35f)
                    else Color.White.copy(alpha = 0.30f)
                )
            )
        }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // ③④ 玻璃顶栏：第一行 日期+周次；第二行 问候语(姓名) + 励志一言
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(tokens.tintConvex)
                    .then(
                        Modifier.drawBehind {
                            drawRect(color = tokens.borderHi, size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "${dateStr} ${WEEKDAY_NAMES[wd - 1]} · $weekText",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont,
                    color = tokens.textPrimary
                )
                // ④ 问候语 + 一言
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val name = meta.userName.trim()
                    Text(
                        if (name.isBlank()) "$greeting~" else "$greeting，$name 老师~",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = tokens.textSecondary
                    )
                    if (motto.isNotBlank()) {
                        Text(
                            "「$motto」",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // 玻璃底栏：凸起玻璃面板 + 高光
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(tokens.tintConvex)
                    .drawBehind {
                        drawRect(color = tokens.borderHi, size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
                    }
            ) {
                NavigationBar(containerColor = Color.Transparent) {
                    val items = listOf(
                        "today" to Pair("今日", Icons.Filled.CalendarToday),
                        "timetable" to Pair("课表", Icons.Filled.CalendarViewWeek),
                        "events" to Pair("日程", Icons.Filled.ListAlt),
                        "settings" to Pair("设置", Icons.Filled.Settings)
                    )
                    items.forEach { (r, pair) ->
                        NavigationBarItem(
                            selected = route == r,
                            onClick = { route = r },
                            icon = { Icon(pair.second, contentDescription = pair.first) },
                            label = { Text(pair.first) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = tokens.textPrimary,
                                selectedTextColor = tokens.textPrimary,
                                indicatorColor = tokens.tintConcave,
                                unselectedIconColor = tokens.textTertiary,
                                unselectedTextColor = tokens.textTertiary
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // ③ 玻璃圆形 FAB：无方框感，点击添加日程（课表页用编辑模式添加课程）
            if (route == "today" || route == "events") {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .glassConvex(cornerRadius = 28.dp, tokens = tokens)
                        .clip(CircleShape)
                        .clickable { form = OpenForm.Event(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add, "添加日程",
                        tint = tokens.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (route) {
                    "today" -> TodayScreen(
                        vm,
                        onAddEvent = { form = OpenForm.Event(null) },
                        onEditEvent = { form = OpenForm.Event(it) }
                    )
                    "timetable" -> TimetableScreen(
                        vm,
                        onAddCourse = { wd, sid ->
                            form = OpenForm.Course(
                                Course(
                                    id = "", name = "", location = "", teacher = "", cls = "",
                                    weekday = wd, weekType = "every", sectionId = sid,
                                    color = COURSE_COLORS[0], note = ""
                                )
                            )
                        },
                        onEditCourse = { form = OpenForm.Course(it) }
                    )
                    "events" -> EventsScreen(
                        vm,
                        onAddEvent = { form = OpenForm.Event(null) },
                        onEditEvent = { form = OpenForm.Event(it) }
                    )
                    "settings" -> SettingsScreen(
                        vm,
                        onEditSection = { form = OpenForm.Section(it) },
                        onAddSection = { form = OpenForm.Section(null) },
                        onEditCountdown = { form = OpenForm.Countdown(it) },
                        onAddCountdown = { form = OpenForm.Countdown(null) },
                        onEditOverride = { form = OpenForm.Override(it.date, it) },
                        onAddOverride = { form = OpenForm.Override(nowDateStr(), null) }
                    )
                }
            }

            // ---- 表单底部弹窗 ----
            when (val f = form) {
                OpenForm.None -> {}
                is OpenForm.Course -> CourseFormSheet(
                    initial = f.initial,
                    sections = vm.sections.value,
                    onDismiss = { form = OpenForm.None },
                    onSave = { vm.saveCourse(it); form = OpenForm.None },
                    onDelete = { vm.removeCourse(it); form = OpenForm.None }
                )
                is OpenForm.Event -> EventFormSheet(
                    initial = f.initial,
                    defaultReminder = vm.settings.value.defaultReminder,
                    onDismiss = { form = OpenForm.None },
                    onSave = { vm.saveEvent(it); form = OpenForm.None },
                    onDelete = { vm.removeEvent(it); form = OpenForm.None }
                )
                is OpenForm.Section -> SectionFormSheet(
                    initial = f.initial,
                    nextPosition = vm.sections.value.size,
                    onDismiss = { form = OpenForm.None },
                    onSave = { vm.saveSection(it); form = OpenForm.None },
                    onDelete = { vm.removeSection(it.id); form = OpenForm.None }
                )
                is OpenForm.Countdown -> CountdownFormSheet(
                    initial = f.initial,
                    onDismiss = { form = OpenForm.None },
                    onSave = { vm.saveCountdown(it); form = OpenForm.None },
                    onDelete = { vm.removeCountdown(it); form = OpenForm.None }
                )
                is OpenForm.Override -> {
                    val initCourses = if (f.initial != null)
                        vm.overrideCourses.value.filter { it.overrideId == f.initial.id }
                    else emptyList()
                    OverrideFormSheet(
                        date = f.date,
                        initial = f.initial,
                        initialCourses = initCourses,
                        sections = vm.sections.value,
                        onDismiss = { form = OpenForm.None },
                        onSave = { o, cs -> vm.saveOverride(o, cs); form = OpenForm.None },
                        onDelete = { vm.removeOverride(it); form = OpenForm.None }
                    )
                }
            }
        }
    }
    }
}
