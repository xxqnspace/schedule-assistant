package com.scheduleassistant.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

/** 当前日期字符串 yyyy-MM-dd */
fun nowDateStr(): String = dateFormat.format(Date())

/** Date -> yyyy-MM-dd */
fun toDateStr(d: Date): String = dateFormat.format(d)

/** yyyy-MM-dd -> Date（本地零时） */
fun parseDate(dateStr: String): Date? = runCatching { dateFormat.parse(dateStr) }.getOrNull()

/** 1=周一 ... 7=周日 */
fun getDayOfWeek(d: Date): Int {
    val w = Calendar.getInstance().apply { time = d }.get(Calendar.DAY_OF_WEEK)
    return if (w == Calendar.SUNDAY) 7 else w - 1
}

fun getDayOfWeek(dateStr: String): Int = parseDate(dateStr)?.let { getDayOfWeek(it) } ?: 1

/** 在 date 基础上加 n 天，返回 yyyy-MM-dd */
fun addDays(dateStr: String, n: Int): String {
    val d = parseDate(dateStr) ?: return dateStr
    val cal = Calendar.getInstance().apply { time = d; add(Calendar.DATE, n) }
    return toDateStr(cal.time)
}

/** 相对今天偏移 n 天，返回 yyyy-MM-dd */
fun dateOffsetStr(offsetDays: Int): String {
    val cal = Calendar.getInstance().apply { add(Calendar.DATE, offsetDays) }
    return toDateStr(cal.time)
}

/**
 * 基于学期起始日（应为某周一）计算 1-based 周次；未设置锚点返回 null。
 */
fun weekIndex(dateStr: String, semesterStart: String): Int? {
    if (semesterStart.isBlank()) return null
    val start = parseDate(semesterStart) ?: return null
    val cur = parseDate(dateStr) ?: return null
    val diff = ((cur.time - start.time) / 86400000).toInt()
    if (diff < 0) return null
    return diff / 7 + 1
}

fun isOddWeek(idx: Int?): Boolean = idx != null && idx % 2 == 1

/** 把 "HH:mm" 转成当天 Date；为空返回 null */
fun timeToDate(dateStr: String, hhmm: String?): Date? {
    if (hhmm.isNullOrBlank()) return null
    val (h, m) = hhmm.split(":").mapNotNull { it.toIntOrNull() }
    if (h == null || m == null) return null
    val base = parseDate(dateStr) ?: return null
    val cal = Calendar.getInstance().apply {
        time = base
        set(Calendar.HOUR_OF_DAY, h)
        set(Calendar.MINUTE, m)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.time
}

/** datetime-local 字符串 -> Date（兼容 "yyyy-MM-dd HH:mm" 与 "yyyy-MM-dd'T'HH:mm"） */
fun parseDateTimeTarget(target: String): Date? {
    if (target.isBlank()) return null
    val normalized = if (target.length == 16 && target[10] == ' ') target else target
    return runCatching { dateTimeFormat.parse(normalized) }.getOrNull()
}

fun nowMillis(): Long = System.currentTimeMillis()
