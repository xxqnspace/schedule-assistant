package com.scheduleassistant.app.ui.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scheduleassistant.app.ui.designsystem.theme.LocalGlassTokens

/**
 * ⑤ 图片背景下的毛玻璃卡片底图层：
 * 卡片内叠加「模糊的背景图 + 半透遮罩」，实现真正的毛玻璃透出效果。
 * 用法：放在 glassConvex 卡片 Box 的最底层（内容之前）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassImageLayer(
    bgModel: Any?,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    if (bgModel == null) return
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    Box(modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(bgModel).size(640).build(),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(cornerRadius))
                .blur(18.dp),
            contentScale = ContentScale.Crop
        )
        // 半透遮罩：保证内容可读性，同时保留磨砂感
        Box(
            Modifier
                .matchParentSize()
                .background(tokens.tintConvex.copy(alpha = 0.55f))
        )
    }
}
