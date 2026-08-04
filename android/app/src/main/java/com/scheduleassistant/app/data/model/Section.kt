package com.scheduleassistant.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 上课节次（按 position 排序） */
@Entity(tableName = "sections")
data class Section(
    @PrimaryKey val id: String,
    val name: String,
    val start: String,
    val end: String,
    val position: Int
)
