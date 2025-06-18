package me.anno.gpu.buffer

import me.anno.gpu.GFX
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.opengl.GL46C.glGetBufferSubData
import java.nio.ByteBuffer

class ComputeBuffer(name: String, attr: AttributeLayout, elementCount: Int, type: Int = GL_SHADER_STORAGE_BUFFER) :
    OpenGLBuffer(name, type, attr, BufferUsage.STATIC) {

    init {
        this.elementCount = elementCount
    }

    override fun createNioBuffer(): ByteBuffer {
        val byteSize = attributes.totalSize(elementCount)
        return ByteBufferPool.allocateDirect(byteSize)
    }

    @Suppress("unused")
    fun readDataF(
        startIndex: Long = 0L,
        values: FloatArray = FloatArray(((elementCount - startIndex) * stride / 4).toInt())
    ): FloatArray {
        ensureBuffer()
        bindBuffer(type, pointer)
        GFX.check()
        glGetBufferSubData(type, startIndex * stride, values)
        GFX.check()
        return values
    }

    fun readDataI(
        startIndex: Long = 0L,
        values: IntArray = IntArray(((elementCount - startIndex) * stride / 4).toInt())
    ): IntArray {
        ensureBuffer()
        bindBuffer(type, pointer)
        GFX.check()
        glGetBufferSubData(type, startIndex * stride, values)
        GFX.check()
        return values
    }
}