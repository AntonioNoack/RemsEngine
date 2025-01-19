package me.anno.tests.image

import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.image.raw.IntImage
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2i
import org.junit.jupiter.api.Test

class ImageTests {

    fun createSampleImage(alpha: Boolean): IntImage {
        return IntImage(
            3, 3,
            intArrayOf(
                0, 10, 20,
                40, 50, 60,
                70, 80, 90
            ), alpha
        )
    }

    @Test
    fun testGetValueAt() {
        val image = createSampleImage(false)
        // test getting fields
        assertEquals(0f, image.getValueAt(0f, 0f, 0))
        assertEquals(10f, image.getValueAt(1f, 0f, 0))
        assertEquals(20f, image.getValueAt(2f, 0f, 0))
        assertEquals(40f, image.getValueAt(0f, 1f, 0))
        assertEquals(50f, image.getValueAt(1f, 1f, 0))
        assertEquals(60f, image.getValueAt(2f, 1f, 0))
        assertEquals(0f, image.getValueAt(-10f, -5f, 0))
        // test NaN input
        assertEquals(Float.NaN, image.getValueAt(Float.NaN, -5f, 0)) // how is this test passing??? NaN != NaN
        // test interpolation x
        assertEquals(11f, image.getValueAt(1.1f, 0f, 0))
        assertEquals(17f, image.getValueAt(1.7f, 0f, 0))
        // test interpolation y
        assertEquals(18f, image.getValueAt(1f, 0.2f, 0))
        assertEquals(42f, image.getValueAt(1f, 0.8f, 0))
        // test interpolation x,y
        assertEquals(21f, image.getValueAt(1.3f, 0.2f, 0))
        assertEquals(49f, image.getValueAt(1.7f, 0.8f, 0))
        // test alpha is set
        assertEquals(255f, image.getValueAt(0f, 0f, 24))
    }

    @Test
    fun testSampleRGB() {
        // todo test filtering and clamping
    }

    @Test
    fun testCreateTexture() {
        HiddenOpenGLContext.createOpenGL()
        val image = createSampleImage(true)
        val texture = Texture2D("img", 3, 3, 1)
        image.createTexture(texture, sync = true, checkRedundancy = false) { tex, err ->
            assertTrue(tex === texture)
            assertNull(err)
            val clonedImage = tex!!.createImage(false, withAlpha = true)
            assertTrue(clonedImage !== image)
            assertTrue(clonedImage.data.contentEquals(image.data))
        }
    }

    @Test
    fun testCreateTextureByRef() {
        HiddenOpenGLContext.createOpenGL()
        val image = createSampleImage(true)
        val texture = TextureCache[image.ref, false]
        assertNotNull(texture)
        val clonedImage = texture!!.createImage(false, withAlpha = true)
        assertTrue(clonedImage !== image)
        assertTrue(clonedImage.data.contentEquals(image.data))
    }

    @Test
    fun testScaleUp() {
        val original = createSampleImage(false)
        val scaled = original.scaleUp(2, 3)
        scaled.forEachPixel { x, y ->
            assertEquals(original.getRGB(x / 2, y / 3), scaled.getRGB(x, y))
        }
    }

    @Test
    fun testSplit() {
        // todo test
        val original = IntImage(
            3, 3,
            intArrayOf(
                0, 10, 20,
                40, 50, 60,
                70, 80, 90
            ), false
        )
        val split = original.split(2, 2)
        assertEquals(4, split.size)
        assertEquals(1, split[0].width)
        assertEquals(1, split[0].height)
        assertEquals(2, split[1].width)
        assertEquals(1, split[1].height)
        assertEquals(1, split[2].width)
        assertEquals(2, split[2].height)
        assertEquals(2, split[3].width)
        assertEquals(2, split[3].height)
        assertEquals(intArrayOf(0), split[0].asIntImage().cloneData())
        assertEquals(intArrayOf(10, 20), split[1].asIntImage().cloneData())
        assertEquals(intArrayOf(40, 70), split[2].asIntImage().cloneData())
        assertEquals(intArrayOf(50, 60, 80, 90), split[3].asIntImage().cloneData())
    }

    @Test
    fun testForEachPixel() {
        val image = createSampleImage(true)
        val coordinates = HashSet<Vector2i>()
        image.forEachPixel { x, y ->
            assertTrue(coordinates.add(Vector2i(x, y)))
        }
        assertEquals(image.width * image.height, coordinates.size)
        for (y in 0 until image.height) {
            for (x in 0 until image.height) {
                assertContains(Vector2i(x, y), coordinates)
            }
        }
    }

    @Test
    fun testFlipY() {
        val original = IntImage(2, 2, intArrayOf(0, 1, 2, 3), false)
        original.flipY()
        assertTrue(intArrayOf(2, 3, 0, 1).contentEquals(original.cloneData()))
    }
}