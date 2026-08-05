package com.scheduleassistant.app.ui.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 玻璃态根主题：提供 GlassTokens + MaterialTheme（颜色/排版映射到玻璃令牌）。
 * 背景 mesh 由页面层（MainScreen）自行绘制，本主题只做状态栏与主题映射。
 */
@Composable
fun GlassTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkGlassTokens else LightGlassTokens

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = tokens.meshBase.toArgb()
            window.navigationBarColor = tokens.meshBase.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalGlassTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                darkColorScheme(
                    primary = GlassAccent.primaryDark,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFF1e3a8a),
                    onPrimaryContainer = Color(0xFFdbeafe),
                    background = tokens.meshBase,
                    surface = tokens.meshBase,
                    surfaceVariant = tokens.tintConcave,
                    onBackground = tokens.textPrimary,
                    onSurface = tokens.textPrimary,
                    onSurfaceVariant = tokens.textSecondary,
                    outline = tokens.borderHi,
                    error = GlassAccent.error,
                )
            } else {
                lightColorScheme(
                    primary = GlassAccent.primary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFdbeafe),
                    onPrimaryContainer = Color(0xFF1e3a8a),
                    background = tokens.meshBase,
                    surface = tokens.meshBase,
                    surfaceVariant = tokens.tintConcave,
                    onBackground = tokens.textPrimary,
                    onSurface = tokens.textPrimary,
                    onSurfaceVariant = tokens.textSecondary,
                    outline = tokens.borderLo,
                    error = Color(0xFFdc2626),
                )
            },
            typography = GlassTypography,
            content = content
        )
    }
}
