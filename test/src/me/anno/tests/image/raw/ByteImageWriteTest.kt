package me.anno.tests.image.raw

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.utils.Color.black
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class ByteImageWriteTest {
    @Test
    fun testWritingAndLoadingByteImage() {
        OfficialExtensions.initForTests()

        val format = ByteImageFormat.R
        val image = ByteImage(10, 10, format)
        val imageData = image.data
        for (i in imageData.indices) imageData[i] = i.toByte()

        val tmpFile = InnerTmpByteFile(ByteArray(0), "png")
        image.write(tmpFile)

        assertTrue(tmpFile.length() > 0)

        val readImage = ImageCache[tmpFile].waitFor()!!
        assertEquals(image.width, readImage.width)
        assertEquals(image.height, readImage.height)
        val compareMask = if (readImage.hasAlphaChannel) -1 else 0xffffff
        image.forEachPixel { x, y ->
            val expectedColor = image.getRGB(x, y)
            val actualColor = image.getRGB(x, y)
            assertEquals(expectedColor and compareMask, actualColor and compareMask)

            val gray = image.getIndex(x, y).and(255)
            val grayColor = black or (gray * 0x10101)
            assertEquals(expectedColor and compareMask, grayColor and compareMask)
        }
    }
}