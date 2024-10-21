package me.anno.tests.image

import me.anno.image.raw.IntImage
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ResizeImageTest {

    @Test
    fun testDownscaleInteger() {
        testDownscaleInteger(0, 0, 3, 3, 0f)
        testDownscaleInteger(10, 9, 7, 8, 1f / 7f)
    }

    // todo test fractional scale
    // todo test upscaling with interpolation

    fun testDownscaleInteger(sx: Int, sy: Int, fx: Int, fy: Int, tolerance: Float) {
        val random = Random(1234L)
        val smallSrcImage = IntImage(sx, sy, true)
        for (i in smallSrcImage.data.indices) {
            smallSrcImage.data[i] = random.nextInt()
        }
        val srcImage = smallSrcImage.scaleUp(fx, fy)
        val dstImage = srcImage.resized(sx, sy, false).asIntImage()
        assertEquals(sx, dstImage.width)
        assertEquals(sy, dstImage.height)
        smallSrcImage.forEachPixel { x, y ->
            val s = smallSrcImage.getRGB(x, y)
            val d = dstImage.getRGB(x, y)
            assertEquals(s.r01(), d.r01(), tolerance)
            assertEquals(s.g01(), d.g01(), tolerance)
            assertEquals(s.b01(), d.b01(), tolerance)
            assertEquals(s.a01(), d.a01(), tolerance)
        }
    }
}