package com.scheduleassistant.app.util

import java.io.File

/**
 * ⑤ 背景图 Coil model 安全构造：
 * - URL（http/https）返回字符串；
 * - 本地文件返回 File（不存在返回 null，不渲染、不崩溃）；
 * - 空字符串返回 null。
 */
fun backgroundImageModel(bgImage: String): Any? = when {
    bgImage.isBlank() -> null
    bgImage.startsWith("http://") || bgImage.startsWith("https://") -> bgImage
    else -> File(bgImage).takeIf { it.exists() && it.isFile }
}

/** 背景图来源描述（设置页展示用） */
fun backgroundImageSource(bgImage: String): String = when {
    bgImage.isBlank() -> ""
    bgImage.startsWith("http://") || bgImage.startsWith("https://") -> "来源：URL 图片"
    else -> "来源：本地相册"
}
