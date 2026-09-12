package com.melakunet.snapshop.logic

import org.junit.Test
import org.junit.Assert.*

class CropMathTest {

    private fun calculateCrop(
        rectLeft: Float, rectTop: Float, rectRight: Float, rectBottom: Float,
        width: Int, height: Int
    ): List<Int> {
        val l = (rectLeft * width).toInt().coerceIn(0, width - 1)
        val t = (rectTop * height).toInt().coerceIn(0, height - 1)
        val r = (rectRight * width).toInt().coerceIn(l + 1, width)
        val b = (rectBottom * height).toInt().coerceIn(t + 1, height)
        return listOf(l, t, r - l, b - t)
    }

    @Test
    fun testCenterCropMapping() {
        val result = calculateCrop(0.25f, 0.25f, 0.75f, 0.75f, 1000, 1000)
        assertEquals(250, result[0])
        assertEquals(250, result[1])
        assertEquals(500, result[2])
        assertEquals(500, result[3])
    }

    @Test
    fun testEdgeClamping() {
        val result = calculateCrop(-0.1f, -0.1f, 1.1f, 1.1f, 1000, 1000)
        assertEquals(0, result[0])
        assertEquals(0, result[1])
        assertEquals(1000, result[2])
        assertEquals(1000, result[3])
    }
}
