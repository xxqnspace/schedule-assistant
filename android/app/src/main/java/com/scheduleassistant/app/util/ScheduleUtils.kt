package com.scheduleassistant.app.util

import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.OverrideCourse
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.data.COURSE_COLORS

/** 时间线中的一项（课程或工作日程） */
data class TimelineItem(
    val id: String,
    val refId: String,
    val title: String,
    val location: String,
    val start: String?,   // HH:mm；全天为 null
    val end: String?,
    val color: String,
    val note: String,
    val allDay: Boolean,
    val reminder: Int,
    val kind: String,     // course | event
    val type: String,     // 仅 event 使用
    val sectionName: String,
    val cls: String = ""  // 仅 course 使用：班级
)

private fun weekMatch(type: String, odd: Boolean): Boolean = when (type) {
    "every" -> true
    "odd" -> odd
    "even" -> !odd
    else -> true
}

private fun sectionOrder(sections: List<Section>, id: String): Int {
    val i = sections.indexOfFirst { it.id == id }
    return if (i < 0) 999 else i
}

/** 根据当前/查看周次，返回有效奇偶类型：every / odd / even */
fun effectiveParity(viewWeek: Int?, currentIdx: Int?): String {
    val idx = viewWeek ?: currentIdx
    if (idx == null) return "every"
    return if (idx % 2 == 1) "odd" else "even"
}

fun overrideOf(dateStr: String, overrides: List<Override>): Override? =
    overrides.firstOrNull { it.date == dateStr }

/**
 * 计算某天完整时间线：课程 + 工作日程，按时间排序。
 * 兼容日期例外（停课 / 调休 / 单日自定义）。
 */
fun getDayTimeline(
    dateStr: String,
    courses: List<Course>,
    overrides: List<Override>,
    overrideCourses: List<OverrideCourse>,
    events: List<Event>,
    sections: List<Section>,
    semesterStart: String,
    defaultReminder: Int
): List<TimelineItem> {
    val sm = sections.associateBy { it.id }
    val wd = getDayOfWeek(dateStr)
    val idx = weekIndex(dateStr, semesterStart)
    val odd = isOddWeek(idx)

    val ov = overrideOf(dateStr, overrides)
    val rawCourses: List<Course> = when {
        ov == null -> courses.filter { it.weekday == wd && weekMatch(it.weekType, odd) }
        ov.mode == "cancel" -> emptyList()
        ov.mode == "copyWeekday" ->
            courses.filter { it.weekday == (ov.copyWeekday ?: wd) && weekMatch(it.weekType, odd) }
        ov.mode == "custom" ->
            overrideCourses.filter { it.overrideId == ov.id }.map { oc ->
                Course(
                    id = oc.id, name = oc.name, location = oc.location, teacher = oc.teacher,
                    cls = "", weekday = wd, weekType = "every", sectionId = oc.sectionId,
                    color = oc.color, note = oc.note
                )
            }
        else -> emptyList()
    }

    val dayCourses = rawCourses.sortedBy { sectionOrder(sections, it.sectionId) }.mapNotNull { c ->
        val sec = sm[c.sectionId]
        TimelineItem(
            id = "c_" + c.id, refId = c.id, title = c.name,
            location = c.location, start = sec?.start, end = sec?.end,
            color = if (c.color.isBlank()) COURSE_COLORS[0] else c.color, note = c.note,
            allDay = false, reminder = defaultReminder, kind = "course",
            type = "", sectionName = sec?.name ?: "", cls = c.cls
        )
    }

    val dayEvents = events.filter { it.date == dateStr }.map { e ->
        TimelineItem(
            id = "e_" + e.id, refId = e.id, title = e.title,
            location = e.location,
            start = if (e.allDay) null else e.start.ifBlank { null },
            end = if (e.allDay) null else e.end.ifBlank { null },
            color = if (e.color.isBlank()) COURSE_COLORS[4] else e.color, note = e.note,
            allDay = e.allDay, reminder = e.reminder ?: defaultReminder,
            kind = "event", type = e.type, sectionName = ""
        )
    }

    return (dayCourses + dayEvents).sortedWith(compareBy<TimelineItem> { if (it.allDay) 0 else 1 }.thenBy { it.start ?: "" })
}

/** 课表网格用：取某星期、某奇偶类型下某节次的课程 */
fun coursesAt(weekday: Int, eff: String, sectionId: String, courses: List<Course>): List<Course> {
    val odd = eff == "odd"
    return courses.filter { it.weekday == weekday && it.sectionId == sectionId && weekMatch(it.weekType, odd) }
}
