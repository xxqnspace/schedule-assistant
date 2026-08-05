package com.scheduleassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.ui.MainViewModel
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.coursesAt
import com.scheduleassistant.app.util.effectiveParity
import com.scheduleassistant.app.util.getDayOfWeek
import com.scheduleassistant.app.util.nowDateStr

@Composable
fun TimetableScreen(
    vm: MainViewModel,
    onAddCourse: (weekday: Int, sectionId: String) -> Unit,
    onEditCourse: (Course) -> Unit
) {
    val courses by vm.courses.collectAsState()
    val sections by vm.sections.collectAsState()

    var viewWeek by rememberSaveable { mutableStateOf(vm.currentWeekIndex ?: 1) }
    val currentIdx = vm.currentWeekIndex
    val eff = effectiveParity(viewWeek, currentIdx)

    // ⑨ 编辑模式：开启时空格显示加号、可点击添加/编辑；关闭时纯展示不可点
    var editMode by rememberSaveable { mutableStateOf(false) }

    // ⑩ 当前星期高亮（表头 + 列底色）
    val todayWday = getDayOfWeek(nowDateStr())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        item {
            // ④ 紧凑单行：‹ 第N周·单双周 › + 编辑模式开关（点击周次标题可回本周）
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewWeek = (viewWeek - 1).coerceAtLeast(1) }) {
                    Icon(Icons.Filled.NavigateBefore, "上一周")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentIdx?.let { viewWeek = it } }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "第 $viewWeek 周",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (viewWeek % 2 == 1) "单周" else "双周",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewWeek += 1 }) {
                    Icon(Icons.Filled.NavigateNext, "下一周")
                }
                Box(Modifier.weight(1f))
                if (currentIdx != null && viewWeek != currentIdx) {
                    TextButton(onClick = { viewWeek = currentIdx }) {
                        Text("回本周", style = MaterialTheme.typography.labelMedium)
                    }
                }
                // ⑨ 编辑模式开关
                FilterChip(
                    selected = editMode,
                    onClick = { editMode = !editMode },
                    label = { Text(if (editMode) "编辑中" else "编辑模式") }
                )
            }
        }

        item {
            // 表头：节次 + 星期（当前星期主色高亮）
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Box(Modifier.width(50.dp))
                WEEKDAY_NAMES.forEachIndexed { i, name ->
                    val col = i + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (col == todayWday) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
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
        }

        items(sections) { sec ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 节次标签
                Box(
                    Modifier.width(50.dp).padding(end = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(sec.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(sec.start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // 7 天
                (1..7).forEach { wd ->
                    val list = coursesAt(wd, eff, sec.id, courses)
                    val isTodayCol = wd == todayWday
                    val courseColor = runCatching { Color(android.graphics.Color.parseColor(list.firstOrNull()?.color ?: "")) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                    val cellBg = when {
                        list.isNotEmpty() -> courseColor.copy(alpha = 0.16f)
                        isTodayCol -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .heightIn(min = 46.dp) // ③ 紧凑格子：两行 4 字即可，不拉长
                            .clip(RoundedCornerShape(6.dp))
                            .background(cellBg)
                            .then(
                                if (editMode) {
                                    Modifier.clickable {
                                        if (list.isNotEmpty()) onEditCourse(list.first())
                                        else onAddCourse(wd, sec.id)
                                    }
                                } else Modifier
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (list.isNotEmpty()) {
                            // ③ 每格只显示课程名（第 1 行）+ 班级（第 2 行）
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    list.first().name.ifBlank { "(课)" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = courseColor
                                )
                                Text(
                                    list.first().cls.ifBlank { "·" },
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (editMode) {
                            // ⑨ 编辑模式下空格显示加号
                            Icon(
                                Icons.Filled.Add, "添加课程",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
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
