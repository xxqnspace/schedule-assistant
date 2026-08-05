package com.scheduleassistant.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.scheduleassistant.app.util.nowDateStr

private val EVENT_TYPE_LABELS = mapOf(
    "work" to "工作", "meeting" to "会议", "prepare" to "备课", "duty" to "值班", "other" to "其他"
)

@Composable
fun EventsScreen(
    vm: MainViewModel,
    onAddEvent: () -> Unit,
    onEditEvent: (Event) -> Unit
) {
    val events by vm.events.collectAsState()

    // ② 已过完的日程自动隐藏（只显示今天及以后）
    val today = nowDateStr()
    val visibleEvents = events.filter { it.date >= today }

    val grouped = visibleEvents
        .sortedWith(compareBy<Event>({ it.date }, { if (it.allDay) "00:00" else it.start.ifBlank { "99:99" } })
            .thenBy { it.title })
        .groupBy { it.date }

    if (visibleEvents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无日程（已过完的日程已自动隐藏）", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (date, list) ->
            item {
                val wd = getDayOfWeek(date)
                Text(
                    "$date  ${WEEKDAY_NAMES[wd - 1]}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            items(list) { e ->
                val accent = runCatching { Color(android.graphics.Color.parseColor(e.color)) }
                    .getOrDefault(MaterialTheme.colorScheme.primary)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onEditEvent(e) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(e.title.ifBlank { "(无标题)" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            val sub = buildList {
                                if (e.allDay) add("全天")
                                else if (e.start.isNotBlank() || e.end.isNotBlank()) {
                                    add(listOfNotNull(e.start.ifBlank { null }, e.end.ifBlank { null }).joinToString(" - "))
                                }
                                if (e.location.isNotBlank()) add("📍 ${e.location}")
                                EVENT_TYPE_LABELS[e.type]?.let { add(it) }
                            }
                            if (sub.isNotEmpty()) {
                                Text(sub.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (e.note.isNotBlank()) {
                                Text(e.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { Box(Modifier.fillMaxWidth().padding(8.dp)) }
    }
}
