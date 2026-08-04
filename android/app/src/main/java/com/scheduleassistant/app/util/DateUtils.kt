package com.scheduleassistant.app.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** 日期格式 yyyy-MM-dd（ISO）与 datetime-local yyyy-MM-dd'T'HH:mm */
private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

/** 合法时间串校验，如 08:45 / 9:05 */
private val HHMM_REGEX = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")

/**
 * 日期/时间工具：基于 java.time（minSdk 26 原生支持）。
 * 修复：原 SimpleDateFormat 共享单例跨线程非安全（UI 与 IO 并发）；
 * weekIndex 毫秒整除 86400000 在 DST 切换日会算错；
 * timeToDate 对异常格式解构会抛 IndexOutOfBoundsException。
 */
fun nowDateStr(): String = LocalDate.now().toString()

/** Date -> yyyy-MM-dd */
fun toDateStr(d: Date): String = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()

/** yyyy-MM-dd -> Date（本地时区零点） */
fun parseDate(dateStr: String): Date? {
    val ld = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return null
    return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

/** 1=周一 ... 7=周日 */
fun getDayOfWeek(d: Date): Int {
    val ld = runCatching { LocalDate.parse(toDateStr(d)) }.getOrNull() ?: return 1
    return ld.dayOfWeek.value
}

fun getDayOfWeek(dateStr: String): Int =
    runCatching { LocalDate.parse(dateStr).dayOfWeek.value }.getOrDefault(1)

/** 在 date 基础上加 n 天，返回 yyyy-MM-dd */
fun addDays(dateStr: String, n: Int): String {
    val ld = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return dateStr
    return ld.plusDays(n.toLong()).toString()
}

/** 相对今天偏移 n 天，返回 yyyy-MM-dd */
fun dateOffsetStr(offsetDays: Int): String = LocalDate.now().plusDays(offsetDays.toLong()).toString()

/**
 * 基于学期起始日（应为某周一）计算 1-based 周次；未设置锚点或日期非法返回 null。
 * 修复：用 ChronoUnit.DAYS.between 按自然日计算，DST 边界不偏差。
 */
fun weekIndex(dateStr: String, semesterStart: String): Int? {
    if (semesterStart.isBlank()) return null
    val start = runCatching { LocalDate.parse(semesterStart) }.getOrNull() ?: return null
    val cur = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return null
    val diff = ChronoUnit.DAYS.between(start, cur)
    if (diff < 0) return null
    return (diff / 7).toInt() + 1
}

fun isOddWeek(idx: Int?): Boolean = idx != null && idx % 2 == 1

/** 把 "HH:mm" 转成当天 Date；为空或格式非法返回 null（不抛异常） */
fun timeToDate(dateStr: String, hhmm: String?): Date? {
    if (hhmm.isNullOrBlank()) return null
    val m = HHMM_REGEX.matchEntire(hhmm.trim()) ?: return null
    val h = m.groupValues[1].toInt()
    val min = m.groupValues[2].toInt()
    val ld = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return null
    return Date.from(ld.atTime(h, min).atZone(ZoneId.systemDefault()).toInstant())
}

/** 日期串是否合法 yyyy-MM-dd */
fun isValidDate(s: String): Boolean = runCatching { LocalDate.parse(s) }.isSuccess

/** 时间串是否合法 HH:mm */
fun isValidTime(s: String): Boolean = HHMM_REGEX.matches(s)

/** datetime-local 字符串 -> Date（兼容 "yyyy-MM-dd HH:mm" 与 "yyyy-MM-dd'T'HH:mm"） */
fun parseDateTimeTarget(target: String): Date? {
    if (target.isBlank()) return null
    val normalized = target.trim().replace(' ', 'T')
    val ldt = runCatching { LocalDateTime.parse(normalized, DATE_TIME_FORMATTER) }.getOrNull() ?: return null
    return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())
}

fun nowMillis(): Long = System.currentTimeMillis()

/** 距离下一次指定 hour:minute 的毫秒数（用于跨天刷新） */
fun millisUntilNext(hour: Int, minute: Int): Long {
    val now = LocalDateTime.now()
    var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next).toMillis() + 1000
}
