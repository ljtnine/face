package com.wendy.face.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import com.google.mlkit.vision.facemesh.FaceMesh
import com.wendy.face.analyzer.FaceAnalyzer
import com.wendy.face.analyzer.PalaceAnalysisResult
import com.wendy.face.llm.LLMService
import com.wendy.face.model.ThreeCourtFiveEyeResult
import com.wendy.face.model.TwelvePalacesData
import com.wendy.face.ui.components.AnalysisReportContent
import com.wendy.face.ui.components.BottomDrawer
import com.wendy.face.ui.components.BottomDrawerState
import com.wendy.face.ui.components.FaceOverlay
import com.wendy.face.ui.components.LayerControlPanel
import kotlinx.coroutines.launch

/**
 * 人脸分析结果展示界面
 * @param capturedBitmap 拍摄的照片
 * @param faceMeshes 检测到的人脸网格
 * @param analysisResults 分析结果
 * @param isBackCamera 是否使用后置摄像头
 * @param onBack 返回按钮的回调
 * @param onReanalyze 重新分析按钮的回调
 */
@Composable
fun FaceAnalysisScreen(
    capturedImageUri: Uri,
    capturedBitmap: Bitmap, // 仍需要bitmap用于获取尺寸信息
    faceMeshes: List<FaceMesh>,
    analysisResults: List<PalaceAnalysisResult>,
    isBackCamera: Boolean,
    onBack: () -> Unit,
    onReanalyze: (() -> Unit)? = null
) {
    var imageDisplaySize by remember { mutableStateOf(IntSize.Zero) }
    var imageDisplayOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val llmService = remember { LLMService(context) }
    var destinyText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showDestinyResult by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 图层控制状态
    var showPhoto by remember { mutableStateOf(true) }
    var showContours by remember { mutableStateOf(true) }
    var showAllKeypoints by remember { mutableStateOf(false) } // 默认不显示所有关键点
    var showThreeCourtFiveEye by remember { mutableStateOf(true) }
    var threeCourtFiveEyeResult by remember { mutableStateOf<ThreeCourtFiveEyeResult?>(null) }
    val faceAnalyzer = remember { FaceAnalyzer() }

    // 底部抽屉状态
    var drawerState by remember { mutableStateOf(BottomDrawerState.COLLAPSED) }

   LaunchedEffect(faceMeshes) {
       if (faceMeshes.isNotEmpty()) {
           threeCourtFiveEyeResult = faceAnalyzer.analyzeThreeCourtFiveEye(faceMeshes.first())
       }
   }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 将拍摄的照片作为背景（根据图层控制显示/隐藏）
        if (showPhoto) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Captured Photo",
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { layoutCoordinates ->
                        // 获取Image Composable的实际尺寸和位置
                        imageDisplaySize = layoutCoordinates.size
                        imageDisplayOffset = layoutCoordinates.localToRoot(androidx.compose.ui.geometry.Offset.Zero)
                    },
                contentScale = ContentScale.Crop
            )
        } else {
            // 当照片隐藏时，仍需要获取尺寸信息用于关键点定位
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .onGloballyPositioned { layoutCoordinates ->
                        imageDisplaySize = layoutCoordinates.size
                        imageDisplayOffset = layoutCoordinates.localToRoot(androidx.compose.ui.geometry.Offset.Zero)
                    }
            )
        }

        // 2. 在照片上叠加人脸关键点
        if (imageDisplaySize != IntSize.Zero && faceMeshes.isNotEmpty()) {
            val scaleAndOffset = calculateScaleAndOffset(
                imageWidth = capturedBitmap.width,
                imageHeight = capturedBitmap.height,
                viewWidth = imageDisplaySize.width.toFloat(),
                viewHeight = imageDisplaySize.height.toFloat()
            )

            FaceOverlay(
                faceMeshes = faceMeshes,
                scaleX = scaleAndOffset.scaleX,
                scaleY = scaleAndOffset.scaleY,
                offsetX = scaleAndOffset.offsetX,
                offsetY = scaleAndOffset.offsetY,
                isBackCamera = isBackCamera,
                isPreviewMode = false,
                show3DPoints = false,
                showAllKeypoints = showAllKeypoints,
                showContours = showContours,
                showPalaceMarkers = true, // 宫位标记始终显示
               showThreeCourtFiveEye = showThreeCourtFiveEye,
               threeCourtFiveEyeResult = threeCourtFiveEyeResult
            )
        }

        // 添加一个从下到上的渐变蒙层，让文字更清晰
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 600f // 从屏幕大约1/3处开始渐变
                    )
                )
        )

        // 顶部返回按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回相机",
                    tint = Color.White
                )
            }
        }

        // 右侧图层控制面板（从上边30%位置开始）
        LayerControlPanel(
            showPhoto = showPhoto,
            showAnalysisReport = drawerState != BottomDrawerState.HIDDEN, // 根据抽屉状态显示
            showContours = showContours,
            showAllKeypoints = showAllKeypoints,
            onPhotoToggle = { showPhoto = it },
            onAnalysisReportToggle = {
                // 控制抽屉的显示/隐藏
                drawerState = if (drawerState == BottomDrawerState.HIDDEN) {
                    BottomDrawerState.COLLAPSED
                } else {
                    BottomDrawerState.HIDDEN
                }
            },
            onContoursToggle = { showContours = it },
            onAllKeypointsToggle = { showAllKeypoints = it },
            showThreeCourtFiveEye = showThreeCourtFiveEye,
            onThreeCourtFiveEyeToggle = { showThreeCourtFiveEye = it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = LocalConfiguration.current.screenHeightDp.dp * 0.2f, end = 8.dp)
        )

        // 底部抽屉
        BottomDrawer(
            drawerState = drawerState,
            onStateChange = { newState ->
                drawerState = newState
            },
            modifier = Modifier.fillMaxSize()
        ) {
            AnalysisReportContent(
                analysisResults = analysisResults,
                threeCourtFiveEyeResult = threeCourtFiveEyeResult,
                destinyText = destinyText,
                isAnalyzing = isAnalyzing,
                showDestinyResult = showDestinyResult,
                onDestinyAnalysis = {
                    coroutineScope.launch {
                        isAnalyzing = true
                        showDestinyResult = true
                        try {
                            val prompt = buildString {
                                append("请根据以下面相分析结果，进行详细的命格推演：\n\n")
                                analysisResults.forEach { result ->
                                    append("${result.palaceName}：${result.description}\n")
                                }
                                append("\n请从以下几个方面进行分析：")
                                append("1. 性格特征与天赋")
                                append("2. 事业发展方向")
                                append("3. 财运与投资建议")
                                append("4. 感情婚姻状况")
                                append("5. 健康注意事项")
                                append("6. 人生重要转折期")
                                append("\n请用专业而易懂的语言，给出具体的建议和指导。")
                            }

                            destinyText = ""
                            llmService.getDestiny(
                                TwelvePalacesData(analysisResults.associate { it.palaceName to it.description }),
                                threeCourtFiveEyeResult,
                                ""
                            ).collect { chunk ->
                                destinyText += chunk
                            }
                        } catch (e: Exception) {
                            destinyText = "分析过程中出现错误，请稍后重试。错误信息：${e.message}"
                        } finally {
                            isAnalyzing = false
                        }
                    }
                },
                listState = listState
            )
        }
    }
}

private data class ScaleAndOffsetResult(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float
)

/**
 * 计算在ContentScale.Crop模式下的缩放比例和偏移量
 */
private fun calculateScaleAndOffset(
    imageWidth: Int,
    imageHeight: Int,
    viewWidth: Float,
    viewHeight: Float
): ScaleAndOffsetResult {
    val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
    val viewAspectRatio = viewWidth / viewHeight

    val scale: Float
    val offsetX: Float
    val offsetY: Float

    if (imageAspectRatio > viewAspectRatio) {
        // 图片比视图更宽，高度填满视图，宽度裁剪
        scale = viewHeight / imageHeight.toFloat()
        val scaledImageWidth = imageWidth * scale
        offsetX = (viewWidth - scaledImageWidth) / 2f
        offsetY = 0f
    } else {
        // 图片比视图更高，宽度填满视图，高度裁剪
        scale = viewWidth / imageWidth.toFloat()
        val scaledImageHeight = imageHeight * scale
        offsetX = 0f
        offsetY = (viewHeight - scaledImageHeight) / 2f
    }

    return ScaleAndOffsetResult(scale, scale, offsetX, offsetY)
}


// 所有分析报告相关组件已移动到AnalysisReportContent中
