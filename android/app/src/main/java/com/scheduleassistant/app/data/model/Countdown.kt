package com.scheduleassistant.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 首页倒计时（最多 3 个）。
 * target: datetime-local 格式 "yyyy-MM-dd'T'HH:mm"
 */
@Entity(tableName = "countdowns")
data class Countdown(
    @PrimaryKey val id: String,
    val title: String,
    val target: String,
    val color: String
)
