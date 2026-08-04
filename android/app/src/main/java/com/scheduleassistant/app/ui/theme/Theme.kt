package com.scheduleassistant.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF2563eb)
private val BrandDark = Color(0xFF3b82f6)

private val LightColors = lightColorScheme(
    primary = Brand,
    primaryContainer = Color(0xFFdbeafe),
    onPrimaryContainer = Color(0xFF1e3a8a)
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    primaryContainer = Color(0xFF1e3a8a),
    onPrimaryContainer = Color(0xFFdbeafe)
)

@Composable
fun ScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
