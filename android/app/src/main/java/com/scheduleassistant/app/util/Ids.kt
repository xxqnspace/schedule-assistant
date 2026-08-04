package com.scheduleassistant.app.util

import java.util.UUID

/**
 * 生成唯一 ID（UUID 前缀，避免同毫秒内生成碰撞导致 REPLACE 覆盖）。
 * 修复：原实现 Math.random()*1296 仅 2 位 base36，同毫秒碰撞概率约 1/1296。
 */
fun uid(prefix: String): String =
    "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"
