package com.wendy.face.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.facemesh.FaceMesh
import com.wendy.face.camera.CameraManager

/**
 * 相机视图组件
 * 集成相机预览和控制功能
 */
@Composable
fun CameraView(
    isBackCamera: Boolean,
    onFacesDetected: (List<FaceMesh>, Int, Int) -> Unit,
    onCameraSwitch: () -> Unit,
    onImageCaptured: (Uri?, Bitmap?) -> Unit,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // 创建相机管理器
    val cameraManager = remember {
        CameraManager(context, lifecycleOwner)
    }

    // 当组件销毁时释放资源
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val preview = PreviewView(ctx).apply {
                    // 设置缩放模式为FILL_CENTER，与ContentScale.Crop对应
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewView = preview
                cameraManager.bindCamera(preview, isBackCamera, onFacesDetected)
                preview
            },
            update = { preview ->
                cameraManager.bindCamera(preview, isBackCamera, onFacesDetected)
            }
        )

        // 添加人脸检测引导框
        FaceDetectionGuide()

        // 相机控制面板
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CameraControls(
                onCameraSwitch = onCameraSwitch,
                onCapture = {
                    cameraManager.takePicture(onImageCaptured)
                },
                onGalleryClick = onGalleryClick
            )
        }
    }
}
