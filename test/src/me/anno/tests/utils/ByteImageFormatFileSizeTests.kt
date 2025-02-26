package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.files.Files.formatFileSize
import org.junit.jupiter.api.Test

class ByteImageFormatFileSizeTests {
    @Test
    fun testFormatFileSizeKibiSizes() {
        val divider = 1024
        assertEquals("980 kiB", (980 * 1024L + 511).formatFileSize(divider))
        assertEquals("835 kiB", (834 * 1024L + 512).formatFileSize(divider))
        assertEquals("17.0 MiB", (17 * 1024L * 1024).formatFileSize(divider))
        assertEquals("173 GiB", (173 * 1024L * 1024L * 1024).formatFileSize(divider))
        assertEquals("257 TiB", (257 * 1024 * 1024L * 1024L * 1024).formatFileSize(divider))
    }

    @Test
    fun testFormatFileSizeRounding() {
        val divider = 1000
        assertEquals("980 kB", 980_499L.formatFileSize(divider))
        assertEquals("835 kB", 834_500L.formatFileSize(divider))
    }

    @Test
    fun testFormatFileSizeDimensions() {
        val divider = 1000
        assertEquals("1 Byte", 1L.formatFileSize(divider))
        assertEquals("12 Bytes", 12L.formatFileSize(divider))
        assertEquals("123 Bytes", 123L.formatFileSize(divider))
        assertEquals("1.23 kB", 123_4L.formatFileSize(divider))
        assertEquals("12.3 kB", 123_45L.formatFileSize(divider))
        assertEquals("123 kB", 123_456L.formatFileSize(divider))
        assertEquals("1.23 MB", 123_456_7L.formatFileSize(divider))
        assertEquals("12.3 MB", 123_456_78L.formatFileSize(divider))
        assertEquals("123 MB", 123_456_789L.formatFileSize(divider))
    }

    @Test
    fun testFormatFileSizeDimensionsBig() {
        val divider = 1000
        assertEquals("123 Bytes", 123L.formatFileSize(divider))
        assertEquals("123 kB", 123_000L.formatFileSize(divider))
        assertEquals("123 MB", 123_000_000L.formatFileSize(divider))
        assertEquals("123 GB", 123_000_000_000L.formatFileSize(divider))
        assertEquals("123 TB", 123_000_000_000_000L.formatFileSize(divider))
        assertEquals("123 PB", 123_000_000_000_000_000L.formatFileSize(divider))
        assertEquals("9.22 EB", Long.MAX_VALUE.formatFileSize(divider))
    }
}