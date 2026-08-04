package com.scheduleassistant.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 全局设置（单行人行，id 固定为 1）。
 * theme: light | dark
 * background: solid | image
 */
@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val defaultReminder: Int = 10,
    val enableSound: Boolean = true,
    val theme: String = "light",
    val background: String = "solid",
    val bgImage: String = ""
)
