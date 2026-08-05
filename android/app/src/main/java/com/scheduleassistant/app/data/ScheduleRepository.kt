package com.scheduleassistant.app.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.scheduleassistant.app.data.model.AppSettings
import com.scheduleassistant.app.data.model.Countdown
import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.data.model.Meta
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.OverrideCourse
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.util.isValidDate
import com.scheduleassistant.app.util.isValidTime

/** 课程/提醒/倒计时可选颜色，与网页版一致 */
val COURSE_COLORS = listOf(
    "#2563eb", "#0891b2", "#7c3aed", "#db2777",
    "#ea580c", "#16a34a", "#ca8a04", "#dc2626", "#475569"
)

fun defaultSections(): List<Section> {
    val data = listOf(
        Triple("第1节", "08:00", "08:45"),
        Triple("第2节", "08:55", "09:40"),
        Triple("第3节", "10:00", "10:45"),
        Triple("第4节", "10:55", "11:40"),
        Triple("第5节", "14:00", "14:45"),
        Triple("第6节", "14:55", "15:40"),
        Triple("第7节", "16:00", "16:45"),
        Triple("第8节", "19:00", "19:45"),
        Triple("第9节", "19:55", "20:40")
    )
    return data.mapIndexed { i, (name, start, end) ->
        Section(id = "s${i + 1}", name = name, start = start, end = end, position = i)
    }
}

fun defaultSettings(): AppSettings = AppSettings(
    defaultReminder = 10,
    enableSound = true,
    theme = "light",
    background = "solid",
    bgImage = ""
)

class ScheduleRepository(
    val dao: ScheduleDao,
    private val db: AppDatabase
) {

    // ---------- 流（供 UI 观察）----------
    val metaFlow: Flow<Meta?> = dao.getMeta()
    val settingsFlow: Flow<AppSettings?> = dao.getSettings()
    val sectionsFlow: Flow<List<Section>> = dao.getSections()
    val coursesFlow: Flow<List<Course>> = dao.getCourses()
    val overridesFlow: Flow<List<Override>> = dao.getOverrides()
    val overrideCoursesFlow: Flow<List<OverrideCourse>> = dao.getAllOverrideCourses()
    val eventsFlow: Flow<List<Event>> = dao.getEvents()
    val countdownsFlow: Flow<List<Countdown>> = dao.getCountdowns()

    /** 首次启动写入默认数据（meta/settings/节次），保证后续流始终有值 */
    suspend fun seedIfEmpty() {
        withContext(Dispatchers.IO) {
            if (dao.getMetaNow() == null) dao.upsertMeta(Meta())
            if (dao.getSettingsNow() == null) dao.upsertSettings(defaultSettings())
            if (dao.getSectionsNow().isEmpty()) defaultSections().forEach { dao.upsertSection(it) }
        }
    }

    // ---------- meta ----------
    suspend fun updateMeta(patch: Meta.() -> Meta) {
        // 修复：getMetaNow 是同步 DAO 调用，必须切 IO 线程（主线程会抛 Cannot access database on the main thread）
        withContext(Dispatchers.IO) {
            val cur = dao.getMetaNow() ?: Meta()
            dao.upsertMeta(cur.patch())
        }
    }

    // ---------- settings ----------
    suspend fun updateSettings(patch: AppSettings.() -> AppSettings) {
        // 修复：getSettingsNow 是同步 DAO 调用，必须切 IO 线程
        withContext(Dispatchers.IO) {
            val cur = dao.getSettingsNow() ?: defaultSettings()
            dao.upsertSettings(cur.patch())
        }
    }

    // ---------- sections ----------
    suspend fun upsertSection(s: Section) = dao.upsertSection(s)
    suspend fun deleteSection(id: String) = dao.deleteSection(id)

    // ---------- courses ----------
    suspend fun upsertCourse(c: Course) = dao.upsertCourse(c)
    suspend fun deleteCourse(c: Course) = dao.deleteCourse(c)

    // ---------- overrides ----------
    suspend fun upsertOverride(o: Override, courses: List<OverrideCourse> = emptyList()) {
        // 先删后插原子化，避免中途失败留下残缺数据
        db.withTransaction {
            dao.upsertOverride(o)
            dao.deleteOverrideCourses(o.id)
            courses.forEach { dao.upsertOverrideCourse(it) }
        }
    }

    suspend fun deleteOverride(o: Override) {
        dao.deleteOverrideCourses(o.id)
        dao.deleteOverride(o)
    }

    // ---------- events ----------
    suspend fun upsertEvent(e: Event) = dao.upsertEvent(e)
    suspend fun deleteEvent(e: Event) = dao.deleteEvent(e)

    // ---------- countdowns ----------
    suspend fun upsertCountdown(c: Countdown) = dao.upsertCountdown(c)
    suspend fun deleteCountdown(c: Countdown) = dao.deleteCountdown(c)

    // ---------- 重置 ----------
    suspend fun resetAll() {
        dao.clearMeta()
        dao.clearSettings()
        dao.clearSections()
        dao.clearCourses()
        dao.clearOverrides()
        dao.clearOverrideCourses()
        dao.clearEvents()
        dao.clearCountdowns()
        seedIfEmpty()
    }

    // ============ 导出 / 导入（与网页版 schedule-data.json 格式兼容）============
    // 修复：内部调用同步 DAO 方法（getXxxNow），必须挂起并在 IO 线程执行
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val settings = dao.getSettingsNow() ?: defaultSettings()
        val sections = dao.getSectionsNow()
        val overrides = dao.getOverridesNow()
        val overrideCourses = dao.getAllOverrideCoursesNow()

        val root = JSONObject()
        val meta = dao.getMetaNow() ?: Meta()
        root.put("meta", JSONObject().apply {
            put("semesterName", meta.semesterName)
            put("semesterStart", meta.semesterStart)
            put("userName", meta.userName)
        })

        val settingsObj = JSONObject().apply {
            put("defaultReminder", settings.defaultReminder)
            put("enableSound", settings.enableSound)
            put("theme", settings.theme)
            put("background", settings.background)
            put("bgImage", settings.bgImage)
            put("sections", JSONArray().apply {
                sections.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("start", s.start)
                        put("end", s.end)
                    })
                }
            })
        }
        root.put("settings", settingsObj)

        val courses = dao.getCoursesNow()
        root.put("courses", JSONArray().apply {
            courses.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("location", c.location)
                    put("teacher", c.teacher)
                    put("cls", c.cls)
                    put("weekday", c.weekday)
                    put("weekType", c.weekType)
                    put("sectionId", c.sectionId)
                    put("color", c.color)
                    put("note", c.note)
                })
            }
        })

        root.put("overrides", JSONArray().apply {
            overrides.forEach { o ->
                val obj = JSONObject().apply {
                    put("id", o.id)
                    put("date", o.date)
                    put("mode", o.mode)
                }
                if (o.mode == "copyWeekday") obj.put("copyWeekday", o.copyWeekday ?: 1)
                if (o.mode == "custom") {
                    obj.put("courses", JSONArray().apply {
                        overrideCourses.filter { it.overrideId == o.id }.forEach { c ->
                            put(JSONObject().apply {
                                put("name", c.name)
                                put("sectionId", c.sectionId)
                                put("color", c.color)
                                put("location", c.location)
                                put("teacher", c.teacher)
                                put("note", c.note)
                            })
                        }
                    })
                }
                put(obj)
            }
        })

        val events = dao.getEventsNow()
        root.put("events", JSONArray().apply {
            events.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("date", e.date)
                    put("allDay", e.allDay)
                    put("start", e.start)
                    put("end", e.end)
                    put("location", e.location)
                    put("color", e.color)
                    put("note", e.note)
                    if (e.reminder == null) put("reminder", JSONObject.NULL) else put("reminder", e.reminder)
                    put("type", e.type)
                })
            }
        })

        val countdowns = dao.getCountdownsNow()
        root.put("countdowns", JSONArray().apply {
            countdowns.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("target", c.target)
                    put("color", c.color)
                })
            }
        })

        root.toString(2)
    }

    /**
     * 导入 JSON（兼容网页版格式），覆盖全部数据。
     * 修复：整库替换包在 Room 事务（dao.replaceAll）中，失败自动回滚，不再出现半清空；
     * 导入时校验字段合法性（坏数据跳过不入库）；调课按日期去重（同日期保留后一条）。
     */
    suspend fun importJson(json: String) {
        // 修复：同步 DAO 调用必须在 IO 线程（含事务内的所有读写）
        withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val metaObj = root.optJSONObject("meta")
        val settingsObj = root.optJSONObject("settings")
        val sectionsArr = settingsObj?.optJSONArray("sections") ?: JSONArray()
        val coursesArr = root.optJSONArray("courses") ?: JSONArray()
        val overridesArr = root.optJSONArray("overrides") ?: JSONArray()
        val eventsArr = root.optJSONArray("events") ?: JSONArray()
        val countdownsArr = root.optJSONArray("countdowns") ?: JSONArray()

        // meta（无强校验）
        val meta = Meta(
            semesterName = metaObj?.optString("semesterName", "") ?: "",
            semesterStart = metaObj?.optString("semesterStart", "") ?: "",
            userName = metaObj?.optString("userName", "") ?: ""
        )

        // settings
        val settings = AppSettings(
            defaultReminder = (settingsObj?.optInt("defaultReminder", 10) ?: 10).coerceIn(0, 10080),
            enableSound = settingsObj?.optBoolean("enableSound", true) ?: true,
            theme = settingsObj?.optString("theme", "light")?.let { if (it in setOf("light", "dark")) it else "light" } ?: "light",
            background = settingsObj?.optString("background", "solid")?.let { if (it in setOf("solid", "image")) it else "solid" } ?: "solid",
            bgImage = settingsObj?.optString("bgImage", "") ?: ""
        )

        // sections（时间非法则跳过；全空回退默认节次）
        val sections = mutableListOf<Section>()
        for (i in 0 until sectionsArr.length()) {
            val s = sectionsArr.optJSONObject(i) ?: continue
            val start = s.optString("start", "")
            val end = s.optString("end", "")
            if (!isValidTime(start) || !isValidTime(end)) continue
            sections.add(
                Section(
                    id = s.optString("id", "s${i + 1}"),
                    name = s.optString("name", "").ifBlank { "第${i + 1}节" },
                    start = start,
                    end = end,
                    position = i
                )
            )
        }
        if (sections.isEmpty()) sections.addAll(defaultSections())

        // courses（weekday 越界 / weekType 非法则跳过）
        val courses = mutableListOf<Course>()
        for (i in 0 until coursesArr.length()) {
            val c = coursesArr.optJSONObject(i) ?: continue
            val weekday = c.optInt("weekday", 0)
            if (weekday !in 1..7) continue
            courses.add(
                Course(
                    id = c.optString("id", "c$i"),
                    name = c.optString("name", ""),
                    location = c.optString("location", ""),
                    teacher = c.optString("teacher", ""),
                    cls = c.optString("cls", ""),
                    weekday = weekday,
                    weekType = c.optString("weekType", "every").let { if (it in setOf("every", "odd", "even")) it else "every" },
                    sectionId = c.optString("sectionId", ""),
                    color = c.optString("color", COURSE_COLORS[0]),
                    note = c.optString("note", "")
                )
            )
        }

        // overrides + 自定义课程（按日期去重：同日期保留后一条；日期非法跳过）
        val overridesByDate = LinkedHashMap<String, Override>()
        val overrideCourses = mutableListOf<OverrideCourse>()
        for (i in 0 until overridesArr.length()) {
            val o = overridesArr.optJSONObject(i) ?: continue
            val date = o.optString("date", "")
            if (!isValidDate(date)) continue
            val mode = o.optString("mode", "cancel").let { if (it in setOf("cancel", "copyWeekday", "custom")) it else "cancel" }
            val id = o.optString("id", "o$i")
            val override = Override(
                id = id,
                date = date,
                mode = mode,
                copyWeekday = if (mode == "copyWeekday") (o.optInt("copyWeekday", 1).coerceIn(1, 7)) else null
            )
            // 同日期已有旧记录时，先清理其自定义课程再覆盖
            overridesByDate[date]?.let { prev -> overrideCourses.removeAll { it.overrideId == prev.id } }
            overridesByDate[date] = override

            if (mode == "custom") {
                val arr = o.optJSONArray("courses") ?: JSONArray()
                for (j in 0 until arr.length()) {
                    val c = arr.optJSONObject(j) ?: continue
                    overrideCourses.add(
                        OverrideCourse(
                            id = "oc_${id}_$j",
                            overrideId = id,
                            name = c.optString("name", ""),
                            sectionId = c.optString("sectionId", ""),
                            color = c.optString("color", COURSE_COLORS[0]),
                            location = c.optString("location", ""),
                            teacher = c.optString("teacher", ""),
                            note = c.optString("note", "")
                        )
                    )
                }
            }
        }

        // events（日期非法 / 时间格式非法则跳过）
        val events = mutableListOf<Event>()
        for (i in 0 until eventsArr.length()) {
            val e = eventsArr.optJSONObject(i) ?: continue
            val date = e.optString("date", "")
            if (!isValidDate(date)) continue
            val allDay = e.optBoolean("allDay", false)
            val start = e.optString("start", "")
            val end = e.optString("end", "")
            if (!allDay && !isValidTime(start)) continue
            if (!allDay && end.isNotBlank() && !isValidTime(end)) continue
            val rem = if (e.isNull("reminder")) null else e.optInt("reminder", 0).coerceIn(0, 10080)
            events.add(
                Event(
                    id = e.optString("id", "e$i"),
                    title = e.optString("title", ""),
                    date = date,
                    allDay = allDay,
                    start = if (allDay) "" else start,
                    end = if (allDay) "" else end,
                    location = e.optString("location", ""),
                    color = e.optString("color", COURSE_COLORS[4]),
                    note = e.optString("note", ""),
                    reminder = rem,
                    type = e.optString("type", "work").let { if (it in setOf("work", "meeting", "prepare", "duty", "other")) it else "work" }
                )
            )
        }

        // countdowns（target 非法则跳过；空格分隔归一化为 T）
        val countdowns = mutableListOf<Countdown>()
        for (i in 0 until countdownsArr.length()) {
            val c = countdownsArr.optJSONObject(i) ?: continue
            val target = c.optString("target", "").trim().replace(' ', 'T')
            if (target.length != 16 || target[10] != 'T') continue
            countdowns.add(
                Countdown(
                    id = c.optString("id", "cd$i"),
                    title = c.optString("title", ""),
                    target = target,
                    color = c.optString("color", COURSE_COLORS[4])
                )
            )
        }

        // 整库替换（单事务，失败自动回滚，避免半清空状态）
        db.withTransaction {
            dao.clearMeta(); dao.clearSettings(); dao.clearSections(); dao.clearCourses()
            dao.clearOverrides(); dao.clearOverrideCourses(); dao.clearEvents(); dao.clearCountdowns()
            dao.upsertMeta(meta)
            dao.upsertSettings(settings)
            sections.forEach { dao.upsertSection(it) }
            courses.forEach { dao.upsertCourse(it) }
            overridesByDate.values.forEach { dao.upsertOverride(it) }
            overrideCourses.forEach { dao.upsertOverrideCourse(it) }
            events.forEach { dao.upsertEvent(it) }
            countdowns.forEach { dao.upsertCountdown(it) }
        }
        }
    }
}
