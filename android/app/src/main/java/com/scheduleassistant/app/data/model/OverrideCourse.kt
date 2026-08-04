package com.scheduleassistant.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 单日自定义课表里的课程（custom 模式下的 override） */
@Entity(tableName = "override_courses")
data class OverrideCourse(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "override_id") val overrideId: String,
    val name: String,
    val sectionId: String,
    val color: String,
    val location: String,
    val teacher: String,
    val note: String
)
