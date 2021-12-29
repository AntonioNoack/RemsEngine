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

/**
 * indexed rendering from a time, when we only needed 1 indexing buffer at maximum per geometry data
 * the times have changes, so now we use multiple IndexBuffers with one Buffer
 * */
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
    var isUpToDate2 = false
    var locallyAllocated2 = 0L

    override fun onSessionChange() {
        super.onSessionChange()
        elementVBO = -1
        isUpToDate2 = false
        locallyAllocated2 = allocate(locallyAllocated2, 0L)
    }

    init {
        createNioBuffer()
    }

    override fun createVAO(shader: Shader) {
        super.createVAO(shader)
        updateElementBuffer()
    }

    override fun createVAOInstanced(shader: Shader, instanceData: Buffer) {
        super.createVAOInstanced(shader, instanceData)
        updateElementBuffer()
    }

    private fun updateElementBuffer() {

        // GFX.check()
        // extra: element buffer

        val indices = indices
        if (indices.isEmpty()) return

        if (elementVBO <= 0) elementVBO = glGenBuffers()
        if (elementVBO <= 0) throw OutOfMemoryError("Failed to generate OpenGL buffer")
        bindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementVBO)

        if (isUpToDate2) return
        isUpToDate2 = true

        val maxIndex = indices.maxOrNull() ?: 0
        when {
            // optimize the size usage on the gpu side
            // how do we find out, what is optimal?
            // TheCherno discord server member said that both u16 and u32 should be optimal :)
            // RX 580 Message:
            // glDrawElements uses element index type 'GL_UNSIGNED_BYTE' that is not optimal for the current hardware configuration;
            // consider using 'GL_UNSIGNED_SHORT' instead, source: API, type: PERFORMANCE, id: 102, severity: MEDIUM
            /*maxIndex < 256 -> {
                elementsType = GL_UNSIGNED_BYTE
                val buffer = ByteBufferPool.allocate(indices.size)
                for (i in indices) buffer.put(i.toByte())
                buffer.flip()
                GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer, usage)
            }*/
            maxIndex < 65536 -> {
                elementsType = GL_UNSIGNED_SHORT
                val buffer = MemoryUtil.memAllocShort(indices.size)
                for (i in indices) buffer.put(i.toShort())
                buffer.flip()
                if (indices.size * 2L == locallyAllocated2) {
                    GL30.glBufferSubData(GL30.GL_ELEMENT_ARRAY_BUFFER, 0, buffer)
                } else {
                    GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer, usage)
                }
                locallyAllocated2 = allocate(locallyAllocated2, indices.size * 2L)
                MemoryUtil.memFree(buffer)
            }
            else -> {
                elementsType = GL_UNSIGNED_INT
                if (indices.size * 4L == locallyAllocated2) {
                    GL30.glBufferSubData(GL30.GL_ELEMENT_ARRAY_BUFFER, 0, indices)
                } else {
                    GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indices, usage)
                }
                locallyAllocated2 = allocate(locallyAllocated2, indices.size * 4L)
            }
        }
        // GFX.check()
    }

    override fun draw(mode: Int, first: Int, length: Int) {
        checkSession()
        glDrawElements(mode, indices.size, elementsType, 0)
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer) {
        drawInstanced(shader, instanceData, drawMode)
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer, mode: Int) {
        checkSession()
        instanceData.ensureBuffer()
        bindInstanced(shader, instanceData)
        GL33.glDrawElementsInstanced(mode, indices.size, elementsType, 0, instanceData.drawLength)
        // GL33.glDrawArraysInstanced(mode, 0, base.drawLength, drawLength)
        unbind(shader)
    }

    override fun drawSimpleInstanced(shader: Shader, mode: Int, count: Int) {
        bind(shader) // defines drawLength
        if (drawLength > 0) {
            glDrawElementsInstanced(mode, indices.size, elementsType, 0, count)
            unbind(shader)
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

}