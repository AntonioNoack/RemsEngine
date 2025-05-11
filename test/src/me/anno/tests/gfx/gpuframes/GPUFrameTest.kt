package me.anno.tests.gfx.gpuframes

import me.anno.engine.DefaultAssets.iconTexture
import me.anno.engine.OfficialExtensions
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toVecRGB
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertGreaterThanEquals
import me.anno.utils.types.Booleans.toInt
import me.anno.video.formats.cpu.YUVFrames
import me.anno.video.formats.gpu.ARGBFrame
import me.anno.video.formats.gpu.BGRAFrame
import me.anno.video.formats.gpu.BGRFrame
import me.anno.video.formats.gpu.GPUFrame
import me.anno.video.formats.gpu.I420Frame
import me.anno.video.formats.gpu.I444Frame
import me.anno.video.formats.gpu.RGBAFrame
import me.anno.video.formats.gpu.RGBFrame
import me.anno.video.formats.gpu.Y4Frame
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class GPUFrameTest {
    companion object {
        private val LOGGER = LogManager.getLogger(GPUFrameTest::class)
    }

    private fun loadImage(): Image {
        return ImageCache[iconTexture, false]!!
    }

    private fun createBytes(image: Image, bytesPerPixel: Int): ByteArray {
        return ByteArray(image.width * image.height * bytesPerPixel)
    }

    private fun frameToImage(frame: GPUFrame, bytes: ByteArray): Image {
        frame.load(ByteArrayInputStream(bytes))
        Sleep.waitUntil(true) { frame.isCreated }
        val asTexture = frame.toTexture()
        val clonedImage = asTexture.createImage(flipY = false, withAlpha = true)
        assertEquals(frame.width, clonedImage.width)
        assertEquals(frame.height, clonedImage.height)
        return clonedImage
    }

    private fun validateToTexture(image: Image, frame: GPUFrame, bytes: ByteArray, mask: Int) {
        val clonedImage = frameToImage(frame, bytes)
        image.forEachPixel { x, y ->
            assertEquals(image.getRGB(x, y) and mask, clonedImage.getRGB(x, y) and mask)
        }
        frame.destroy()
    }

    @Suppress("SameParameterValue")
    private fun validateToTextureWithMargin(image: Image, frame: GPUFrame, bytes: ByteArray, margin: Int) {
        val clonedImage = frameToImage(frame, bytes)
        image.forEachPixel { x, y ->
            val expected = image.getRGB(x, y)
            val actual = clonedImage.getRGB(x, y)
            assertEquals(expected.r(), actual.r(), margin)
            assertEquals(expected.g(), actual.g(), margin)
            assertEquals(expected.b(), actual.b(), margin)
        }
        frame.destroy()
    }

    private fun init(bytesPerPixel: Int): Pair<Image, ByteArray> {
        OfficialExtensions.initForTests()
        HiddenOpenGLContext.createOpenGL()
        val image = loadImage()
        val bytes = createBytes(image, bytesPerPixel)
        return image to bytes
    }

    @Test
    fun testRGBAFrame() {
        val (image, bytes) = init(4)
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            bytes[i * 4] = rgb.r().toByte()
            bytes[i * 4 + 1] = rgb.g().toByte()
            bytes[i * 4 + 2] = rgb.b().toByte()
            bytes[i * 4 + 3] = rgb.a().toByte()
        }
        val frame = RGBAFrame(image.width, image.height)
        validateToTexture(image, frame, bytes, -1)
    }

    @Test
    fun testARGBFrame() {
        val (image, bytes) = init(4)
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            bytes[i * 4] = rgb.a().toByte()
            bytes[i * 4 + 1] = rgb.r().toByte()
            bytes[i * 4 + 2] = rgb.g().toByte()
            bytes[i * 4 + 3] = rgb.b().toByte()
        }
        val frame = ARGBFrame(image.width, image.height)
        validateToTexture(image, frame, bytes, -1)
    }

    @Test
    fun testBGRAFrame() {
        val (image, bytes) = init(4)
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            bytes[i * 4] = rgb.b().toByte()
            bytes[i * 4 + 1] = rgb.g().toByte()
            bytes[i * 4 + 2] = rgb.r().toByte()
            bytes[i * 4 + 3] = rgb.a().toByte()
        }
        val frame = BGRAFrame(image.width, image.height)
        validateToTexture(image, frame, bytes, -1)
    }

    @Test
    fun testRGBFrame() {
        val (image, bytes) = init(3)
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            bytes[i * 3] = rgb.r().toByte()
            bytes[i * 3 + 1] = rgb.g().toByte()
            bytes[i * 3 + 2] = rgb.b().toByte()
        }
        val frame = RGBFrame(image.width, image.height)
        validateToTexture(image, frame, bytes, 0xffffff)
    }

    @Test
    fun testBGRFrame() {
        val (image, bytes) = init(3)
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            bytes[i * 3] = rgb.b().toByte()
            bytes[i * 3 + 1] = rgb.g().toByte()
            bytes[i * 3 + 2] = rgb.r().toByte()
        }
        val frame = BGRFrame(image.width, image.height)
        validateToTexture(image, frame, bytes, 0xffffff)
    }

    @Test
    fun testY4Frame() {
        val (image, bytes) = init(1)
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            bytes[i] = rgb.g().toByte()
        }
        val frame = Y4Frame(image.width, image.height)
        validateToTexture(image, frame, bytes, 0x00ff00)
    }

    @Test
    fun testI444Frame() {
        val (image, bytes) = init(3)
        val stride = image.width * image.height
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            val yuv = YUVFrames.rgb2yuv(rgb)
            bytes[i] = yuv.r().toByte()
            bytes[i + stride] = yuv.g().toByte()
            bytes[i + stride * 2] = yuv.b().toByte()
        }
        val frame = I444Frame(image.width, image.height)
        validateToTextureWithMargin(image, frame, bytes, 3)
    }

    private fun makeOdd(i: Int): Int {
        return (i or 1) - 2
    }

    @Test
    fun testI420Frame() {
        val (image0, bytes) = init(2) // bytes/pixel: ~1.5
        val image = image0.cropped(0, 0, makeOdd(image0.width), makeOdd(image0.height))
        val halfWidth = ceilDiv(image.width, 2)
        val halfHeight = ceilDiv(image.height, 2)
        val uOffset = image.width * image.height
        val vOffset = uOffset + halfWidth * halfHeight
        image.forEachPixel { x, y ->
            val i = x + y * image.width
            val rgb = image.getRGB(x, y)
            val yuv = YUVFrames.rgb2yuv(rgb)
            bytes[i] = yuv.r().toByte()
            val j = x.shr(1) + y.shr(1) * halfWidth
            bytes[uOffset + j] = yuv.g().toByte()
            bytes[vOffset + j] = yuv.b().toByte()
        }
        val frame = I420Frame(image.width, image.height)
        validateToTextureApproximate(image, frame, bytes, 3, 0.8f) // 84% are actually correct
    }

    @Suppress("SameParameterValue")
    private fun validateToTextureApproximate(
        image: Image, frame: GPUFrame, bytes: ByteArray,
        margin: Int, ratio: Float
    ) {
        val clonedImage = frameToImage(frame, bytes)
        val bounds = AABBf()
        val tmp = Vector3f()
        val marginF = margin / 255f
        var correct = 0
        image.forEachPixel { x, y ->
            bounds.clear()
            for (dy in -1..1) {
                for (dx in -1..1) {
                    bounds.union(image.getRGB(x + dx, y + dy).toVecRGB(tmp))
                }
            }
            bounds.addMargin(marginF)
            val actual = clonedImage.getRGB(x, y)
            correct += bounds.testPoint(actual.toVecRGB(tmp)).toInt()
        }
        assertGreaterThanEquals(correct.toFloat(), image.width * image.height * ratio)
        LOGGER.info("Correct Pixels: ${correct}/${image.width * image.height}")
        frame.destroy()
    }
}