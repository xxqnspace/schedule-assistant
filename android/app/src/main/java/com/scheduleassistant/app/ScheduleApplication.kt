package com.scheduleassistant.app

import android.app.Application
import com.scheduleassistant.app.data.AppDatabase
import com.scheduleassistant.app.data.ScheduleRepository

class ScheduleApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ScheduleRepository(database.scheduleDao()) }
}
