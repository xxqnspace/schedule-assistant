package com.scheduleassistant.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 闹钟触发后弹出系统通知（样式/声音/振动均走系统通知服务）。
 * 修复：M4 每日续期闹钟（renew=true）在此分支触发，静默重注册；
 * M2 通知 ID 使用 key.hashCode()，同名课程不再互相覆盖。
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 每日续期闹钟：不显示通知，仅重新注册未来 30 天提醒
        if (intent.getBooleanExtra("renew", false)) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ReminderScheduler.scheduleAll(context.applicationContext)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val key = intent.getStringExtra("key") ?: "r_${System.currentTimeMillis()}"
        val title = intent.getStringExtra("title") ?: "日程提醒"
        val body = intent.getStringExtra("body") ?: ""
        val sound = intent.getBooleanExtra("sound", true)

        // ② 通知样式/声音/振动全部交给系统通知服务（通知栏展示、系统默认通知音）
        NotificationHelper.show(context, title, body, key.hashCode() and Int.MAX_VALUE, sound)
    }
}
