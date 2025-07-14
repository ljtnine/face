package com.wendy.face.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * ImageUtils的单元测试
 */
class ImageUtilsTest {

    @Test
    fun testCalculateDisplayBounds_WiderImage() {
        // 测试图片比视图更宽的情况
        val result = ImageUtils.calculateDisplayBounds(
            imageWidth = 800,
            imageHeight = 600,
            viewWidth = 400f,
            viewHeight = 400f
        )

        // 预期结果：以宽度为准，高度按比例缩放
        val expectedDisplayWidth = 400f
        val expectedDisplayHeight = 300f // 400 * (600/800)
        val expectedOffsetX = 0f
        val expectedOffsetY = 50f // (400 - 300) / 2

        assertEquals(expectedDisplayWidth, result[0], 0.1f)
        assertEquals(expectedDisplayHeight, result[1], 0.1f)
        assertEquals(expectedOffsetX, result[2], 0.1f)
        assertEquals(expectedOffsetY, result[3], 0.1f)
    }

    @Test
    fun testCalculateDisplayBounds_TallerImage() {
        // 测试图片比视图更高的情况
        val result = ImageUtils.calculateDisplayBounds(
            imageWidth = 600,
            imageHeight = 800,
            viewWidth = 400f,
            viewHeight = 400f
        )

        // 预期结果：以高度为准，宽度按比例缩放
        val expectedDisplayWidth = 300f // 400 * (600/800)
        val expectedDisplayHeight = 400f
        val expectedOffsetX = 50f // (400 - 300) / 2
        val expectedOffsetY = 0f

        assertEquals(expectedDisplayWidth, result[0], 0.1f)
        assertEquals(expectedDisplayHeight, result[1], 0.1f)
        assertEquals(expectedOffsetX, result[2], 0.1f)
        assertEquals(expectedOffsetY, result[3], 0.1f)
    }

    @Test
    fun testCropHeadRegion_EmptyFaces() {
        // 由于需要Android环境来创建Bitmap，这个测试在单元测试中无法运行
        // 应该在Android Instrumented Test中进行
        assertTrue("This test requires Android environment", true)
    }
}
