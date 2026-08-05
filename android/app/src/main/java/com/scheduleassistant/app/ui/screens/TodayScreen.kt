package com.scheduleassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.ui.MainViewModel
import com.scheduleassistant.app.ui.designsystem.component.GlassImageLayer
import com.scheduleassistant.app.ui.designsystem.theme.LocalGlassTokens
import com.scheduleassistant.app.ui.designsystem.theme.glassConvex
import com.scheduleassistant.app.util.TimelineItem
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.backgroundImageModel
import com.scheduleassistant.app.util.getDayOfWeek
import com.scheduleassistant.app.util.getDayTimeline
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.util.nowMillis
import com.scheduleassistant.app.util.parseDateTimeTarget
import com.scheduleassistant.app.util.timeToDate
import kotlinx.coroutines.delay

/** ② 各项安排类型 -> 强调色（上课蓝 / 工作青 / 会议红 / 备课绿 / 值班橙 / 其他灰） */
private val EVENT_TYPE_COLORS = mapOf(
    "work" to "#0891b2",      // 工作：青
    "meeting" to "#dc2626",   // 会议：红
    "prepare" to "#16a34a",   // 备课：绿
    "duty" to "#ea580c",      // 值班：橙
    "other" to "#64748b"      // 其他：灰
)

/** 上课（课程）统一使用蓝色 */
private val COURSE_COLOR = "#2563eb"

/** ③ 数字等宽字体 */
private val MonoFont = FontFamily.Monospace

/** 事件类型中文名 */
private val EVENT_TYPE_LABELS = mapOf(
    "work" to "工作", "meeting" to "会议", "prepare" to "备课", "duty" to "值班", "other" to "其他"
)

@Composable
fun TodayScreen(
    vm: MainViewModel,
    onAddEvent: () -> Unit,
    onEditEvent: (Event) -> Unit
) {
    val countdowns by vm.countdowns.collectAsState()
    val courses by vm.courses.collectAsState()
    val overrides by vm.overrides.collectAsState()
    val overrideCourses by vm.overrideCourses.collectAsState()
    val events by vm.events.collectAsState()
    val sections by vm.sections.collectAsState()
    val meta by vm.meta.collectAsState()
    val settings by vm.settings.collectAsState()

    // 修复（M11）：跨午夜自动刷新日期与今日安排
    var dateStr by remember { mutableStateOf(nowDateStr()) }
    val wd = getDayOfWeek(dateStr)
    val timeline = remember(
        dateStr, courses, overrides, overrideCourses, events, sections, meta, settings
    ) {
        getDayTimeline(
            dateStr, courses, overrides, overrideCourses, events, sections,
            meta.semesterStart, settings.defaultReminder
        )
    }

    // 倒计时每秒刷新 + 日期跨天检测
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = nowMillis()
            val today = nowDateStr()
            if (today != dateStr) dateStr = today
            delay(1000)
        }
    }

    // ③ 已完成判断：非全天且结束时间已过
    fun itemDone(item: TimelineItem): Boolean {
        if (item.allDay) return false
        val endStr = item.end?.ifBlank { null } ?: item.start
        val end = timeToDate(dateStr, endStr) ?: return false
        return end.time < now
    }

    // ① 进行中判断：非全天、有明确结束时间，且 开始 ≤ now ≤ 结束
    fun itemInProgress(item: TimelineItem): Boolean {
        if (item.allDay) return false
        val startStr = item.start?.ifBlank { null } ?: return false
        val endStr = item.end?.ifBlank { null } ?: return false
        val start = timeToDate(dateStr, startStr) ?: return false
        val end = timeToDate(dateStr, endStr) ?: return false
        return start.time <= now && now <= end.time
    }

    // ③ 已完成项排到当日最后，其余保持时间顺序
    val ordered = remember(timeline, now) {
        val (active, done) = timeline.partition { !itemDone(it) }
        active + done
    }

    // ⑤ 图片背景时，卡片叠加模糊背景图做毛玻璃
    val isImageBg = settings.background == "image"
    val bgModel = remember(settings.bgImage) { backgroundImageModel(settings.bgImage) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ② 倒计时板块：去掉"倒计时"标题，直接展示卡片
        items(countdowns) { cd ->
            CountdownCard(
                title = cd.title, target = cd.target, color = cd.color, now = now,
                isImageBg = isImageBg, bgModel = bgModel
            )
        }

        item {
            // ④ 标题只保留"今日安排"（去掉日期星期）
            Text(
                "今日安排",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        if (ordered.isEmpty()) {
            item {
                val tokens = LocalGlassTokens.current
                Box(
                    Modifier.fillMaxWidth().glassConvex(cornerRadius = 14.dp, tokens = tokens)
                ) {
                    if (isImageBg) GlassImageLayer(bgModel, 14.dp)
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("今天没有安排，好好休息 🌿", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            // ① 第一个即将开始（未完成、未进行中、且未到开始时间）的日程，右侧显示"距开始"
            val firstUpcomingId = ordered.firstOrNull { item ->
                !itemDone(item) && !itemInProgress(item) &&
                    item.start?.let { timeToDate(dateStr, it)?.time?.let { t -> t > now } } == true
            }?.id

            items(ordered, key = { it.id }) { item ->
                val isEvent = item.kind == "event"
                val done = itemDone(item)
                val inProgress = itemInProgress(item)
                val isUpcoming = item.id == firstUpcomingId
                val typeLabel = if (isEvent) (EVENT_TYPE_LABELS[item.type] ?: item.type) else "上课"
                // ② 各项安排前不同颜色：上课蓝 / 工作青 / 会议红 / 备课绿 / 值班橙 / 其他灰
                val accent = if (isEvent) {
                    runCatching { Color(android.graphics.Color.parseColor(EVENT_TYPE_COLORS[item.type] ?: item.color)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                } else {
                    runCatching { Color(android.graphics.Color.parseColor(COURSE_COLOR)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                }
                // ① 卡片背景：玻璃凸面（毛玻璃）；图片背景时叠加模糊图；已完成整卡变灰
                val tokens = LocalGlassTokens.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassConvex(cornerRadius = 12.dp, tokens = tokens)
                        .then(
                            // 修复（M12）：firstOrNull 避免极端竞态下崩溃
                            if (isEvent) {
                                Modifier.clickable {
                                    events.firstOrNull { it.id == item.refId }?.let(onEditEvent)
                                }
                            } else Modifier
                        )
                ) {
                    // ⑤ 图片背景：毛玻璃底图（模糊背景 + 半透遮罩，内容之下）
                    if (isImageBg) {
                        GlassImageLayer(bgModel, 12.dp)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        // ② 最左：类型颜色标记
                        Box(
                            Modifier.width(8.dp).fillMaxHeight().background(if (done) accent.copy(alpha = 0.35f) else accent)
                        )
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ② 最左侧：类型文字（上课/工作/会议/备课/值班/其他），横排居中
                            Column(
                                Modifier.width(44.dp).padding(end = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    typeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                            // ② 日程时间：独立居中区域（所有日程都显示开始/结束时间）
                            Column(
                                Modifier.width(58.dp).padding(end = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (item.allDay) {
                                    Text(
                                        "全天",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        item.start ?: "--",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                        fontFamily = MonoFont,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        item.end ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = MonoFont,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // 内容：名称行 + 其他信息行（与名称对齐）
                            Column(Modifier.weight(1f)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.title.ifBlank { "(无标题)" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (done) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                "已完成",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (inProgress) {
                                        // ① 正在进行的日程：卡片最右端"进行中"标签（强调色）
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = accent.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "进行中",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isUpcoming) {
                                        // ② 最近一个日程：卡片最右端"xx 分钟后"（右对齐固定）
                                        val startDate = timeToDate(dateStr, item.start)
                                        val remainMin = startDate?.let { ((it.time - now) / 60_000L).coerceAtLeast(1) } ?: 1L
                                        Text(
                                            "$remainMin 分钟后",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = accent,
                                            fontFamily = MonoFont
                                        )
                                    }
                                }
                                // ② 其他信息行（与日程名称对齐，下一行）
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!isEvent && item.cls.isNotBlank()) {
                                        Text("${item.cls}班", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (item.sectionName.isNotBlank()) {
                                        Text(item.sectionName, style = MaterialTheme.typography.bodyMedium, color = accent)
                                    }
                                    if (item.location.isNotBlank()) {
                                        Text(
                                            "📍 ${item.location}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }
                                if (item.note.isNotBlank()) {
                                    Text(
                                        item.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    // ① 已完成：整卡灰色覆盖（置于内容之上，与未完成卡片明显区分）
                    if (done) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF9E9E9E).copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

/**
 * ② 倒计时卡片：单行式 "距离 {title} 还有 {N} 天"，剩余天数大字突出。
 * 不足 1 天显示小时；格式错误/已结束有明确提示。
 */
@Composable
private fun CountdownCard(
    title: String,
    target: String,
    color: String,
    now: Long,
    isImageBg: Boolean,
    bgModel: Any?
) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    val targetDate = parseDateTimeTarget(target)
    val name = title.ifBlank { "目标" }

    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier.fillMaxWidth().glassConvex(cornerRadius = 14.dp, tokens = tokens)
    ) {
        // ⑤ 图片背景：毛玻璃底图
        if (isImageBg) GlassImageLayer(bgModel, 14.dp)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(8.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(12.dp))
            if (targetDate == null) {
                Text("时间格式错误", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            } else {
                val diff = targetDate.time - now
                if (diff <= 0) {
                    Text("$name 已结束", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val days = diff / 86_400_000L
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("距离 ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(" 还有 ", style = MaterialTheme.typography.bodyLarge)
                        if (days > 0) {
                            // ① 剩余天数略微大于正文即可
                            Text(
                                "$days",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                fontFamily = MonoFont
                            )
                            Text(" 天", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            val hours = diff / 3_600_000L
                            Text(
                                "$hours",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                fontFamily = MonoFont
                            )
                            Text(" 小时", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
