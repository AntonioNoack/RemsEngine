package me.anno.gpu.buffer

import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BUFFER
import java.nio.ByteBuffer

class ComputeBuffer(name: String, attr: AttributeLayout, elementCount: Int, type: Int = GL_SHADER_STORAGE_BUFFER) :
    GPUBuffer(name, type, attr, BufferUsage.STATIC) {

    init {
        this.elementCount = elementCount
    }

    override fun createNioBuffer(): ByteBuffer {
        val byteSize = attributes.totalSize(elementCount)
        return ByteBufferPool.allocateDirect(byteSize)
    }
}