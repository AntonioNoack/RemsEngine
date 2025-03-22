package me.anno.tests.image

import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.image.raw.FloatImage
import me.anno.image.raw.IntImage
import me.anno.jvm.HiddenOpenGLContext
import me.anno.tests.image.raw.ByteImageFormatTest.Companion.supportedMask
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertIs
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertNotSame
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertSame
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2i
import org.junit.jupiter.api.Test

class ImageTests {

    fun createSampleImage(withAlpha: Boolean, multiplier: Int = 0x1020304): IntImage {
        return IntImage(
            3, 3,
            intArrayOf(
                0, 1, 2,
                4, 5, 6,
                7, 8, 9
            ).map { it * multiplier }.toIntArray(), withAlpha
        )
    }

    fun createByteImage(src: Image, format: ByteImageFormat): ByteImage {
        val dst = ByteImage(src.width, src.height, format)
        src.forEachPixel { x, y ->
            format.toBytes(src.getRGB(x, y), dst.data, dst.getIndex(x, y) * format.numChannels)
        }
        return dst
    }

    fun createFloatImage(src: Image, numChannels: Int): FloatImage {
        val dst = FloatImage(src.width, src.height, numChannels)
        val shifts = intArrayOf(16, 8, 0, 24)
        src.forEachPixel { x, y ->
            val rgb = src.getRGB(x, y)
            for (c in 0 until numChannels) {
                dst.setValue(x, y, c, rgb.shr(shifts[c]).and(255) / 255f)
            }
        }
        return dst
    }

    @Test
    fun testGetValueAt() {
        val image = createSampleImage(false, 10)
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
    fun testCreateTextureIntImage() {
        testCreateTextureImage(createSampleImage(true), supportedMask(4))
        testCreateTextureImage(createSampleImage(false), supportedMask(3))
    }

    @Test
    fun testCreateTextureByteImage() {
        val base = createSampleImage(true)
        for (format in ByteImageFormat.entries) {
            val image = createByteImage(base, format)
            testCreateTextureImage(image, supportedMask(format.numChannels))
        }
    }

    @Test
    fun testCreateTextureFloatImage() {
        val base = createSampleImage(true)
        for (numChannels in 1..4) {
            val image = createFloatImage(base, numChannels)
            testCreateTextureImage(image, supportedMask(numChannels))
        }
    }

    @Test
    fun testConfirmFloatTextureCreatesFloatImage() {
        HiddenOpenGLContext.createOpenGL()
        val base = createSampleImage(true)
        for (numChannels in 1..4) {
            val image = createFloatImage(base, numChannels)
            val texture = Texture2D("img", 3, 3, 1)
            image.createTexture(texture, checkRedundancy = false) { tex, err ->
                assertSame(tex, texture)
                assertNull(err)
                val clonedImage = assertIs(FloatImage::class, texture.createImage(false, withAlpha = true))
                assertNotSame(clonedImage, image)
                assertContentEquals(clonedImage, image)
                texture.destroy()
            }
        }
    }

    fun testCreateTextureImage(image: Image, mask: Int) {
        HiddenOpenGLContext.createOpenGL()
        val texture = Texture2D("img", 3, 3, 1)
        image.createTexture(texture, checkRedundancy = false) { tex, err ->
            assertSame(tex, texture)
            assertNull(err)
            val clonedImage = texture.createImage(false, withAlpha = true)
            assertNotSame(clonedImage, image)
            assertContentEqualsMasked(clonedImage, image, mask)
            texture.destroy()
        }
    }

    fun assertContentEqualsMasked(a: Image, b: Image, mask: Int) {
        assertEquals(a.width, b.width)
        assertEquals(a.height, b.width)
        a.forEachPixel { x, y ->
            val colorA = a.getRGB(x, y) and mask
            val colorB = b.getRGB(x, y) and mask
            assertEquals(colorA, colorB)
        }
    }

    fun assertContentEquals(a: Image, b: Image) {
        return assertContentEqualsMasked(a, b, -1)
    }

    @Test
    fun testCreateTextureByRef() {
        HiddenOpenGLContext.createOpenGL()
        val image = createSampleImage(true)
        val texture = TextureCache[image.ref, false]
        val clonedImage = assertNotNull(texture).createImage(false, withAlpha = true).asIntImage()
        assertNotSame(clonedImage, image)
        assertContentEquals(clonedImage.data, image.data)
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
        val original = createSampleImage(false, 10)
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