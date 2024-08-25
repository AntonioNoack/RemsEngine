package me.anno.tests.io

import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.CountingInputStream
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class CountingInputStreamTest {
    @Test
    fun testCIS(){
        val rawStream = ByteArrayInputStream(ByteArray(1024))
        val stream = CountingInputStream(rawStream)
        assertEquals(0L, stream.position)
        stream.read()
        assertEquals(1L, stream.position)
        stream.read(ByteArray(100))
        assertEquals(101L, stream.position)
        stream.read(ByteArray(100), 9, 15)
        assertEquals(116L, stream.position)
        stream.read(ByteArray(1024))
        assertEquals(1024L, stream.position)
    }
}