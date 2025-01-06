package me.anno.tests.image.tga

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TgaImageReaderTest {

    companion object {
        private val baseline = IntImage(
            3, 2, intArrayOf(
                0xffffff, 0xff0000, 0x000000,
                0x77ff77, 0x3fc5e6, 0xffc900,
            ), false
        )
    }

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    @Test
    fun testSize() {
        val meta = getMeta(res.getChild("files/gimp-3x3.tga"), false)!!
        assertEquals(3, meta.videoWidth)
        assertEquals(2, meta.videoHeight)
        assertEquals(1, meta.videoFrameCount)
    }

    @Test
    fun testSize2() {
        val meta = getMeta(res.getChild("files/gimp-3x3-fy.tga"), false)!!
        assertEquals(3, meta.videoWidth)
        assertEquals(2, meta.videoHeight)
        assertEquals(1, meta.videoFrameCount)
    }

    @Test
    fun testImageContent() {
        val image = ImageCache[res.getChild("files/gimp-3x3.tga"), false]!!
        baseline.forEachPixel { x, y ->
            assertEquals(baseline.getRGB(x, y), image.getRGB(x, y), "Mismatch at ($x $y)")
        }
    }

    @Test
    fun testImageContent2() {
        val image = ImageCache[res.getChild("files/gimp-3x3-fy.tga"), false]!!
        baseline.forEachPixel { x, y ->
            assertEquals(baseline.getRGB(x, y), image.getRGB(x, y), "Mismatch at ($x $y)")
        }
    }
}