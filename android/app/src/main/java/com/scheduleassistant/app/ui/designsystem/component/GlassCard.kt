package com.scheduleassistant.app.ui.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scheduleassistant.app.ui.designsystem.theme.glassConvex

/** 玻璃卡片：凸起玻璃面 + 内容内边距 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    contentPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.glassConvex(cornerRadius = cornerRadius).padding(contentPadding),
        content = { content() }
    )
}
