package me.anno.tests.image.utils

import me.anno.image.utils.GaussianBlur
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.exp

class GaussianBlurTest {

    @Test
    fun testDoesNotCrash() {
        for (w in listOf(4, 8, 15, 31, 49, 50)) {
            for (h in listOf(7, 15, 32)) {
                val image = FloatArray(w * h)
                GaussianBlur.gaussianBlur(image, w, h, 0, w, 11, false)
            }
        }
    }

    @Test
    fun testX() {
        val w = 11
        val cx = w shr 1
        val h = 1
        val image = FloatArray(w * h)
        image[cx] = 1f
        val thickness = 11
        GaussianBlur.gaussianBlur(image, w, h, 0, w, thickness, true)
        for (i in image.indices) {
            val x = 3.3f * (i - cx).toFloat() / thickness
            val expected = 0.15f * exp(-x * x)
            // println("$i: ${image[i]} / $expected")
            assertEquals(expected, image[i], 0.03f)
        }
        assertEquals(image.sum(), 1f, 0.01f)
    }

    @Test
    fun testY() {
        val w = 1
        val h = 11
        val cx = h shr 1
        val image = FloatArray(w * h)
        image[cx] = 1f
        val thickness = 11
        GaussianBlur.gaussianBlur(image, w, h, 0, 1, thickness, true)
        for (i in image.indices) {
            val x = 3.3f * (i - cx).toFloat() / thickness
            val expected = 0.15f * exp(-x * x)
            // println("$i: ${image[i]} / $expected")
            assertEquals(expected, image[i], 0.03f)
        }
        assertEquals(image.sum(), 1f, 0.01f)
    }


    @Test
    fun testXEqualsY() {
        val cx = 5
        val imageX = FloatArray(11)
        val imageY = FloatArray(11)
        imageX[cx] = 1f
        imageY[cx] = 1f
        val thickness = 11
        GaussianBlur.gaussianBlur(imageX, 11, 1, 0, 11, thickness, true)
        GaussianBlur.gaussianBlur(imageY, 1, 11, 0, 1, thickness, true)
        for (i in 0 until 11) {
            assertEquals(imageX[i], imageY[i])
        }
    }
}