package me.anno.gpu.buffer

import me.anno.gpu.GFX
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL46C.glGetBufferSubData
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BUFFER

class ComputeBuffer(name: String, attr: List<Attribute>, elementCount: Int) :
    OpenGLBuffer(name, GL_SHADER_STORAGE_BUFFER, attr) {

    init {
        this.elementCount = elementCount
        createNioBuffer()
    }

    override fun createNioBuffer() {
        val byteSize = elementCount * attributes.sumOf { it.byteSize }
        nioBuffer = ByteBufferPool.allocateDirect(byteSize)
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