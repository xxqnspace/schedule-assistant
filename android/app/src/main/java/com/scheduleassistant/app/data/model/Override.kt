package com.scheduleassistant.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日期例外（调课 / 调休 / 单日自定义）。
 * mode: cancel（当天停课）| copyWeekday（按某天课表）| custom（单日自定义课表）
 * copyWeekday: 仅 copyWeekday 模式使用，1..7
 * custom 模式的课程存于 override_courses 表
 */
@Entity(tableName = "overrides")
data class Override(
    @PrimaryKey val id: String,
    val date: String,
    val mode: String,
    val copyWeekday: Int? = null
)
