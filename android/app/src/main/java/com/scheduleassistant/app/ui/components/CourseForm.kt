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

    // ④ 星期/节次由点击的单元格决定，用户无需再选择（保留 initial 传入值）
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var weekType by rememberSaveable { mutableStateOf(initial?.weekType ?: "every") }
    var cls by rememberSaveable { mutableStateOf(initial?.cls ?: "") }
    var color by rememberSaveable { mutableStateOf(initial?.color?.ifBlank { COURSE_COLORS[0] } ?: COURSE_COLORS[0]) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }

    val weekday = initial?.weekday ?: 1
    val sectionId = initial?.sectionId ?: sections.firstOrNull()?.id ?: ""
    val weekdayName = WEEKDAY_NAMES.getOrNull(weekday - 1) ?: ""
    val secInfo = sections.firstOrNull { it.id == sectionId }

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

            // ④ 星期 · 节次固定信息（标题下方展示，无需选择）
            Text(
                "$weekdayName · ${secInfo?.name ?: ""}（${secInfo?.start ?: ""} - ${secInfo?.end ?: ""}）",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            FormColumn(spacing = 8.dp) {
                // ④ 去掉"如：高等数学"占位提示
                LabeledTextField("课程名称", name, { name = it })
                FormSectionTitle("周次")
                ChipGroup(
                    listOf("every" to "每周", "odd" to "单周", "even" to "双周"),
                    weekType
                ) { weekType = it }
                // ④ 班级仅允许数字，课表上自动补"班"字
                LabeledTextField(
                    "班级（仅数字）",
                    cls,
                    { cls = it.filter { ch -> ch.isDigit() }.take(6) },
                    placeholder = "如：15"
                )
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
                                // ② 修复：新建课程时 initial.id 是 ""（非 null），
                                // 直接使用会导致多条课程共用空主键、后写覆盖先写（丢课程）。
                                id = initial?.id?.takeIf { it.isNotBlank() } ?: uid("c"),
                                name = name.trim(),
                                // ⑥ 不再编辑地点/教师：保存时保留原值（不丢失导入数据）
                                location = initial?.location ?: "",
                                teacher = initial?.teacher ?: "",
                                cls = cls.trim(),
                                weekday = weekday,
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
