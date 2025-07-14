package com.wendy.face.model

import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.abs

/**
 * 三庭的分析结果
 * @param upperCourt 上庭比例
 * @param middleCourt 中庭比例
 * @param lowerCourt 下庭比例
 * @param description 综合描述
 */
data class ThreeCourtResult(
    val upperCourtRatio: Float,
    val middleCourtRatio: Float,
    val lowerCourtRatio: Float,
    val description: String,
    val lineYCoordinates: List<Float> // 用于绘制的4条水平线的Y坐标
)

/**
 * 五眼的分析结果
 * @param eyeWidths 五个眼宽的比例列表
 * @param description 综合描述
 */
data class FiveEyeResult(
    val eyeWidthRatios: List<Float>,
    val description: String,
    val lineXCoordinates: List<Float> // 用于绘制的6条垂直线的X坐标
)

/**
 * 综合三庭五眼的分析结果
 */
data class ThreeCourtFiveEyeResult(
    val threeCourt: ThreeCourtResult,
    val fiveEye: FiveEyeResult
)

/**
 * 定义三庭五眼分析所需的关键点索引和计算逻辑
 */
object ThreeCourtFiveEye {
    // 三庭关键点
    const val FOREHEAD_LINE = 10 // 发际线
    const val BROW_LINE = 9      // 眉骨线 (取两眉中心)
    const val NOSE_BASE_LINE = 2   // 鼻底线
    const val CHIN_TIP = 152     // 下巴尖

    // 五眼关键点
    const val LEFT_FACE_BORDER = 234
    const val LEFT_EYE_OUTER_CORNER = 130
    const val LEFT_EYE_INNER_CORNER = 133
    const val RIGHT_EYE_INNER_CORNER = 362
    const val RIGHT_EYE_OUTER_CORNER = 359
    const val RIGHT_FACE_BORDER = 454

    /**
     * 计算两点之间的垂直距离
     */
    fun verticalDistance(p1: FaceMeshPoint, p2: FaceMeshPoint): Float {
        return abs(p1.position.y - p2.position.y)
    }

    /**
     * 计算两点之间的水平距离
     */
    fun horizontalDistance(p1: FaceMeshPoint, p2: FaceMeshPoint): Float {
        return abs(p1.position.x - p2.position.x)
    }
}