package com.scheduleassistant.app.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.scheduleassistant.app.ScheduleApplication
import com.scheduleassistant.app.data.ScheduleRepository
import com.scheduleassistant.app.data.defaultSettings
import com.scheduleassistant.app.data.model.AppSettings
import com.scheduleassistant.app.data.model.Countdown
import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.data.model.Meta
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.OverrideCourse
import com.scheduleassistant.app.data.model.Section
import com.scheduleassistant.app.notify.ReminderScheduler
import com.scheduleassistant.app.util.nowDateStr
import com.scheduleassistant.app.util.weekIndex

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: ScheduleRepository = (application as ScheduleApplication).repository

    val meta: StateFlow<Meta>
    val settings: StateFlow<AppSettings>
    val sections: StateFlow<List<Section>>
    val courses: StateFlow<List<Course>>
    val overrides: StateFlow<List<Override>>
    val overrideCourses: StateFlow<List<OverrideCourse>>
    val events: StateFlow<List<Event>>
    val countdowns: StateFlow<List<Countdown>>

    init {
        // 修复（H4）：播种完成后才注册提醒，串行执行，消除 600ms 硬编码竞态
        viewModelScope.launch {
            runCatching {
                repo.seedIfEmpty()
                ReminderScheduler.scheduleAll(getApplication())
            }.onFailure { Log.e(TAG, "init scheduleAll 失败", it) }
        }
        val eager = SharingStarted.Eagerly
        meta = repo.metaFlow.map { it ?: Meta() }.stateIn(viewModelScope, eager, Meta())
        settings = repo.settingsFlow.map { it ?: defaultSettings() }
            .stateIn(viewModelScope, eager, defaultSettings())
        sections = repo.sectionsFlow.stateIn(viewModelScope, eager, emptyList())
        courses = repo.coursesFlow.stateIn(viewModelScope, eager, emptyList())
        overrides = repo.overridesFlow.stateIn(viewModelScope, eager, emptyList())
        overrideCourses = repo.overrideCoursesFlow.stateIn(viewModelScope, eager, emptyList())
        events = repo.eventsFlow.stateIn(viewModelScope, eager, emptyList())
        countdowns = repo.countdownsFlow.stateIn(viewModelScope, eager, emptyList())
    }

    /** 立即重注册（用于初始化 / 导入 / 重置等大变更）；异常不外抛避免崩溃 */
    private suspend fun reschedule() {
        runCatching { ReminderScheduler.scheduleAll(getApplication()) }
            .onFailure { Log.e(TAG, "reschedule 失败", it) }
    }

    /** 防抖重注册（M16）：连续小变更 400ms 内合并为一次全量重注册 */
    private var rescheduleJob: Job? = null
    private fun scheduleReschedule() {
        rescheduleJob?.cancel()
        rescheduleJob = viewModelScope.launch {
            delay(400)
            runCatching { ReminderScheduler.scheduleAll(getApplication()) }
                .onFailure { Log.e(TAG, "scheduleReschedule 失败", it) }
        }
    }

    val currentWeekIndex: Int?
        get() = weekIndex(nowDateStr(), meta.value.semesterStart)

    // ---------- meta ----------
    fun saveMeta(semesterName: String, semesterStart: String, userName: String) {
        viewModelScope.launch {
            runCatching {
                repo.updateMeta { copy(semesterName = semesterName, semesterStart = semesterStart, userName = userName) }
                reschedule()
            }.onFailure {
                Log.e(TAG, "saveMeta 失败", it)
                Toast.makeText(getApplication(), "保存失败：${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- settings ----------
    fun updateSettings(patch: AppSettings.() -> AppSettings) {
        viewModelScope.launch {
            runCatching {
                repo.updateSettings(patch)
                scheduleReschedule()
            }.onFailure {
                Log.e(TAG, "updateSettings 失败", it)
                Toast.makeText(getApplication(), "设置保存失败：${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- sections ----------
    fun saveSection(section: Section) {
        viewModelScope.launch {
            runCatching { repo.upsertSection(section) }
                .onFailure { Log.e(TAG, "saveSection 失败", it) }
        }
    }

    fun removeSection(id: String) {
        viewModelScope.launch {
            runCatching { repo.deleteSection(id) }
                .onFailure { Log.e(TAG, "removeSection 失败", it) }
        }
    }

    // ---------- courses ----------
    fun saveCourse(c: Course) {
        viewModelScope.launch {
            runCatching {
                repo.upsertCourse(c)
                scheduleReschedule()
            }.onFailure { Log.e(TAG, "saveCourse 失败", it) }
        }
    }

    fun removeCourse(c: Course) {
        viewModelScope.launch {
            runCatching {
                repo.deleteCourse(c)
                scheduleReschedule()
            }.onFailure { Log.e(TAG, "removeCourse 失败", it) }
        }
    }

    // ---------- overrides ----------
    fun saveOverride(o: Override, courses: List<OverrideCourse>) {
        viewModelScope.launch {
            runCatching {
                repo.upsertOverride(o, courses)
                scheduleReschedule()
            }.onFailure { Log.e(TAG, "saveOverride 失败", it) }
        }
    }

    fun removeOverride(o: Override) {
        viewModelScope.launch {
            runCatching {
                repo.deleteOverride(o)
                scheduleReschedule()
            }.onFailure { Log.e(TAG, "removeOverride 失败", it) }
        }
    }

    // ---------- events ----------
    fun saveEvent(e: Event) {
        viewModelScope.launch {
            runCatching {
                repo.upsertEvent(e)
                scheduleReschedule()
            }.onFailure { Log.e(TAG, "saveEvent 失败", it) }
        }
    }

    fun removeEvent(e: Event) {
        viewModelScope.launch {
            runCatching {
                repo.deleteEvent(e)
                scheduleReschedule()
            }.onFailure { Log.e(TAG, "removeEvent 失败", it) }
        }
    }

    // ---------- countdowns ----------
    fun saveCountdown(c: Countdown) {
        viewModelScope.launch {
            runCatching { repo.upsertCountdown(c) }
                .onFailure { Log.e(TAG, "saveCountdown 失败", it) }
        }
    }

    fun removeCountdown(c: Countdown) {
        viewModelScope.launch {
            runCatching { repo.deleteCountdown(c) }
                .onFailure { Log.e(TAG, "removeCountdown 失败", it) }
        }
    }

    // ---------- 重置 / 导入导出 ----------
    fun resetAll() {
        viewModelScope.launch {
            runCatching {
                repo.resetAll()
                reschedule()
            }.onFailure { Log.e(TAG, "resetAll 失败", it) }
        }
    }

    suspend fun exportJson(): String = repo.exportJson()

    suspend fun importJson(json: String) {
        runCatching {
            repo.importJson(json)
            reschedule()
        }.onFailure { Log.e(TAG, "importJson 失败", it) }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
