package com.scheduleassistant.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/** 闹钟触发后弹出系统通知；依据设置决定是否播放提示音 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "日程提醒"
        val body = intent.getStringExtra("body") ?: ""
        val sound = intent.getBooleanExtra("sound", true)

        NotificationHelper.show(context, title, body)
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
