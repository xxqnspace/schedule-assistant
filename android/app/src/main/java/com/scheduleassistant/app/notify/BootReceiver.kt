package com.scheduleassistant.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** 开机 / 应用更新后，重新注册本地提醒。修复：M8 增加 8 秒超时护栏，避免 goAsync 超时被杀。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeout(8000) {
                        ReminderScheduler.scheduleAll(context.applicationContext)
                    }
                } catch (_: Exception) {
                    // 超时或失败：不阻塞系统，下次开机/应用启动会再尝试
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
