package com.scheduleassistant.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.scheduleassistant.app.notify.ReminderScheduler
import com.scheduleassistant.app.ui.theme.ScheduleTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 申请通知权限（用于上课/日程提醒）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            val dark = settings.theme == "dark"
            ScheduleTheme(darkTheme = dark) {
                MainScreen(vm)
            }
        }

        // 启动后注册系统本地提醒（等待数据库播种完成）
        CoroutineScope(Dispatchers.IO).launch {
            delay(600)
            ReminderScheduler.scheduleAll(this@MainActivity)
        }
    }
}
