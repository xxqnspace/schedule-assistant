package com.scheduleassistant.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scheduleassistant.app.data.model.AppSettings
import com.scheduleassistant.app.data.model.Countdown
import com.scheduleassistant.app.data.model.Course
import com.scheduleassistant.app.data.model.Event
import com.scheduleassistant.app.data.model.Meta
import com.scheduleassistant.app.data.model.Override
import com.scheduleassistant.app.data.model.OverrideCourse
import com.scheduleassistant.app.data.model.Section
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    // ---------- meta ----------
    @Query("SELECT * FROM meta WHERE id = 1")
    fun getMeta(): Flow<Meta?>

    @Query("SELECT * FROM meta WHERE id = 1")
    fun getMetaNow(): Meta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(m: Meta)

    // ---------- settings ----------
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingsNow(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(s: AppSettings)

    // ---------- sections ----------
    @Query("SELECT * FROM sections ORDER BY position ASC")
    fun getSections(): Flow<List<Section>>

    @Query("SELECT * FROM sections ORDER BY position ASC")
    fun getSectionsNow(): List<Section>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(s: Section)

    @Update
    suspend fun updateSection(s: Section)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteSection(id: String)

    @Query("DELETE FROM sections")
    suspend fun clearSections()

    // ---------- courses ----------
    @Query("SELECT * FROM courses")
    fun getCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses")
    fun getCoursesNow(): List<Course>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourse(c: Course)

    @Delete
    suspend fun deleteCourse(c: Course)

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    // ---------- overrides ----------
    @Query("SELECT * FROM overrides")
    fun getOverrides(): Flow<List<Override>>

    @Query("SELECT * FROM overrides")
    fun getOverridesNow(): List<Override>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(o: Override)

    @Delete
    suspend fun deleteOverride(o: Override)

    @Query("DELETE FROM overrides")
    suspend fun clearOverrides()

    // ---------- override_courses ----------
    @Query("SELECT * FROM override_courses WHERE override_id = :overrideId")
    fun getOverrideCourses(overrideId: String): Flow<List<OverrideCourse>>

    @Query("SELECT * FROM override_courses WHERE override_id = :overrideId")
    fun getOverrideCoursesNow(overrideId: String): List<OverrideCourse>

    @Query("SELECT * FROM override_courses")
    fun getAllOverrideCourses(): Flow<List<OverrideCourse>>

    @Query("SELECT * FROM override_courses")
    fun getAllOverrideCoursesNow(): List<OverrideCourse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverrideCourse(c: OverrideCourse)

    @Query("DELETE FROM override_courses WHERE override_id = :overrideId")
    suspend fun deleteOverrideCourses(overrideId: String)

    @Query("DELETE FROM override_courses")
    suspend fun clearOverrideCourses()

    // ---------- events ----------
    @Query("SELECT * FROM events")
    fun getEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events")
    fun getEventsNow(): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(e: Event)

    @Delete
    suspend fun deleteEvent(e: Event)

    @Query("DELETE FROM events")
    suspend fun clearEvents()

    // ---------- countdowns ----------
    @Query("SELECT * FROM countdowns")
    fun getCountdowns(): Flow<List<Countdown>>

    @Query("SELECT * FROM countdowns")
    fun getCountdownsNow(): List<Countdown>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCountdown(c: Countdown)

    @Delete
    suspend fun deleteCountdown(c: Countdown)

    @Query("DELETE FROM countdowns")
    suspend fun clearCountdowns()

    // ---------- 批量重置 ----------
    @Query("DELETE FROM meta")
    suspend fun clearMeta()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    @Query("DELETE FROM countdowns")
    suspend fun clearCountdownsOnly()
}
