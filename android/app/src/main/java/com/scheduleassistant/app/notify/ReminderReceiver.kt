package com.scheduleassistant.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 闹钟触发后弹出系统通知；依据设置决定是否播放提示音。
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

        NotificationHelper.show(context, title, body, key.hashCode() and Int.MAX_VALUE)
        if (sound) playBeep()
    }

    private fun playBeep() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 220)
            Handler(Looper.getMainLooper()).postDelayed({ tg.release() }, 400)
        } catch (_: Exception) {
            // 音频不可用时忽略
        }
    }
}
