package me.anno.tests.image.utils

import me.anno.image.utils.BoxBlur
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

class BoxBlurTest {

    @Test
    fun testBlurXLeft() {
        val src = floatArrayOf(10f, 0f, 0f, 0f, 0f, 0f, 0f)
        BoxBlur.boxBlurX(src, src.size, 1, 0, src.size, 5, false)
        assertContentEquals(floatArrayOf(30f, 20f, 10f, 0f, 0f, 0f, 0f), src)
    }

    @Test
    fun testBlurXCenter() {
        val src = floatArrayOf(0f, 0f, 0f, 10f, 0f, 0f, 0f)
        BoxBlur.boxBlurX(src, src.size, 1, 0, src.size, 5, false)
        assertContentEquals(floatArrayOf(0f, 10f, 10f, 10f, 10f, 10f, 0f), src)
    }

    @Test
    fun testBlurXRight() {
        val src = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 10f)
        BoxBlur.boxBlurX(src, src.size, 1, 0, src.size, 5, false)
        assertContentEquals(floatArrayOf(0f, 0f, 0f, 0f, 10f, 20f, 30f), src)
    }

    @Test
    fun testBlurXJoined() {
        val src = floatArrayOf(
            10f, 0f, 0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 10f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f, 0f, 10f
        )
        BoxBlur.boxBlurX(src, 7, 3, 0, 7, 5, false)
        assertContentEquals(
            floatArrayOf(
                30f, 20f, 10f, 0f, 0f, 0f, 0f,
                0f, 10f, 10f, 10f, 10f, 10f, 0f,
                0f, 0f, 0f, 0f, 10f, 20f, 30f
            ), src
        )
    }

    @Test
    fun testBlurYLeft() {
        val src = floatArrayOf(10f, 0f, 0f, 0f, 0f, 0f, 0f)
        BoxBlur.boxBlurY(src, 1, src.size, 0, 1, 5, false)
        assertContentEquals(floatArrayOf(30f, 20f, 10f, 0f, 0f, 0f, 0f), src)
    }

    @Test
    fun testBlurYCenter() {
        val src = floatArrayOf(0f, 0f, 0f, 10f, 0f, 0f, 0f)
        BoxBlur.boxBlurY(src, 1, src.size, 0, 1, 5, false)
        assertContentEquals(floatArrayOf(0f, 10f, 10f, 10f, 10f, 10f, 0f), src)
    }

    @Test
    fun testBlurYRight() {
        val src = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 10f)
        BoxBlur.boxBlurY(src, 1, src.size, 0, 1, 5, false)
        assertContentEquals(floatArrayOf(0f, 0f, 0f, 0f, 10f, 20f, 30f), src)
    }

    @Test
    fun testBlurYJoined() {
        val src = floatArrayOf(
            10f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 10f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 10f
        )
        BoxBlur.boxBlurY(src, 3, 7, 0, 3, 5, false)
        assertContentEquals(
            floatArrayOf(
                30f, 0f, 0f,
                20f, 10f, 0f,
                10f, 10f, 0f,
                0f, 10f, 0f,
                0f, 10f, 10f,
                0f, 10f, 20f,
                0f, 0f, 30f
            ), src
        )
    }

    @Test
    fun testBlurXEven() {
        val src = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 10f)
        BoxBlur.boxBlurX(src, src.size, 1, 0, src.size, 6, false)
        assertContentEquals(floatArrayOf(0f, 0f, 0f, 10f, 20f, 30f, 40f), src)
    }

    @Test
    fun testBlurYEven() {
        val src = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 10f)
        BoxBlur.boxBlurY(src, 1, src.size, 0, 1, 6, false)
        assertContentEquals(floatArrayOf(0f, 0f, 0f, 10f, 20f, 30f, 40f), src)
    }
}