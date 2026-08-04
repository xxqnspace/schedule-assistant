package com.scheduleassistant.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.scheduleassistant.app.ScheduleApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.scheduleassistant.app.data.defaultSettings
import com.scheduleassistant.app.data.model.Meta
import com.scheduleassistant.app.util.getDayTimeline
import com.scheduleassistant.app.util.dateOffsetStr
import com.scheduleassistant.app.util.nowMillis
import com.scheduleassistant.app.util.timeToDate

/**
 * 把未来 30 天的课程/日程注册为系统精确闹钟。
 * 即使 App 关闭 / 进程被杀，到点仍由系统发出通知；数据变更或开机后重新注册。
 */
object ReminderScheduler {

    private const val PREFS = "reminder_state"
    private const val KEY_IDS = "ids"
    private const val SCHEDULE_DAYS = 30

    suspend fun scheduleAll(context: Context) {
        val app = context.applicationContext as? ScheduleApplication ?: return
        val dao = app.repository.dao

        withContext(Dispatchers.IO) {
        val meta: Meta = dao.getMetaNow() ?: Meta()
        val settings = dao.getSettingsNow() ?: defaultSettings()
        val sections = dao.getSectionsNow()
        val courses = dao.getCoursesNow()
        val overrides = dao.getOverridesNow()
        val overrideCourses = dao.getAllOverrideCoursesNow()
        val events = dao.getEventsNow()

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@withContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // 1) 取消上一批
        val oldKeys = prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        oldKeys.forEach { key -> am.cancel(buildPendingIntent(context, key, "", "")) }

        // 2) 重新注册未来 SCHEDULE_DAYS 天
        val newKeys = mutableSetOf<String>()
        val now = nowMillis()
        for (d in 0 until SCHEDULE_DAYS) {
            val dateStr = dateOffsetStr(d)
            val items = getDayTimeline(
                dateStr = dateStr,
                courses = courses,
                overrides = overrides,
                overrideCourses = overrideCourses,
                events = events,
                sections = sections,
                semesterStart = meta.semesterStart,
                defaultReminder = settings.defaultReminder
            )
            for (it in items) {
                if (it.allDay || it.start.isNullOrBlank()) continue
                val start = timeToDate(dateStr, it.start) ?: continue
                val at = start.time - it.reminder * 60_000L
                if (at <= now) continue
                val title = if (it.kind == "course") "上课：" + it.title else "日程：" + it.title
                val body = listOfNotNull(it.location.takeIf { l -> l.isNotBlank() }, it.sectionName.takeIf { s -> s.isNotBlank() })
                    .joinToString(" · ")
                val key = "${it.kind}|${it.refId}|${dateStr}|${it.start}|${it.reminder}"
                try {
                    val pi = buildPendingIntent(context, key, title, body, settings.enableSound)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, at, pi)
                    }
                    newKeys.add(key)
                } catch (e: SecurityException) {
                    // 缺少 SCHEDULE_EXACT_ALARM 权限时跳过（不影响其他逻辑）
                }
            }
        }

        prefs.edit().putStringSet(KEY_IDS, newKeys).apply()
        }
    }

    private fun buildPendingIntent(
        context: Context,
        key: String,
        title: String,
        body: String,
        sound: Boolean = true
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("key", key)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("sound", sound)
        }
        val id = key.hashCode()
        return PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
