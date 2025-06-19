package me.anno.tests.image.bmp

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.bmp.BMPWriter
import me.anno.image.raw.IntImage
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class BMPDecoderTest {

    companion object {
        private val baseline = IntImage(
            3, 2, intArrayOf(
                0xffffff, 0xff0000, 0x000000,
                0x77ff77, 0x3fc5e6, 0xffc900,
            ), false
        )
    }

    fun createBmpFile(): FileReference {
        val binaryData = BMPWriter.createBMP(baseline)
        return InnerTmpByteFile(binaryData)
    }

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testSignature() {
        val src = createBmpFile()
        val signature = Signature.findName(src.readBytesSync())
        assertEquals("bmp", signature)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testSize() {
        val src = createBmpFile()
        val meta = getMeta(src).waitFor()!!
        assertEquals(3, meta.videoWidth)
        assertEquals(2, meta.videoHeight)
        assertEquals(1, meta.videoFrameCount)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testImageContent() {
        // todo this isn't using the standard BMP decoder, but the built-in one from ImageIO
        //  -> replace BMPDecoderClass?
        val image = ImageCache[createBmpFile()].waitFor()!!
        baseline.forEachPixel { x, y ->
            assertEquals(baseline.getRGB(x, y), image.getRGB(x, y), "Mismatch at ($x $y)")
        }
    }
}