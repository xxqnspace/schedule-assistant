package com.scheduleassistant.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.scheduleassistant.app.R
import com.scheduleassistant.app.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "schedule_reminder"
    private const val CHANNEL_NAME = "上课 / 日程提醒"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "课程与日程的提前提醒"
                    enableLights(true)
                    enableVibration(true)
                    // ② 通知声音走系统默认通知音（不指定 sound 即用系统默认）
                    // 关闭声音由调用方通过 show(..., sound=false) 时切换静音通道实现
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    /**
     * 显示通知（系统通知栏样式 + 系统默认通知音/振动）。
     * @param id 通知 ID：调用方应传闹钟 key 的 hashCode，同名课程不会互相覆盖
     * @param sound false 时切换到静音通道（不响铃不振动）
     * 修复：M1 增加点击跳转主页；L13 使用独立小图标。
     */
    fun show(context: Context, title: String, body: String, id: Int, sound: Boolean = true) {
        val effectiveId = if (sound) CHANNEL_ID else "$CHANNEL_ID-silent"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(effectiveId) == null) {
                val ch = NotificationChannel(
                    effectiveId,
                    if (sound) CHANNEL_NAME else "上课 / 日程提醒（静音）",
                    if (sound) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "课程与日程的提前提醒"
                    if (!sound) {
                        setSound(null, null)
                        enableVibration(false)
                    } else {
                        enableLights(true)
                        enableVibration(true)
                    }
                }
                mgr.createNotificationChannel(ch)
            }
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, effectiveId)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setPriority(if (sound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
