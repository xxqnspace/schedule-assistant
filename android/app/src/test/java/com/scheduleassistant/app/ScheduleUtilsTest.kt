package com.scheduleassistant.app

import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.util.coursesAt
import com.scheduleassistant.app.util.effectiveParity
import com.scheduleassistant.app.util.getDayTimeline
import com.scheduleassistant.app.util.weekIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ScheduleUtils / DateUtils 纯逻辑单元测试（JVM，无需设备） */
class ScheduleUtilsTest {

    private val sections = listOf(
        Section("s1", "第1节", "08:00", "08:45", 0),
        Section("s2", "第2节", "08:55", "09:40", 1)
    )

    @Test
    fun `weekIndex 计算正确且非法输入返回 null`() {
        assertEquals(1, weekIndex("2026-02-16", "2026-02-16"))
        assertEquals(2, weekIndex("2026-02-23", "2026-02-16"))
        assertEquals(3, weekIndex("2026-03-02", "2026-02-16"))
        // 学期开始前
        assertNull(weekIndex("2026-02-15", "2026-02-16"))
        // 未设置学期
        assertNull(weekIndex("2026-02-16", ""))
        // 非法日期
        assertNull(weekIndex("bad-date", "2026-02-16"))
    }

    @Test
    fun `weekIndex 非周一开学按自然周到周日为一周`() {
        // 2026-02-19 为周四开学
        assertEquals(1, weekIndex("2026-02-19", "2026-02-19"))   // 开学当天 = 第 1 周
        assertEquals(1, weekIndex("2026-02-22", "2026-02-19"))   // 周日仍在第 1 周
        assertEquals(2, weekIndex("2026-02-23", "2026-02-19"))   // 下周一 = 第 2 周
        assertEquals(2, weekIndex("2026-03-01", "2026-02-19"))   // 下周日 = 第 2 周
        assertEquals(3, weekIndex("2026-03-02", "2026-02-19"))   // 再下周一 = 第 3 周
        assertNull(weekIndex("2026-02-18", "2026-02-19"))        // 开学前 = 放假
    }

    @Test
    fun `effectiveParity 按查看周次奇偶返回`() {
        assertEquals("odd", effectiveParity(1, 3))
        assertEquals("even", effectiveParity(2, 3))
        assertEquals("odd", effectiveParity(null, 5))
        assertEquals("every", effectiveParity(null, null))
    }

    @Test
    fun `coursesAt 按星期与单双周过滤`() {
        val courses = listOf(
            Course(id = "c1", name = "数学", location = "", teacher = "", cls = "", weekday = 1, weekType = "every", sectionId = "s1", color = "#2563eb", note = ""),
            Course(id = "c2", name = "语文", location = "", teacher = "", cls = "", weekday = 1, weekType = "odd", sectionId = "s1", color = "#0891b2", note = ""),
            Course(id = "c3", name = "英语", location = "", teacher = "", cls = "", weekday = 1, weekType = "even", sectionId = "s2", color = "#7c3aed", note = "")
        )
        assertEquals(2, coursesAt(1, "odd", "s1", courses).size)
        assertEquals(1, coursesAt(1, "even", "s1", courses).size)
        assertEquals(1, coursesAt(1, "even", "s2", courses).size)
        assertEquals(0, coursesAt(2, "every", "s1", courses).size)
    }

    @Test
    fun `getDayTimeline 停课调休取消当天课程`() {
        val courses = listOf(
            Course(id = "c1", name = "数学", location = "教三301", teacher = "", cls = "", weekday = 1, weekType = "every", sectionId = "s1", color = "#2563eb", note = "")
        )
        val overrides = listOf(Override(id = "o1", date = "2026-02-16", mode = "cancel"))
        val tl = getDayTimeline("2026-02-16", courses, overrides, emptyList(), emptyList(), sections, "2026-02-16", 10)
        assertTrue(tl.none { it.kind == "course" })
    }

    @Test
    fun `getDayTimeline 全天事件排在定时事件前`() {
        val events = listOf(
            Event(id = "e1", title = "全天活动", date = "2026-02-16", allDay = true, start = "", end = "", location = "", color = "#db2777", note = "", reminder = null, type = "work"),
            Event(id = "e2", title = "会议", date = "2026-02-16", allDay = false, start = "10:00", end = "11:00", location = "", color = "#0891b2", note = "", reminder = null, type = "meeting")
        )
        val tl = getDayTimeline("2026-02-16", emptyList(), emptyList(), emptyList(), events, sections, "2026-02-16", 10)
        assertEquals("e1", tl[0].refId)
        assertEquals("e2", tl[1].refId)
    }

    @Test
    fun `getDayTimeline 课程按节次顺序排列`() {
        val courses = listOf(
            Course(id = "c2", name = "第二节", location = "", teacher = "", cls = "", weekday = 1, weekType = "every", sectionId = "s2", color = "#16a34a", note = ""),
            Course(id = "c1", name = "第一节", location = "", teacher = "", cls = "", weekday = 1, weekType = "every", sectionId = "s1", color = "#2563eb", note = "")
        )
        val tl = getDayTimeline("2026-02-16", courses, emptyList(), emptyList(), emptyList(), sections, "2026-02-16", 10)
        assertEquals("c1", tl[0].refId)
        assertEquals("c2", tl[1].refId)
    }
}
