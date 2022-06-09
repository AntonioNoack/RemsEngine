package me.anno.gpu.buffer

import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER

class ComputeBuffer(elementCount: Int, attr: List<Attribute>) : OpenGLBuffer(GL_SHADER_STORAGE_BUFFER, attr) {

    init {
        this.elementCount = elementCount
        createNioBuffer()
    }

    override fun createNioBuffer() {
        val byteSize = elementCount * attributes.sumOf { it.byteSize }
        nioBuffer = ByteBufferPool.allocateDirect(byteSize)
    }

}