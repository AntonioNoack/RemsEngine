package me.anno.gpu.buffer

import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER

// OpenGL 4.3, OpenGL ES 3.1
class ShaderStorageBuffer(
    attributes: List<Attribute>,
    val elementCountLimit: Int
) : OpenGLBuffer(GL_SHADER_STORAGE_BUFFER, attributes) {

    override fun createNioBuffer() {
        val byteSize = elementCountLimit * attributes.sumOf { it.byteSize }
        val nio = ByteBufferPool.allocateDirect(byteSize)
        nioBuffer = nio
    }

}