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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.COURSE_COLORS
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.util.uid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormSheet(
    initial: Event?,
    defaultReminder: Int,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: ((Event) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: nowDateStr()) }
    var allDay by rememberSaveable { mutableStateOf(initial?.allDay ?: false) }
    var start by rememberSaveable { mutableStateOf(initial?.start ?: "09:00") }
    var end by rememberSaveable { mutableStateOf(initial?.end ?: "10:00") }
    var location by rememberSaveable { mutableStateOf(initial?.location ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: "work") }
    var color by rememberSaveable {
        mutableStateOf(
            initial?.color?.ifBlank { COURSE_COLORS[4] } ?: COURSE_COLORS[4]
        )
    }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var reminder by rememberSaveable { mutableStateOf(initial?.reminder?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                if (initial == null) "添加日程" else "编辑日程",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // ④ 紧凑布局：间距 8dp，全天与提醒合并一行
            FormColumn(spacing = 8.dp) {
                LabeledTextField("标题", title, { title = it }, placeholder = "如：教研组会议")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("日期")
                        OutlinedClickField(date) { showDatePicker(context, date) { date = it } }
                    }
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("类型")
                        DropdownField(
                            "类型",
                            listOf(
                                "work" to "工作", "meeting" to "会议",
                                "prepare" to "备课", "duty" to "值班", "other" to "其他"
                            ),
                            type
                        ) { type = it }
                    }
                }

                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = allDay,
                            onCheckedChange = { allDay = it }
                        )
                        Text("全天", style = MaterialTheme.typography.bodyMedium)
                    }
                    LabeledTextField(
                        "提醒(分钟)",
                        reminder,
                        // 修复（M14）：仅数字且限长，避免超大值溢出导致提醒静默丢失
                        { reminder = it.filter { ch -> ch.isDigit() }.take(5) },
                        placeholder = "默认 $defaultReminder",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            FormSectionTitle("开始")
                            OutlinedClickField(start) { showTimePicker(context, start) { start = it } }
                        }
                        Column(Modifier.weight(1f)) {
                            FormSectionTitle("结束")
                            OutlinedClickField(end) { showTimePicker(context, end) { end = it } }
                        }
                    }
                }

                LabeledTextField("地点", location, { location = it }, placeholder = "可选")
                FormSectionTitle("颜色")
                ColorSwatchRow(color) { color = it }
                LabeledTextField("备注", note, { note = it }, singleLine = false)
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
                        if (title.isBlank()) return@Button
                        onSave(
                            Event(
                                id = initial?.id ?: uid("e"),
                                title = title.trim(),
                                date = date,
                                allDay = allDay,
                                start = if (allDay) "" else start,
                                end = if (allDay) "" else end,
                                location = location.trim(),
                                color = color,
                                note = note.trim(),
                                reminder = reminder.toIntOrNull()?.coerceIn(0, 10080),
                                type = type
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (initial == null) "添加" else "保存") }
            }
        }
    }
}
