package com.scheduleassistant.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

/** 弹出系统日期选择器，返回 yyyy-MM-dd */
fun showDatePicker(context: Context, initial: String, onResult: (String) -> Unit) {
    val cal = Calendar.getInstance()
    initial.takeIf { it.length >= 10 }?.let {
        runCatching {
            val y = it.slice(0..3).toInt()
            val m = it.slice(5..6).toInt() - 1
            val d = it.slice(8..9).toInt()
            cal.set(y, m, d)
        }
    }
    DatePickerDialog(
        context,
        { _, y, m, d -> onResult("%04d-%02d-%02d".format(y, m + 1, d)) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

/** 弹出系统时间选择器，返回 HH:mm */
fun showTimePicker(context: Context, initial: String, onResult: (String) -> Unit) {
    val cal = Calendar.getInstance().apply {
        if (initial.length >= 5) {
            runCatching {
                set(Calendar.HOUR_OF_DAY, initial.slice(0..1).toInt())
                set(Calendar.MINUTE, initial.slice(3..4).toInt())
            }
        }
    }
    TimePickerDialog(
        context,
        { _, h, m -> onResult("%02d:%02d".format(h, m)) },
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
    ).show()
}
