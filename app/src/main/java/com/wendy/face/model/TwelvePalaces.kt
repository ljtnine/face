package com.wendy.face.model

import com.google.mlkit.vision.facemesh.FaceMeshPoint
import androidx.compose.ui.geometry.Offset

data class TwelvePalacesData(val palaces: Map<String, String>)

/**
 * 面相十二宫位置定义
 * 基于传统面相学理论和MediaPipe Face Mesh关键点
 */
object TwelvePalaces {
    
    /**
     * 十二宫的名称和序号
     */
    val palaceNames = listOf(
        "命宫",      // ① 印堂部位，双眉之间
        "财帛宫",    // ② 鼻子部位
        "兄弟宫",    // ③ 左右眉毛
        "田宅宫",    // ④ 上眼皮部位
        "子女宫",    // ⑤ 眼袋、卧蚕部位
        "奴仆宫",    // ⑥ 下巴两侧
        "夫妻宫",    // ⑦ 眼尾、鱼尾纹部位
        "疾厄宫",    // ⑧ 山根、鼻梁部位
        "迁移宫",    // ⑨ 额角、太阳穴部位
        "官禄宫",    // ⑩ 额头中央
        "福德宫",    // ⑪ 额头两侧
        "父母宫"     // ⑫ 额头上方
    )
    
    /**
     * 获取十二宫的圆圈序号
     */
    fun getCircledNumber(index: Int): String {
        val circledNumbers = arrayOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩", "⑪", "⑫")
        return if (index in 0..11) circledNumbers[index] else ""
    }
    
    /**
     * 根据人脸关键点计算十二宫的位置
     * @param allPoints 所有468个面部关键点
     * @return Pair<List<Offset>, List<Int>>
     *         - List<Offset>: 所有宫位区域中心点的扁平化列表
     *         - List<Int>: 每个宫位对应的坐标点数量（1或2）
     */
    fun calculatePalacePositions(
        allPoints: List<FaceMeshPoint>
    ): Pair<List<Offset>, List<Int>> {
        if (allPoints.size < 468) return Pair(emptyList(), emptyList())

        val allPositions = mutableListOf<Offset>()
        val counts = mutableListOf<Int>()

        FacePalaces.palaceIndices.forEach { (_, pointGroups) ->
            var countForPalace = 0
            pointGroups.forEach { group ->
                if (group.isNotEmpty()) {
                    val points = group.mapNotNull { allPoints.getOrNull(it) }
                    if (points.isNotEmpty()) {
                        val centerX = points.map { it.position.x }.average().toFloat()
                        val centerY = points.map { it.position.y }.average().toFloat()
                        allPositions.add(Offset(centerX, centerY))
                        countForPalace++
                    }
                }
            }
            counts.add(countForPalace)
        }
        return Pair(allPositions, counts)
    }
    
    /**
     * 获取宫位名称（带序号）
     */
    fun getPalaceNameWithNumber(index: Int): String {
        return if (index in 0..11) {
            "${getCircledNumber(index)} ${palaceNames[index]}"
        } else ""
    }
}
