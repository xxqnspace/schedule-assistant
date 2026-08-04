package com.scheduleassistant.app.util

/** 简单唯一 ID 生成（与网页版策略类似） */
fun uid(prefix: String): String {
    val r = (Math.random() * 1296).toInt().toString(36).padStart(2, '0')
    return "${prefix}_${System.currentTimeMillis().toString(36)}$r"
}
