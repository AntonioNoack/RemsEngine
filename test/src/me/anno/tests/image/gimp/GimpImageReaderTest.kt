package me.anno.tests.image.gimp

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.raw.IntImage
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.utils.Color.toHexColor
import me.anno.utils.OS.desktop
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class GimpImageReaderTest {

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testSize() {
        val meta = getMeta(res.getChild("files/gimp-3x3.xcf"), false)!!
        assertEquals(3, meta.videoWidth)
        assertEquals(2, meta.videoHeight)
        assertEquals(1, meta.videoFrameCount)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testImageContent() {
        val image = ImageCache[res.getChild("files/gimp-3x3.xcf"), false]!!
        val baseline = IntImage(
            3, 2, intArrayOf(
                0xffffff, 0xff0000, 0x000000,
                0x77ff77, 0x3fc5e6, 0xffc900,
            ), false
        )
        image.write(desktop.getChild("incorrect gimp.png"))
        baseline.forEachPixel { x, y ->
            assertEquals(baseline.getRGB(x, y), image.getRGB(x, y)) {
                "Mismatch at ($x $y), ${baseline.getRGB(x, y).toHexColor()} != ${image.getRGB(x, y).toHexColor()}"
            }
        }
    }

    @Test
    fun testLayers() {
        val layers = res.getChild("files/gimp-3x3.xcf/layers")
        assertEquals(1, layers.listChildren().size)
        val layerFile = layers.getChild("Background.png")
        assertTrue(layerFile.exists)
        assertTrue(layerFile is ImageReadable)
        val image = ImageCache[layerFile, false]!!
        assertEquals(3, image.width)
        assertEquals(3, image.height)
        val baseline = IntImage(
            3, 3, intArrayOf(
                0xffffff, 0xff0000, 0x000000,
                0x77ff77, 0x3fc5e6, 0xffc900,
                0xb1b599, 0x8e9372, 0x666d44,
            ), false
        )
        baseline.forEachPixel { x, y ->
            assertEquals(baseline.getRGB(x, y), image.getRGB(x, y), "Mismatch at ($x $y)")
        }
    }
}