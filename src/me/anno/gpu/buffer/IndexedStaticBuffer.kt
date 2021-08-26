package me.anno.gpu.buffer

import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import org.lwjgl.opengl.GL11.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL31.glDrawElementsInstanced
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class IndexedStaticBuffer(
    attributes: List<Attribute>,
    vertexCount: Int,
    val indices: IntArray,
    usage: Int = GL_STATIC_DRAW
) : StaticBuffer(attributes, vertexCount, usage) {

    constructor(points: List<List<Float>>, attributes: List<Attribute>, vertices: IntArray) : this(
        attributes,
        points.size,
        vertices
    ) {
        for (point in points) {
            for (value in point) {
                put(value)
            }
        }
    }

    constructor(floats: FloatArray, attributes: List<Attribute>, indices: IntArray) : this(
        attributes,
        floats.size / attributes.sumOf { it.components },
        indices
    ) {
        put(floats)
    }

    var elementVBO = -1
    var elementsType = GL_UNSIGNED_INT

    init {
        createNioBuffer()
    }

    override fun createVAO(shader: Shader) {
        super.createVAO(shader)
        updateElementBuffer()
    }

    fun updateElementBuffer() {
        // GFX.check()
        // extra: element buffer
        val indices = indices
        if (indices.isEmpty()) return
        if (elementVBO < 0) elementVBO = glGenBuffers()
        bindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementVBO)
        // todo if size is the same as the old one, reuse it with glBufferSubData
        val maxIndex = indices.maxOrNull() ?: 0
        when {// optimize the size usage on the gpu side
            maxIndex < 256 -> {
                elementsType = GL_UNSIGNED_BYTE
                val buffer = ByteBuffer.allocateDirect(indices.size)
                for (i in indices) buffer.put(i.toByte())
                buffer.flip()
                GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer, usage)
            }
            maxIndex < 65536 -> {
                elementsType = GL_UNSIGNED_SHORT
                val buffer = MemoryUtil.memAllocShort(indices.size)
                for (i in indices) buffer.put(i.toShort())
                buffer.flip()
                GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer, usage)
            }
            else -> {
                elementsType = GL_UNSIGNED_INT
                GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indices, usage)
            }
        }
        // GFX.check()
    }

    override fun createVAOInstanced(shader: Shader, base: Buffer) {
        super.createVAOInstanced(shader, base)
        updateElementBuffer()
    }

    override fun draw(mode: Int, first: Int, length: Int) {
        glDrawElements(mode, indices.size, elementsType, 0)
    }

    override fun drawInstanced(shader: Shader, base: Buffer, mode: Int) {
        base.ensureBuffer()
        bindInstanced(shader, base)
        GL33.glDrawElementsInstanced(mode, indices.size, elementsType, 0, base.drawLength)
        // GL33.glDrawArraysInstanced(mode, 0, base.drawLength, drawLength)
        unbind()
    }

    override fun drawSimpleInstanced(shader: Shader, mode: Int, count: Int) {
        bind(shader) // defines drawLength
        if (drawLength > 0) {
            glDrawElementsInstanced(mode, indices.size, elementsType, 0, count)
            unbind()
            GFX.check()
        }
    }

    override fun destroy() {
        super.destroy()
        destroyIndexBuffer()
    }

    fun destroyIndexBuffer() {
        val buffer = elementVBO
        if (buffer >= 0) {
            GFX.addGPUTask(1) {
                onDestroyBuffer(buffer)
                glDeleteBuffers(buffer)
            }
        }
        elementVBO = -1
    }

    companion object {
        /*fun join(buffers: List<IndexedStaticBuffer>): IndexedStaticBuffer? {
            if (buffers.isEmpty()) return null
            val vertexCount = buffers.sumOf { it.vertexCount }
            val sample = buffers.first()
            val joint = IndexedStaticBuffer(sample.attributes, vertexCount)
            for (buffer in buffers) joint.put(buffer)
            return joint
        }*/
    }

}