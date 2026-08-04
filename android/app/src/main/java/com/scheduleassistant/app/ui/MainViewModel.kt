package com.scheduleassistant.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch { repo.seedIfEmpty() }
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

    /** 数据变化后重新注册系统本地通知 */
    private suspend fun reschedule() {
        ReminderScheduler.scheduleAll(getApplication())
    }

    val currentWeekIndex: Int?
        get() = weekIndex(nowDateStr(), meta.value.semesterStart)

    // ---------- meta ----------
    fun saveMeta(semesterName: String, semesterStart: String, userName: String) {
        viewModelScope.launch {
            repo.updateMeta { copy(semesterName = semesterName, semesterStart = semesterStart, userName = userName) }
            reschedule()
        }
    }

    // ---------- settings ----------
    fun updateSettings(patch: AppSettings.() -> AppSettings) {
        viewModelScope.launch { repo.updateSettings(patch); reschedule() }
    }

    // ---------- sections ----------
    fun saveSection(section: Section) {
        viewModelScope.launch { repo.upsertSection(section) }
    }

    fun removeSection(id: String) {
        viewModelScope.launch { repo.deleteSection(id) }
    }

    // ---------- courses ----------
    fun saveCourse(c: Course) {
        viewModelScope.launch { repo.upsertCourse(c); reschedule() }
    }

    fun removeCourse(c: Course) {
        viewModelScope.launch { repo.deleteCourse(c); reschedule() }
    }

    // ---------- overrides ----------
    fun saveOverride(o: Override, courses: List<OverrideCourse>) {
        viewModelScope.launch { repo.upsertOverride(o, courses); reschedule() }
    }

    fun removeOverride(o: Override) {
        viewModelScope.launch { repo.deleteOverride(o); reschedule() }
    }

    // ---------- events ----------
    fun saveEvent(e: Event) {
        viewModelScope.launch { repo.upsertEvent(e); reschedule() }
    }

    fun removeEvent(e: Event) {
        viewModelScope.launch { repo.deleteEvent(e); reschedule() }
    }

    // ---------- countdowns ----------
    fun saveCountdown(c: Countdown) {
        viewModelScope.launch { repo.upsertCountdown(c) }
    }

    fun removeCountdown(c: Countdown) {
        viewModelScope.launch { repo.deleteCountdown(c) }
    }

    // ---------- 重置 / 导入导出 ----------
    fun resetAll() {
        viewModelScope.launch { repo.resetAll(); reschedule() }
    }

    suspend fun exportJson(): String = repo.exportJson()

    suspend fun importJson(json: String) {
        repo.importJson(json)
        reschedule()
    }
}
