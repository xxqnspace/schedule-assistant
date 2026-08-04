package com.scheduleassistant.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.COURSE_COLORS
import com.scheduleassistant.app.data.model.Countdown
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.util.uid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownFormSheet(
    initial: Countdown?,
    onDismiss: () -> Unit,
    onSave: (Countdown) -> Unit,
    onDelete: ((Countdown) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // 默认目标：今天 23:59
    val defaultTarget = "${nowDateStr()}T23:59"
    val (initDate, initTime) = if (initial?.target?.length == 16) {
        initial.target.slice(0..9) to initial.target.slice(11..15)
    } else defaultTarget.slice(0..9) to defaultTarget.slice(11..15)

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initDate) }
    var time by remember { mutableStateOf(initTime) }
    var color by remember {
        mutableStateOf(initial?.color?.ifBlank { COURSE_COLORS[4] } ?: COURSE_COLORS[4])
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
                if (initial == null) "添加倒计时" else "编辑倒计时",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            FormColumn {
                LabeledTextField("标题", title, { title = it }, placeholder = "如：期末考试")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("日期")
                        OutlinedClickField(date) { showDatePicker(context, date) { date = it } }
                    }
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("时间")
                        OutlinedClickField(time) { showTimePicker(context, time) { time = it } }
                    }
                }
                FormSectionTitle("颜色")
                ColorSwatchRow(color) { color = it }
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
                            Countdown(
                                id = initial?.id ?: uid("cd"),
                                title = title.trim(),
                                target = "${date}T$time",
                                color = color
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (initial == null) "添加" else "保存") }
            }
        }
    }
}

/** 点击触发的只读输入框（用于弹出系统日期/时间选择器） */
@Composable
private fun OutlinedClickField(value: String, onClick: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        trailingIcon = { Icon(Icons.Filled.EditCalendar, null) }
    )
}
