package com.scheduleassistant.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.COURSE_COLORS
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.OverrideCourse
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.util.uid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverrideFormSheet(
    date: String,
    initial: Override?,
    initialCourses: List<OverrideCourse>,
    sections: List<Section>,
    onDismiss: () -> Unit,
    onSave: (Override, List<OverrideCourse>) -> Unit,
    onDelete: ((Override) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val overrideId = initial?.id ?: uid("o")

    var dateState by rememberSaveable { mutableStateOf(date.ifBlank { nowDateStr() }) }
    var mode by rememberSaveable { mutableStateOf(initial?.mode ?: "cancel") }
    var copyWeekday by rememberSaveable { mutableStateOf((initial?.copyWeekday ?: 1).toString()) }
    val customCourses = remember {
        mutableStateListOf<OverrideCourse>().apply { addAll(initialCourses) }
    }

    fun addCustomCourse() {
        customCourses.add(
            OverrideCourse(
                id = uid("oc"),
                overrideId = overrideId,
                name = "",
                sectionId = sections.firstOrNull()?.id ?: "",
                color = COURSE_COLORS[0],
                location = "",
                teacher = "",
                note = ""
            )
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                if (initial == null) "添加调课 / 调休" else "编辑调课 / 调休",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 0.dp)
            )

            FormColumn {
                FormSectionTitle("日期")
                OutlinedClickField(dateState) { showDatePicker(context, dateState) { dateState = it } }

                FormSectionTitle("类型")
                ChipGroup(
                    listOf(
                        "cancel" to "当天停课",
                        "copyWeekday" to "按某天课表",
                        "custom" to "单日自定义"
                    ),
                    mode
                ) { mode = it }

                if (mode == "copyWeekday") {
                    FormSectionTitle("复制星期")
                    ChipGroup(
                        WEEKDAY_NAMES.mapIndexed { i, n -> (i + 1).toString() to n },
                        copyWeekday
                    ) { copyWeekday = it }
                }

                if (mode == "custom") {
                    FormSectionTitle("当日课程（覆盖常规课表）")
                    if (customCourses.isEmpty()) {
                        Text("尚未添加课程", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    customCourses.forEachIndexed { idx, course ->
                        CardSurface {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("课程 ${idx + 1}", style = MaterialTheme.typography.labelMedium)
                                IconButton(
                                    onClick = { customCourses.removeAt(idx) },
                                    modifier = Modifier.padding(0.dp)
                                ) { Icon(Icons.Filled.Delete, "删除") }
                            }
                            LabeledTextField("课程名称", course.name, { v ->
                                customCourses[idx] = course.copy(name = v)
                            }, placeholder = "如：物理实验")
                            DropdownField(
                                "节次",
                                sections.map { it.id to "${it.name} (${it.start})" },
                                course.sectionId
                            ) { v -> customCourses[idx] = course.copy(sectionId = v) }
                            LabeledTextField("地点", course.location, { v ->
                                customCourses[idx] = course.copy(location = v)
                            }, placeholder = "可选")
                            FormSectionTitle("颜色")
                            ColorSwatchRow(course.color) { c -> customCourses[idx] = course.copy(color = c) }
                        }
                    }
                    OutlinedButton(onClick = { addCustomCourse() }, Modifier.fillMaxWidth()) {
                        Text("+ 添加课程")
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initial != null && onDelete != null) {
                    OutlinedButton(onClick = { onDelete(initial) }, Modifier.weight(1f)) { Text("删除") }
                }
                Button(
                    onClick = {
                        if (dateState.isBlank()) return@Button
                        onSave(
                            Override(
                                id = overrideId,
                                date = dateState,
                                mode = mode,
                                copyWeekday = if (mode == "copyWeekday") copyWeekday.toIntOrNull()?.coerceIn(1, 7) ?: 1 else null
                            ),
                            if (mode == "custom") customCourses.toList() else emptyList()
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (initial == null) "添加" else "保存") }
            }
        }
    }
}
