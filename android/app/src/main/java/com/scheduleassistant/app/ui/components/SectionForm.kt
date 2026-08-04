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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.util.uid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionFormSheet(
    initial: Section?,
    nextPosition: Int,
    onDismiss: () -> Unit,
    onSave: (Section) -> Unit,
    onDelete: ((Section) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var start by remember { mutableStateOf(initial?.start ?: "08:00") }
    var end by remember { mutableStateOf(initial?.end ?: "08:45") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                if (initial == null) "添加节次" else "编辑节次",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            FormColumn {
                LabeledTextField("节次名称", name, { name = it }, placeholder = "如：第1节")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("开始时间")
                        OutlinedClickField(start) { showTimePicker(context, start) { start = it } }
                    }
                    Column(Modifier.weight(1f)) {
                        FormSectionTitle("结束时间")
                        OutlinedClickField(end) { showTimePicker(context, end) { end = it } }
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
                        if (name.isBlank()) return@Button
                        onSave(
                            Section(
                                id = initial?.id ?: uid("s"),
                                name = name.trim(),
                                start = start,
                                end = end,
                                position = initial?.position ?: nextPosition
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (initial == null) "添加" else "保存") }
            }
        }
    }
}
