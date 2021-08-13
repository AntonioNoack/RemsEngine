package me.anno.gpu.buffer

import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import org.lwjgl.opengl.GL11.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33

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

    init {
        createNioBuffer()
    }

    override fun createVAO(shader: Shader, instanced: Boolean) {
        super.createVAO(shader, instanced)

        // GFX.check()

        // extra: element buffer
        if (elementVBO < 0) elementVBO = glGenBuffers()
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, elementVBO)
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indices, usage)

        // GFX.check()

    }

    override fun draw(mode: Int, first: Int, length: Int) {
        glDrawElements(mode, indices.size, GL_UNSIGNED_INT, 0)
    }

    override fun drawInstanced(shader: Shader, base: Buffer, mode: Int) {
        base.bind(shader)
        bindInstanced(shader)
        GL33.glDrawElementsInstanced(mode, indices.size, GL_UNSIGNED_INT, 0, base.drawLength)
        // GL33.glDrawArraysInstanced(mode, 0, base.drawLength, drawLength)
        unbind()
    }

    override fun destroy() {
        super.destroy()
        destroyIndexBuffer()
    }

    fun destroyIndexBuffer() {
        if (elementVBO >= 0) {
            val buffer = elementVBO
            GFX.addGPUTask(1) {
                glDeleteBuffers(buffer)
            }
            elementVBO = -1
        }
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