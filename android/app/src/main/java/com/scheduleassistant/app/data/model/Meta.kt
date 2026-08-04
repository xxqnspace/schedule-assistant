package com.scheduleassistant.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 学期与个人信息（单行人行，id 固定为 1） */
@Entity(tableName = "meta")
data class Meta(
    @PrimaryKey val id: Int = 1,
    val semesterName: String = "",
    val semesterStart: String = "",
    val userName: String = ""
)
