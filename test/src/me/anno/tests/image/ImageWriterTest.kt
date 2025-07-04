package me.anno.tests.image

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.io.files.FileFileRef
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class ImageWriterTest {

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testImageWriter() {
        OfficialExtensions.initForTests()
        val src = res.getChild("textures/Pacman.png")
        val srcImage = ImageCache[src].waitFor()!!
        val tmp = FileFileRef.createTempFile(src.nameWithoutExtension, src.lcExtension)
        srcImage.write(tmp)
        val cloneImage = ImageCache[tmp].waitFor()!!
        assertEquals(srcImage.width, cloneImage.width)
        assertEquals(srcImage.height, cloneImage.height)
        srcImage.forEachPixel { x, y ->
            assertEquals(srcImage.getRGB(x, y), cloneImage.getRGB(x, y))
        }
        tmp.delete()
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testHDRImageWriter() {
        OfficialExtensions.initForTests()
        val srcImage = FloatImage(
            2, 2, 3,
            floatArrayOf(
                0f, 1f, 1f,
                1f, 1f, 2f,
                2f, 2f, 2f,
                3f, 0f, 3f
            )
        )
        val tmp = FileFileRef.createTempFile("img", "hdr")
        srcImage.write(tmp)
        val cloneImage = ImageCache[tmp].waitFor() as FloatImage
        assertEquals(srcImage.width, cloneImage.width)
        assertEquals(srcImage.height, cloneImage.height)
        srcImage.forEachPixel { x, y ->
            for (c in 0 until 3) {
                assertEquals(
                    srcImage.getValue(x, y, c),
                    cloneImage.getValue(x, y, c)
                )
            }
        }
        tmp.delete()
    }
}