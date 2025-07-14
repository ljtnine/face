package com.wendy.face.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * 可镜像的异步图片组件
 * 用于处理前置摄像头拍照后的镜像显示问题
 */
@Composable
fun MirrorableAsyncImage(
    model: Uri,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    shouldMirror: Boolean = false
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = if (shouldMirror) {
                Modifier.scale(scaleX = -1f, scaleY = 1f) // 水平镜像
            } else {
                Modifier
            },
            contentScale = contentScale
        )
    }
}
