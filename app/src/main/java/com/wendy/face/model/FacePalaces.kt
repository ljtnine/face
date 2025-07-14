package com.wendy.face.model

import com.google.mlkit.vision.facemesh.FaceMeshPoint

/**
 * 面相十二宫定义
 * 存储了每个宫位对应的Face Mesh关键点索引
 */
object FacePalaces {

    // 为了方便代码可读性，我们为一些关键点定义常量
    object Landmarks {
        const val NOSE_TIP = 1
        const val FOREHEAD_CENTER = 10
        const val CHIN_TIP = 152
        // ... 可以根据需要添加更多
    }

    // 使用LinkedHashMap确保迭代顺序与插入顺序一致，从而与TwelvePalaces中的列表顺序匹配
    // 修正原始定义中的重复索引问题
    val palaceIndices: LinkedHashMap<String, List<List<Int>>> = linkedMapOf(
        // 1. 命宫 (Life Palace) - 中轴线，单个区域
        "命宫" to listOf(
            listOf(10, 109, 67, 105, 53, 52, 65, 55, 9, 107, 336, 297, 338, 334, 283, 282, 295, 285)
        ),

        // 2. 财帛宫 (Wealth Palace) - 中轴线，单个区域
        "财帛宫" to listOf(
            listOf(6, 168, 197, 195, 1, 4, 5, 48, 125, 209, 49, 278, 354, 429, 279)
        ),

        // 3. 兄弟宫 (Siblings Palace) - 左右对称，两个区域
        "兄弟宫" to listOf(
            listOf(46, 53, 52, 65, 55, 107, 66, 105, 63, 70), // 左眉（与FaceOverlay一致）
            listOf(276, 283, 282, 295, 285, 336, 296, 334, 293, 300) // 右眉（与FaceOverlay一致）
        ),

        // 4. 田宅宫 (Property Palace) - 左右对称，两个区域
        "田宅宫" to listOf(
            listOf(70, 63, 105, 66, 107, 55, 65, 52, 53, 155, 154, 153, 144, 145, 33), // 左侧
            listOf(300, 293, 334, 296, 336, 285, 295, 282, 283, 382, 381, 380, 373, 374, 263) // 右侧
        ),

        // 5. 子女宫 (Children Palace) - 左右对称，两个区域 (眼袋、卧蚕部位，避开眼球)
        "子女宫" to listOf(
            listOf(116, 117, 118, 119, 120), // 左侧眼袋区域
            listOf(345, 346, 347, 348, 349)  // 右侧眼袋区域
        ),

        // 6. 奴仆宫 (Subordinates Palace) - 左右对称，两个区域
        "奴仆宫" to listOf(
            listOf(172, 136, 150, 149, 176, 58), // 左侧
            listOf(400, 377, 378, 379, 365, 288)  // 右侧
        ),

        // 7. 夫妻宫 (Spouse Palace) - 左右对称，两个区域
        "夫妻宫" to listOf(
            listOf(33, 133, 234, 127, 162), // 左侧
            listOf(263, 362, 454, 356, 389) // 右侧
        ),

        // 8. 疾厄宫 (Health Palace) - 中轴线，单个区域
        "疾厄宫" to listOf(
            listOf(6, 168, 128, 197, 357)
        ),

        // 9. 迁移宫 (Travel Palace) - 左右对称，两个区域
        "迁移宫" to listOf(
            listOf(70, 109, 103, 67, 104), // 左侧
            listOf(300, 338, 332, 297, 333) // 右侧
        ),

        // 10. 官禄宫 (Career Palace) - 中轴线，单个区域
        "官禄宫" to listOf(
            listOf(10, 151, 8)
        ),

        // 11. 福德宫 (Fortune Palace) - 左右对称，两个区域
        "福德宫" to listOf(
            listOf(66, 107, 105, 103, 67, 109), // 左侧
            listOf(296, 336, 334, 332, 297, 338) // 右侧
        ),

        // 12. 父母宫 (Parents Palace) - 左右对称，两个区域
        "父母宫" to listOf(
            listOf(105, 104, 67), // 左侧 (日角)
            listOf(334, 333, 297)  // 右侧 (月角)
        )
    )

    /**
     * 获取特定宫位的所有关键点坐标
     * @param palaceName 宫位名称
     * @param allPoints 所有468个面部关键点
     * @return 该宫位对应的FaceMeshPoint列表的列表（每个子列表是一个区域）
     */
    fun getPalacePoints(palaceName: String, allPoints: List<FaceMeshPoint>): List<List<FaceMeshPoint>> {
        val indicesList = palaceIndices[palaceName] ?: return emptyList()
        return indicesList.map { indices ->
            indices.mapNotNull { index ->
                allPoints.getOrNull(index)
            }
        }
    }
}