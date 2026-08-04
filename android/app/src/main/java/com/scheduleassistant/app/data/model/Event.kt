package com.scheduleassistant.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 工作日程（按具体日期）。
 * allDay: 是否全天（无具体时间）
 * reminder: 提前提醒分钟数，null 表示使用默认
 * type: work | meeting | prepare | duty | other
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val allDay: Boolean,
    val start: String,
    val end: String,
    val location: String = "",
    val color: String,
    val note: String,
    val reminder: Int?,
    val type: String
)
