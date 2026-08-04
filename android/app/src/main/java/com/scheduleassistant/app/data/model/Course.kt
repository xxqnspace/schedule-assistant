package com.scheduleassistant.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 周课表课程（单双周循环）。
 * weekday: 1(周一)..7(周日)
 * weekType: every | odd | even
 */
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val teacher: String,
    val cls: String,
    val weekday: Int,
    val weekType: String,
    val sectionId: String,
    val color: String,
    val note: String
)
