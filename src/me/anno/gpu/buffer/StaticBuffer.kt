package me.anno.gpu.buffer

import me.anno.gpu.GFXState
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.shader.Shader
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL46C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

open class StaticBuffer(
    name: String, attributes: AttributeLayout,
    var vertexCount: Int, usage: BufferUsage = BufferUsage.STATIC
) : Buffer(name, attributes, usage) {

    constructor(name: String, points: FloatArray, vertices: IntArray, attributes: List<Attribute>) :
            this(name, bind(attributes), vertices.size, BufferUsage.STATIC) {
        val dimPerPoint = attributes.sumOf { it.numComponents }
        for (v in vertices) {
            val baseIndex = v * dimPerPoint
            for (dOffset in 0 until dimPerPoint) {
                put(points[baseIndex + dOffset])
            }
        }
    }

    constructor(name: String, floats: FloatArray, attributes: List<Attribute>) :
            this(name, bind(attributes), floats.size / attributes.sumOf { it.numComponents }, BufferUsage.STATIC) {
        put(floats)
    }

    open fun clear() {
        val buffer = nioBuffer
        if (buffer != null) {
            buffer.position(0)
            buffer.limit(buffer.capacity())
        }
        isUpToDate = false
        drawLength = 0
    }

    fun ensureCapacity(capacityInBytes: Int) {
        val oldBytes = nioBuffer
        val size = oldBytes?.capacity() ?: 0
        if (capacityInBytes <= size) return
        val newCapacity = max(64, max(size * 2, capacityInBytes))
        val newBytes = ByteBufferPool.allocateDirect(newCapacity)
        if (oldBytes != null) {
            // copy over
            val position = oldBytes.position()
            oldBytes.position(0)
            newBytes.position(0)
            newBytes.put(oldBytes)
            newBytes.position(position)
            ByteBufferPool.free(oldBytes)
        }
        nioBuffer = newBytes
    }

    fun ensureHasExtraSpace(extraSizeInBytes: Int) {
        val oldBytes = nioBuffer
        val position = oldBytes?.position() ?: 0
        val newSize = extraSizeInBytes + position
        ensureCapacity(newSize)
    }

    final override fun createNioBuffer(): ByteBuffer {
        val byteSize = vertexCount * stride
        return ByteBufferPool.allocateDirect(byteSize)
            .order(ByteOrder.nativeOrder())
    }

    companion object {

        /**
         * Buffer with draw calls without any attributes
         * */
        private val nullBuffer = StaticBuffer(
            "null",
            bind(Attribute("nothing0", AttributeType.UINT8_NORM, 4)),
            4, BufferUsage.STATIC
        ).apply {
            putInt(0)
            putInt(1)
            putInt(2)
            putInt(3)
        }

        fun drawArraysNull(shader: Shader, mode: DrawMode, length: Int) {
            // we need a null array, or bind bogus data, because drivers don't like this
            // https://stackoverflow.com/questions/8039929/opengl-drawarrays-without-binding-vbo
            nullBuffer.drawMode = mode
            nullBuffer.ensureBuffer()
            nullBuffer.apply {
                val baseLength = mode.minLength
                bind(shader)
                GFXState.bind()
                GL46C.glDrawArraysInstanced(mode.id, 0, baseLength, length)
                unbind(shader)
            }
        }
    }
}