package me.anno.tests.gfx

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.StaticBuffer
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.assertions.assertEquals
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.WrapDirect.wrapDirect
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class BufferUploadPartiallyTest {

    private val size = 256
    private fun createBuffer(attributeType: AttributeType, put: (ByteBuffer, Int) -> Unit): StaticBuffer {
        val attributes = bind(Attribute("test", attributeType, 1))
        val buffer = StaticBuffer("bupt", attributes, size)
        val nio = buffer.getOrCreateNioBuffer()
        for (i in 0 until size) {
            put(nio, i)
        }
        assertEquals(nio.position(), buffer.stride * size)
        buffer.cpuSideChanged()
        buffer.ensureBuffer()
        return buffer
    }

    @BeforeEach
    fun init() {
        HiddenOpenGLContext.createOpenGL()
    }

    @Test
    fun testUploadByteArrayPartially() {
        val buffer = createBuffer(AttributeType.UINT8_NORM) { nio, i -> nio.put(i.toByte()) }
        val fromData = ByteArray(25) { it.toByte() }
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        val tmp = ByteBufferPool.allocateDirect(size)
        buffer.readAsByteBuffer(0, tmp)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i).toByte()
            assertEquals(expected, tmp[i])
        }
        ByteBufferPool.free(tmp)
        buffer.destroy()
    }

    @Test
    fun testUploadByteBufferPartially() {
        val buffer = createBuffer(AttributeType.UINT8_NORM) { nio, i -> nio.put(i.toByte()) }
        val (fromData, fromCleanup) = ByteArray(25) { it.toByte() }.wrapDirect(0, 25)
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        fromCleanup()
        val tmp = ByteBufferPool.allocateDirect(size)
        buffer.readAsByteBuffer(0, tmp)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i).toByte()
            assertEquals(expected, tmp[i])
        }
        ByteBufferPool.free(tmp)
        buffer.destroy()
    }

    @Test
    fun testUploadShortArrayPartially() {
        val buffer = createBuffer(AttributeType.UINT16_NORM) { nio, i -> nio.putShort(i.toShort()) }
        val fromData = ShortArray(25) { it.toShort() }
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        val tmp = buffer.readAsShortArray(0)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i).toShort()
            assertEquals(expected, tmp[i])
        }
        buffer.destroy()
    }

    @Test
    fun testUploadShortBufferPartially() {
        val buffer = createBuffer(AttributeType.UINT16_NORM) { nio, i -> nio.putShort(i.toShort()) }
        val (fromData, fromCleanup) = ShortArray(25) { it.toShort() }.wrapDirect(0, 25)
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        fromCleanup()
        val tmp = buffer.readAsShortArray(0)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i).toShort()
            assertEquals(expected, tmp[i])
        }
        buffer.destroy()
    }

    @Test
    fun testUploadIntArrayPartially() {
        val buffer = createBuffer(AttributeType.UINT32_NORM) { nio, i -> nio.putInt(i) }
        val fromData = IntArray(25) { it }
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        val tmp = buffer.readAsIntArray(0)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i)
            assertEquals(expected, tmp[i])
        }
        buffer.destroy()
    }

    @Test
    fun testUploadIntBufferPartially() {
        val buffer = createBuffer(AttributeType.UINT32_NORM) { nio, i -> nio.putInt(i) }
        val (fromData, fromCleanup) = IntArray(25) { it }.wrapDirect(0, 25)
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        fromCleanup()
        val tmp = buffer.readAsIntArray(0)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i)
            assertEquals(expected, tmp[i])
        }
        buffer.destroy()
    }

    @Test
    fun testUploadFloatArrayPartially() {
        val buffer = createBuffer(AttributeType.UINT32_NORM) { nio, i -> nio.putFloat(i.toFloat()) }
        val fromData = FloatArray(25) { it.toFloat() }
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        val tmp = buffer.readAsFloatArray(0)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i).toFloat()
            assertEquals(expected, tmp[i])
        }
        buffer.destroy()
    }

    @Test
    fun testUploadFloatBufferPartially() {
        val buffer = createBuffer(AttributeType.UINT32_NORM) { nio, i -> nio.putFloat(i.toFloat()) }
        val (fromData, fromCleanup) = FloatArray(25) { it.toFloat() }.wrapDirect(0, 25)
        buffer.uploadElementsPartially(0, fromData, 25, 17)
        fromCleanup()
        val tmp = buffer.readAsFloatArray(0)
        for (i in 0 until size) {
            val expected = (if (i - 17 in 0 until 25) i - 17 else i).toFloat()
            assertEquals(expected, tmp[i])
        }
        buffer.destroy()
    }
}