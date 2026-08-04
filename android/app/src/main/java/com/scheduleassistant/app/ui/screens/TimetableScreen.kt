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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.ui.MainViewModel
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.coursesAt
import com.scheduleassistant.app.util.effectiveParity

@Composable
fun TimetableScreen(
    vm: MainViewModel,
    onAddCourse: (weekday: Int, sectionId: String) -> Unit,
    onEditCourse: (Course) -> Unit
) {
    val courses by vm.courses.collectAsState()
    val sections by vm.sections.collectAsState()

    var viewWeek by remember { mutableStateOf(vm.currentWeekIndex ?: 1) }
    val currentIdx = vm.currentWeekIndex
    val eff = effectiveParity(viewWeek, currentIdx)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        item {
            // 周次切换
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewWeek = (viewWeek - 1).coerceAtLeast(1) }) {
                    Icon(Icons.Filled.NavigateBefore, "上一周")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("第 $viewWeek 周", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (viewWeek % 2 == 1) "单周" else "双周",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewWeek += 1 }) {
                    Icon(Icons.Filled.NavigateNext, "下一周")
                }
            }
            if (currentIdx != null) {
                OutlinedButton(
                    onClick = { viewWeek = currentIdx },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) { Text("回到本周（第 $currentIdx 周）") }
            }
        }

        item {
            // 表头：节次 + 星期
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Box(Modifier.width(54.dp))
                WEEKDAY_NAMES.forEach { name ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        items(sections) { sec ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 节次标签
                Box(
                    Modifier.width(54.dp).padding(end = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(sec.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(sec.start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // 7 天
                (1..7).forEach { wd ->
                    val list = coursesAt(wd, eff, sec.id, courses)
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (list.isNotEmpty()) {
                                    val c = runCatching { Color(android.graphics.Color.parseColor(list.first().color)) }
                                        .getOrDefault(MaterialTheme.colorScheme.primary)
                                    Modifier.background(c.copy(alpha = 0.15f))
                                } else Modifier
                            )
                            .clickable {
                                if (list.isNotEmpty()) onEditCourse(list.first())
                                else onAddCourse(wd, sec.id)
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (list.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    list.first().name.ifBlank { "(课)" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = runCatching { Color(android.graphics.Color.parseColor(list.first().color)) }
                                        .getOrDefault(MaterialTheme.colorScheme.primary)
                                )
                                if (list.first().location.isNotBlank()) {
                                    Text(
                                        list.first().location,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (list.size > 1) {
                                    Text("+${list.size - 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Icon(
                                Icons.Filled.Add, null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.width(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Box(Modifier.fillMaxWidth().padding(8.dp)) }
    }
}
