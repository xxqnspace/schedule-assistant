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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.ui.MainViewModel
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.getDayOfWeek
import com.scheduleassistant.app.util.getDayTimeline
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.util.nowMillis
import com.scheduleassistant.app.util.parseDateTimeTarget
import kotlinx.coroutines.delay

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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (countdowns.isNotEmpty()) {
            item { Text("倒计时", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
            items(countdowns) { cd ->
                CountdownCard(title = cd.title, target = cd.target, color = cd.color, now = now)
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "今日安排 · ${dateStr} ${WEEKDAY_NAMES[wd - 1]}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (timeline.isEmpty()) {
            item {
                Surface(
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("今天没有安排，好好休息 🌿", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(timeline) { item ->
                val isEvent = item.kind == "event"
                val accent = runCatching { Color(android.graphics.Color.parseColor(item.color)) }
                    .getOrDefault(MaterialTheme.colorScheme.primary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            // 修复（M12）：firstOrNull 避免极端竞态下崩溃
                            if (isEvent) {
                                Modifier.clickable {
                                    events.firstOrNull { it.id == item.refId }?.let(onEditEvent)
                                }
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.width(6.dp).fillMaxHeight().background(accent)
                        )
                        Column(Modifier.padding(14.dp).fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.title.ifBlank { "(无标题)" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (item.allDay) "全天"
                                    else listOfNotNull(item.start, item.end).joinToString(" - "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.sectionName.isNotBlank()) {
                                Text(item.sectionName, style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            if (item.location.isNotBlank()) {
                                Text("📍 ${item.location}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (item.note.isNotBlank()) {
                                Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CountdownCard(title: String, target: String, color: String, now: Long) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    val targetDate = parseDateTimeTarget(target)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(accent))
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                Text(title.ifBlank { "(无标题)" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                // 修复（L5）：目标时间格式非法时明确提示，而非误显示"已结束"
                if (targetDate == null) {
                    Text("时间格式错误", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                } else {
                    val diff = targetDate.time - now
                    if (diff <= 0) {
                        Text("已结束", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val totalSec = diff / 1000
                        val days = totalSec / 86400
                        val hours = (totalSec % 86400) / 3600
                        val mins = (totalSec % 3600) / 60
                        val secs = totalSec % 60
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TimeUnit(days, "天", accent)
                            TimeUnit(hours, "时", accent)
                            TimeUnit(mins, "分", accent)
                            TimeUnit(secs, "秒", accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeUnit(value: Long, unit: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "%02d".format(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
