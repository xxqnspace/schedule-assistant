package com.scheduleassistant.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scheduleassistant.app.ui.theme.ScheduleTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 申请通知权限（用于上课/日程提醒）。
        // 修复（H2）：仅首次启动自动请求；被拒后不再反复弹窗，改由设置页引导开启。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = getSharedPreferences("perm_state", Context.MODE_PRIVATE)
            val asked = prefs.getBoolean("notif_asked", false)
            if (!asked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }
                prefs.edit().putBoolean("notif_asked", true).apply()
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
    }
}
