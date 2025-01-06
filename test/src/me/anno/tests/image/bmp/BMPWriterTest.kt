package me.anno.tests.image.bmp

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.bmp.BMPWriter
import me.anno.image.raw.IntImage
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class BMPWriterTest {
    @Test
    fun testWriter() {
        OfficialExtensions.initForTests()
        testWriter(true)
        testWriter(false)
    }

    fun testWriter(withAlpha: Boolean) {
        val original = IntImage(12, 9, withAlpha)
        val random = Random(1234)
        original.forEachPixel { x, y ->
            original.setRGB(x, y, random.nextInt())
        }
        val bmpBytes = BMPWriter.createBMP(original)
        assertEquals(bmpBytes.size.toLong(), BMPWriter.calculateSize(original))
        val tmpBmp = InnerTmpByteFile(bmpBytes)
        val clone = ImageCache[tmpBmp, false]!!
        assertEquals(original.width, clone.width)
        assertEquals(original.height, clone.height)
        assertEquals(original.hasAlphaChannel, clone.hasAlphaChannel)
        original.forEachPixel { x, y ->
            assertEquals(original.getRGB(x, y), clone.getRGB(x, y))
        }
    }
}