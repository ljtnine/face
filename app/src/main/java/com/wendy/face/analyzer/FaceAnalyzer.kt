package com.wendy.face.analyzer

import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.wendy.face.model.FacePalaces
import com.wendy.face.model.FiveEyeResult
import com.wendy.face.model.ThreeCourtFiveEye
import com.wendy.face.model.ThreeCourtFiveEyeResult
import com.wendy.face.model.ThreeCourtResult
import com.wendy.face.model.TwelvePalaces
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 增强后的人脸分析结果的数据类
 * 使用Map来存储不同维度的量化特征
 */
data class PalaceAnalysisResult(
    val palaceName: String,
    val features: Map<String, Any>, // 使用Map存储多维度特征，如 "width" to 1.5, "fullness" to "high"
    val description: String // 基于所有特征生成的综合描述
)

/**
 * 人脸分析器
 * 负责对面部十二宫进行几何特征分析
 */
class FaceAnalyzer {

    /**
     * 分析单张人脸的十二宫
     * @param faceMesh ML Kit返回的人脸网格数据
     * @return 返回一个包含所有宫位分析结果的列表
     */
    fun analyze(faceMesh: FaceMesh): List<PalaceAnalysisResult> {
        val allPoints = faceMesh.allPoints
        
        return listOf(
            analyzeMingGong(allPoints),
            analyzeXiongDiGong(allPoints),
            analyzeFuQiGong(allPoints),
            analyzeZiNvGong(allPoints),
            analyzeCaiBoGong(allPoints),
            analyzeJiEGong(allPoints),
            analyzeQianYiGong(allPoints),
            analyzeNuPuGong(allPoints),
            analyzeGuanLuGong(allPoints),
            analyzeTianZhaiGong(allPoints),
            analyzeFuDeGong(allPoints),
            analyzeFuMuGong(allPoints)
        )
    }

    private fun analyzeMingGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("命宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("1. 命宫", emptyMap(), "未能定位命宫。")

        val features = mutableMapOf<String, Any>()
        var description = ""

        val leftEyebrowInner = allPoints.find { it.index == 55 }
        val rightEyebrowInner = allPoints.find { it.index == 285 }
        val leftCheek = allPoints.find { it.index == 234 }
        val rightCheek = allPoints.find { it.index == 454 }

        if (leftEyebrowInner != null && rightEyebrowInner != null && leftCheek != null && rightCheek != null) {
            val mingGongWidth = distance(leftEyebrowInner, rightEyebrowInner)
            val faceWidth = distance(leftCheek, rightCheek)
            val widthRatio = mingGongWidth / faceWidth
            features["widthRatio"] = widthRatio
            description += when {
                widthRatio > 0.15 -> "命宫宽阔，器量大。 "
                widthRatio < 0.10 -> "命宫狭窄，气量不大。 "
                else -> "命宫宽窄适中。 "
            }
        }

        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness
        description += when {
            fullness > 2.0 -> "命宫丰隆，易获成功。"
            fullness < -3.0 -> "命宫低陷，生活较艰苦。"
            else -> "命宫平满。"
        }

        return PalaceAnalysisResult("1. 命宫", features, description)
    }

    private fun analyzeXiongDiGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val leftPalacePoints = FacePalaces.getPalacePoints("兄弟宫", allPoints).getOrNull(0) ?: emptyList()
        val rightPalacePoints = FacePalaces.getPalacePoints("兄弟宫", allPoints).getOrNull(1) ?: emptyList()
        if (leftPalacePoints.isEmpty() || rightPalacePoints.isEmpty()) return PalaceAnalysisResult("2. 兄弟宫", emptyMap(), "未能定位兄弟宫。")

        val features = mutableMapOf<String, Any>()
        var description = ""

        val leftEyebrowOuter = allPoints.find { it.index == 133 }
        val leftEyebrowInner = allPoints.find { it.index == 55 }
        val leftEyeOuter = allPoints.find { it.index == 130 }
        
        if(leftEyebrowOuter != null && leftEyebrowInner != null && leftEyeOuter != null) {
            val eyebrowLength = distance(leftEyebrowInner, leftEyebrowOuter)
            val eyeLength = distance(allPoints.find { it.index == 33 }!!, leftEyeOuter)
            val lengthRatio = eyebrowLength / eyeLength
            features["lengthRatio"] = lengthRatio
            description += if (eyebrowLength > distance(leftEyebrowInner, leftEyeOuter)) "眉梢过眼，兄弟多。 " else "眉毛较短，兄弟可能不多。 "
        }

        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = (leftPalacePoints.map { it.position.z }.average() + rightPalacePoints.map { it.position.z }.average()) / 2
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness
        description += when {
            fullness > 1.5 -> "眉毛浓密，兄弟和睦。"
            fullness < -1.0 -> "眉毛稀疏，麻烦事多。"
            else -> "眉毛浓淡适中。"
        }
        
        return PalaceAnalysisResult("2. 兄弟宫", features, description)
    }

    private fun analyzeFuQiGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("夫妻宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("3. 夫妻宫", emptyMap(), "未能定位夫妻宫。")
        
        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness
        
        val description = when {
            fullness > 1.0 -> "夫妻宫饱满，婚姻幸福。"
            fullness < -2.0 -> "夫妻宫低陷，婚姻易生障碍。"
            else -> "夫妻宫平润，婚姻关系稳定。"
        }

        return PalaceAnalysisResult("3. 夫妻宫", features, description)
    }

    private fun analyzeZiNvGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("子女宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("4. 子女宫", emptyMap(), "未能定位子女宫。")

        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness

        val description = when {
            fullness > 1.5 -> "子女宫肌肉饱满，有卧蚕，子女成器有出息。"
            fullness < -2.0 -> "子女宫低陷，与子女缘分较薄。"
            else -> "子女宫平满，子女运势平稳。"
        }
        
        return PalaceAnalysisResult("4. 子女宫", features, description)
    }

    private fun analyzeCaiBoGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("财帛宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("5. 财帛宫", emptyMap(), "未能定位财帛宫。")

        val features = mutableMapOf<String, Any>()
        val noseTip = allPoints.find { it.index == 1 }!!
        val noseBridge = allPoints.find { it.index == 6 }!!
        val noseWidthPointLeft = allPoints.find { it.index == 48 }!!
        val noseWidthPointRight = allPoints.find { it.index == 278 }!!

        val noseHeight = abs(noseTip.position.z - noseBridge.position.z)
        val noseWidth = distance(noseWidthPointLeft, noseWidthPointRight)
        features["noseHeight"] = noseHeight
        features["noseWidth"] = noseWidth

        var description = ""
        description += if (noseHeight > 10) "鼻子高隆，事业有成。 " else "鼻梁不高，财运平平。 "
        description += if (noseWidth > 40) "鼻翼丰厚，财富聚集。 " else "鼻翼较薄，不易守财。 "

        return PalaceAnalysisResult("5. 财帛宫", features, description)
    }

    private fun analyzeJiEGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("疾厄宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("6. 疾厄宫", emptyMap(), "未能定位疾厄宫。")

        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness

        val description = when {
            fullness > 1.0 -> "山根饱满，抵抗力强。"
            fullness < -2.5 -> "山根断裂或低陷，需注意健康。"
            else -> "山根平满，健康状况良好。"
        }
        
        return PalaceAnalysisResult("6. 疾厄宫", features, description)
    }

    private fun analyzeQianYiGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("迁移宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("7. 迁移宫", emptyMap(), "未能定位迁移宫。")

        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness

        val description = when {
            fullness > 2.0 -> "迁移宫丰隆，出外有良好契机。"
            fullness < -2.0 -> "迁移宫低陷，不宜远行。"
            else -> "迁移宫平满，迁移运平稳。"
        }
        
        return PalaceAnalysisResult("7. 迁移宫", features, description)
    }

    private fun analyzeNuPuGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("奴仆宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("8. 奴仆宫", emptyMap(), "未能定位奴仆宫。")

        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness

        val description = when {
            fullness > 3.0 -> "下巴饱满，官运享通，晚景暮年运势佳。"
            fullness < -3.0 -> "两腮尖削无肉，晚运不佳。"
            else -> "下巴适中，人际关系平稳。"
        }
        
        return PalaceAnalysisResult("8. 奴仆宫", features, description)
    }

    private fun analyzeGuanLuGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("官禄宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("9. 官禄宫", emptyMap(), "未能定位官禄宫。")

        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness

        val description = when {
            fullness > 2.5 -> "官禄宫丰隆，事业展望极佳。"
            fullness < -2.5 -> "官禄宫塌陷，事业易遇挫折。"
            else -> "官禄宫平满，事业平稳发展。"
        }
        
        return PalaceAnalysisResult("9. 官禄宫", features, description)
    }

    private fun analyzeTianZhaiGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("田宅宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("10. 田宅宫", emptyMap(), "未能定位田宅宫。")

        val features = mutableMapOf<String, Any>()
        val eyeTop = allPoints.find { it.index == 159 }!!
        val eyebrowBottom = allPoints.find { it.index == 105 }!!
        val distance = abs(eyeTop.position.y - eyebrowBottom.position.y)
        features["distance"] = distance

        val description = when {
            distance > 10.0 -> "眼睑宽广，能得祖业。"
            distance < 5.0 -> "上眼睑狭窄，居住环境较差。"
            else -> "田宅宫距离适中，家运平稳。"
        }
        
        return PalaceAnalysisResult("10. 田宅宫", features, description)
    }

    private fun analyzeFuDeGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("福德宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("11. 福德宫", emptyMap(), "未能定位福德宫。")

        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness

        val description = when {
            fullness > 2.0 -> "福德宫饱满，福气很大。"
            fullness < -2.0 -> "福德宫尖削无肉，较为劳碌。"
            else -> "福德宫平满，福气平稳。"
        }
        
        return PalaceAnalysisResult("11. 福德宫", features, description)
    }

    private fun analyzeFuMuGong(allPoints: List<FaceMeshPoint>): PalaceAnalysisResult {
        val palacePoints = FacePalaces.getPalacePoints("父母宫", allPoints).flatten()
        if (palacePoints.isEmpty()) return PalaceAnalysisResult("12. 父母宫", emptyMap(), "未能定位父母宫。")

        val features = mutableMapOf<String, Any>()
        val averageZ = allPoints.map { it.position.z }.average()
        val palaceAverageZ = palacePoints.map { it.position.z }.average()
        val fullness = averageZ - palaceAverageZ
        features["fullness"] = fullness

        val description = when {
            fullness > 1.5 -> "日月角丰隆，父母健康长寿。"
            fullness < -1.5 -> "日月角低陷，与父母缘分较薄。"
            else -> "日月角平满，与父母关系平稳。"
        }
        
        return PalaceAnalysisResult("12. 父母宫", features, description)
    }

    private fun distance(p1: FaceMeshPoint, p2: FaceMeshPoint): Double {
        val dx = p1.position.x - p2.position.x
        val dy = p1.position.y - p2.position.y
        val dz = p1.position.z - p2.position.z
        return sqrt(dx.pow(2) + dy.pow(2) + dz.pow(2)).toDouble()
    }

    /**
     * 分析三庭五眼
     * @param faceMesh ML Kit返回的人脸网格数据
     * @return 返回三庭五眼的分析结果，如果关键点不足则返回null
     */
    fun analyzeThreeCourtFiveEye(faceMesh: FaceMesh): ThreeCourtFiveEyeResult? {
        val allPoints = faceMesh.allPoints
        if (allPoints.size < 468) return null

        // --- 关键点获取 ---
        val browLinePoint = allPoints.getOrNull(ThreeCourtFiveEye.BROW_LINE) ?: return null
        val noseBaseLinePoint = allPoints.getOrNull(ThreeCourtFiveEye.NOSE_BASE_LINE) ?: return null
        val chinTipPoint = allPoints.getOrNull(ThreeCourtFiveEye.CHIN_TIP) ?: return null
        val leftFaceBorderPoint = allPoints.getOrNull(ThreeCourtFiveEye.LEFT_FACE_BORDER) ?: return null
        val leftEyeOuterCornerPoint = allPoints.getOrNull(ThreeCourtFiveEye.LEFT_EYE_OUTER_CORNER) ?: return null
        val leftEyeInnerCornerPoint = allPoints.getOrNull(ThreeCourtFiveEye.LEFT_EYE_INNER_CORNER) ?: return null
        val rightEyeInnerCornerPoint = allPoints.getOrNull(ThreeCourtFiveEye.RIGHT_EYE_INNER_CORNER) ?: return null
        val rightEyeOuterCornerPoint = allPoints.getOrNull(ThreeCourtFiveEye.RIGHT_EYE_OUTER_CORNER) ?: return null
        val rightFaceBorderPoint = allPoints.getOrNull(ThreeCourtFiveEye.RIGHT_FACE_BORDER) ?: return null

        // --- 三庭分析 (优化策略) ---
        // 1. 以中庭为基准
        val middleCourtHeight = ThreeCourtFiveEye.verticalDistance(browLinePoint, noseBaseLinePoint)
        if (middleCourtHeight <= 0) return null

        // 2. 估算上庭和下庭高度
        val estimatedUpperCourtHeight = middleCourtHeight
        val lowerCourtHeight = ThreeCourtFiveEye.verticalDistance(noseBaseLinePoint, chinTipPoint)

        // 3. 计算虚拟发际线Y坐标
        val browY = browLinePoint.position.y
        val virtualForeheadY = browY - estimatedUpperCourtHeight

        val totalHeight = estimatedUpperCourtHeight + middleCourtHeight + lowerCourtHeight
        if (totalHeight <= 0f) return null

        val upperRatio = estimatedUpperCourtHeight / totalHeight
        val middleRatio = middleCourtHeight / totalHeight
        val lowerRatio = lowerCourtHeight / totalHeight

        val threeCourtDescription = buildString {
            append(if (abs(upperRatio - middleRatio) < 0.05 && abs(middleRatio - lowerRatio) < 0.05) "三庭均等，比例协调。 " else "三庭比例不均。 ")
            append("上庭占比${"%.1f".format(upperRatio * 100)}%, ")
            append("中庭占比${"%.1f".format(middleRatio * 100)}%, ")
            append("下庭占比${"%.1f".format(lowerRatio * 100)}%。")
        }
        
        val threeCourtLineYs = listOf(
            virtualForeheadY,
            browY,
            noseBaseLinePoint.position.y,
            chinTipPoint.position.y
        )
        val threeCourtResult = ThreeCourtResult(upperRatio, middleRatio, lowerRatio, threeCourtDescription, threeCourtLineYs)

        // --- 五眼分析 ---
        val eye1Width = ThreeCourtFiveEye.horizontalDistance(leftFaceBorderPoint, leftEyeOuterCornerPoint)
        val eye2Width = ThreeCourtFiveEye.horizontalDistance(leftEyeOuterCornerPoint, leftEyeInnerCornerPoint)
        val eye3Width = ThreeCourtFiveEye.horizontalDistance(leftEyeInnerCornerPoint, rightEyeInnerCornerPoint)
        val eye4Width = ThreeCourtFiveEye.horizontalDistance(rightEyeInnerCornerPoint, rightEyeOuterCornerPoint)
        val eye5Width = ThreeCourtFiveEye.horizontalDistance(rightEyeOuterCornerPoint, rightFaceBorderPoint)
        val totalWidth = eye1Width + eye2Width + eye3Width + eye4Width + eye5Width

        if (totalWidth == 0f) return null

        val eyeRatios = listOf(eye1Width, eye2Width, eye3Width, eye4Width, eye5Width).map { it / totalWidth }
        val fiveEyeDescription = buildString {
            append(if (abs(eye2Width - eye3Width) < eye2Width * 0.15) "眼间距适中。 " else "眼间距可能过宽或过窄。 ")
            append("面部宽度约等于五个眼长。")
        }
        
        val fiveEyeLineXs = listOf(
            leftFaceBorderPoint.position.x,
            leftEyeOuterCornerPoint.position.x,
            leftEyeInnerCornerPoint.position.x,
            rightEyeInnerCornerPoint.position.x,
            rightEyeOuterCornerPoint.position.x,
            rightFaceBorderPoint.position.x
        )
        val fiveEyeResult = FiveEyeResult(eyeRatios, fiveEyeDescription, fiveEyeLineXs)

        return ThreeCourtFiveEyeResult(threeCourtResult, fiveEyeResult)
    }
}