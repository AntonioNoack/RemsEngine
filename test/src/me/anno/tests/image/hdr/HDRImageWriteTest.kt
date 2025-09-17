package me.anno.tests.image.hdr

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.utils.assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HDRImageWriteTest {
    @Test
    fun testWritingAndLoadingHDRImage() {
        OfficialExtensions.initForTests()

        for (numChannels in 1..3) {
            val image = FloatImage(10, 10, numChannels)
            val imageData = image.data
            for (i in imageData.indices) imageData[i] = i.toFloat()

            val tmpFile = InnerTmpByteFile(ByteArray(0), "hdr")
            image.write(tmpFile)

            assertTrue(tmpFile.length() > 0)

            val readImage = ImageCache[tmpFile].waitFor() as FloatImage
            assertEquals(image.width, readImage.width)
            assertEquals(image.height, readImage.height)
            assertFalse(readImage.hasAlphaChannel)

            image.forEachPixel { x, y ->
                for (channel in 0 until numChannels) {
                    val expected = image.getValue(x, y, channel)
                    val actual = image.getValue(x, y, channel)
                    assertEquals(expected, actual)
                }
            }
        }
    }
}