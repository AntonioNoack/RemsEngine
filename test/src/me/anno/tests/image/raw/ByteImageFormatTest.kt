package me.anno.tests.image.raw

import me.anno.image.raw.ByteImageFormat
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ByteImageFormatTest {
    companion object {
        fun supportedMask(numChannels: Int): Int {
            return when (numChannels) {
                1 -> 0xff0000
                2 -> 0xffff00
                3 -> 0xffffff
                else -> -1
            }
        }
    }

    @Test
    fun testFromAndToBytes() {
        val random = Random(1234)
        val buffer = ByteArray(16)
        buffer[0] = -1
        for (format in ByteImageFormat.entries) {
            buffer[format.numChannels + 1] = -2

            val supportedMask = supportedMask(format.numChannels)
            for (i in 0 until 100) {
                val inputColor = random.nextInt()
                val expectedColor = inputColor and supportedMask
                format.toBytes(inputColor, buffer, 1)
                val actualColor = format.fromBytes(buffer, 1, true) and supportedMask
                assertEquals(expectedColor, actualColor)
            }

            assertEquals(-1, buffer[0].toInt())
            assertEquals(-2, buffer[format.numChannels + 1].toInt())
        }
    }
}