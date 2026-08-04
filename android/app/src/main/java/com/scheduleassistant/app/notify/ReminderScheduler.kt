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
import com.scheduleassistant.app.util.millisUntilNext
import com.scheduleassistant.app.util.nowMillis
import com.scheduleassistant.app.util.timeToDate

/**
 * 把未来 30 天的课程/日程注册为系统精确闹钟。
 * 即使 App 关闭 / 进程被杀，到点仍由系统发出通知；数据变更或开机后重新注册。
 *
 * 修复项：
 * - H1：Android 12+ 检查 canScheduleExactAlarms()，未授权时降级为 setAndAllowWhileIdle（非精确），不再静默跳过；
 * - M3：requestCode 取 |key.hashCode()|（key 已含 UUID，确定性且冲突概率可忽略）；
 * - M4：注册每日 03:00 的静默续期闹钟，保证 30 天窗口持续滚动；
 * - L11：通知 body 前缀具体时间。
 */
object ReminderScheduler {

    private const val PREFS = "reminder_state"
    private const val KEY_IDS = "ids"
    private const val SCHEDULE_DAYS = 30

    private const val RENEW_REQUEST_CODE = 987654

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

            // Android 12+：精确闹钟权限（SCHEDULE_EXACT_ALARM）可能未授予，降级为非精确
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

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
                    val body = buildList {
                        if (!it.start.isNullOrBlank()) add(it.start)
                        if (!it.location.isNullOrBlank()) add(it.location)
                        if (!it.sectionName.isNullOrBlank()) add(it.sectionName)
                    }.joinToString(" · ")
                    val key = "${it.kind}|${it.refId}|${dateStr}|${it.start}|${it.reminder}"
                    try {
                        val pi = buildPendingIntent(context, key, title, body, settings.enableSound)
                        if (exactAllowed) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                            } else {
                                am.setExact(AlarmManager.RTC_WAKEUP, at, pi)
                            }
                        } else {
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                        }
                        newKeys.add(key)
                    } catch (e: SecurityException) {
                        // 兜底：仍无权限时跳过该条（不影响其他提醒）
                    }
                }
            }

            // 3) 每日静默续期闹钟：凌晨 03:00 触发后重新 scheduleAll，滚动 30 天窗口
            try {
                am.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    now + millisUntilNext(3, 0),
                    AlarmManager.INTERVAL_DAY,
                    buildRenewPendingIntent(context)
                )
            } catch (_: Exception) {
                // 续期闹钟注册失败不影响本次提醒
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
        val id = key.hashCode() and Int.MAX_VALUE
        return PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 每日续期闹钟（固定 requestCode，重复注册即覆盖） */
    private fun buildRenewPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("renew", true)
        }
        return PendingIntent.getBroadcast(
            context, RENEW_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
