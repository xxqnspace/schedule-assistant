package com.scheduleassistant.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.data.COURSE_COLORS
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.util.WEEKDAY_NAMES
import com.scheduleassistant.app.util.uid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFormSheet(
    initial: Course?,
    sections: List<Section>,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: ((Course) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var weekday by rememberSaveable { mutableStateOf((initial?.weekday ?: 1).toString()) }
    var weekType by rememberSaveable { mutableStateOf(initial?.weekType ?: "every") }
    var sectionId by rememberSaveable {
        mutableStateOf(
            initial?.sectionId ?: sections.firstOrNull()?.id ?: ""
        )
    }
    var location by rememberSaveable { mutableStateOf(initial?.location ?: "") }
    // 修复（H3）：教师字段保留原值，不再硬编码清空
    var teacher by rememberSaveable { mutableStateOf(initial?.teacher ?: "") }
    var cls by rememberSaveable { mutableStateOf(initial?.cls ?: "") }
    var color by rememberSaveable { mutableStateOf(initial?.color?.ifBlank { COURSE_COLORS[0] } ?: COURSE_COLORS[0]) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                if (initial == null) "添加课程" else "编辑课程",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            FormColumn {
                LabeledTextField("课程名称", name, { name = it }, placeholder = "如：高等数学")
                FormSectionTitle("上课星期")
                ChipGroup(
                    WEEKDAY_NAMES.mapIndexed { i, n -> (i + 1).toString() to n },
                    weekday
                ) { weekday = it }
                FormSectionTitle("周次")
                ChipGroup(
                    listOf("every" to "每周", "odd" to "单周", "even" to "双周"),
                    weekType
                ) { weekType = it }
                FormSectionTitle("节次")
                DropdownField(
                    "节次",
                    sections.map { it.id to "${it.name} (${it.start})" },
                    sectionId
                ) { sectionId = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        LabeledTextField("上课地点", location, { location = it }, placeholder = "如：教三 301")
                    }
                    Column(Modifier.weight(1f)) {
                        LabeledTextField("教师", teacher, { teacher = it }, placeholder = "可选")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        LabeledTextField("班级", cls, { cls = it }, placeholder = "1-15")
                    }
                }
                FormSectionTitle("颜色")
                ColorSwatchRow(color) { color = it }
                LabeledTextField("备注", note, { note = it }, singleLine = false)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initial != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initial) },
                        modifier = Modifier.weight(1f)
                    ) { Text("删除") }
                }
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        onSave(
                            Course(
                                id = initial?.id ?: uid("c"),
                                name = name.trim(),
                                location = location.trim(),
                                teacher = teacher.trim(),
                                cls = cls.trim(),
                                weekday = weekday.toIntOrNull() ?: 1,
                                weekType = weekType,
                                sectionId = sectionId,
                                color = color,
                                note = note.trim()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (initial == null) "添加" else "保存") }
            }
        }
    }
}
