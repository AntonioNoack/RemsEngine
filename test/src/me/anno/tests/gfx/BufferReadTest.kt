package me.anno.tests.gfx

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.StaticBuffer
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertSame
import me.anno.utils.pooling.ByteBufferPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteOrder
import kotlin.random.Random

class BufferReadTest {

    private val attributes = bind(Attribute("values", 1))

    val size = 1024

    private fun createBuffer(): StaticBuffer {
        val buffer = StaticBuffer("brt", attributes, size shr 2)
        val nio = buffer.getOrCreateNioBuffer()
        assertEquals(ByteOrder.nativeOrder(), nio.order())

        // just fill random data into it
        val rnd = Random(1324)
        repeat(size shr 2) {
            nio.putInt(rnd.nextInt())
        }
        buffer.isUpToDate = false
        return buffer
    }

    @BeforeEach
    fun init() {
        HiddenOpenGLContext.createOpenGL()
    }

    @Test
    fun testReadAsByteBuffer() {
        val buffer = createBuffer()
        buffer.ensureBuffer()
        val readValues = ByteBufferPool.allocateDirect(size)
        assertEquals(size, readValues.remaining())
        assertSame(readValues, buffer.readAsByteBuffer(0, readValues))
    }

    @Test
    fun testReadAsIntArray() {
        val buffer = createBuffer()
        buffer.ensureBuffer()
        val readValues = buffer.readAsIntArray(0)
        assertEquals(size shr 2, readValues.size)
        val nio = buffer.nioBuffer!!
        assertEquals(ByteOrder.nativeOrder(), nio.order())
        for (i in 0 until size.shr(2)) {
            assertEquals(nio.getInt(i.shl(2)), readValues[i])
        }
    }

    @Test
    fun testReadAsFloatArray() {
        val buffer = createBuffer()
        buffer.ensureBuffer()
        val readValues = buffer.readAsFloatArray(0)
        assertEquals(size shr 2, readValues.size)
        val nio = buffer.nioBuffer!!
        assertEquals(ByteOrder.nativeOrder(), nio.order())
        for (i in 0 until size.shr(2)) {
            assertEquals(nio.getFloat(i.shl(2)), readValues[i])
        }
    }

    @Test
    fun testReadAsShortArray() {
        val buffer = createBuffer()
        buffer.ensureBuffer()
        val readValues = buffer.readAsShortArray(0)
        assertEquals(size shr 1, readValues.size)
        val nio = buffer.nioBuffer!!
        assertEquals(ByteOrder.nativeOrder(), nio.order())
        for (i in 0 until size.shr(1)) {
            assertEquals(nio.getShort(i.shl(1)), readValues[i])
        }
    }
}