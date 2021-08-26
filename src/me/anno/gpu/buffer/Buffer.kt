package me.anno.gpu.buffer

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import java.nio.ByteBuffer

abstract class Buffer(val attributes: List<Attribute>, val usage: Int) :
    ICacheData {

    // constructor(attributes: List<Attribute>, usage: Int) : this(attributes, usage)
    constructor(attributes: List<Attribute>) : this(attributes, GL15.GL_STATIC_DRAW)

    init {
        var offset = 0L
        val stride = attributes.sumOf { it.byteSize }
        attributes.forEach {
            it.offset = offset
            it.stride = stride
            offset += it.byteSize
        }
    }

    var drawMode = GL_TRIANGLES
    var nioBuffer: ByteBuffer? = null

    var drawLength = 0

    var buffer = -1
    var isUpToDate = false

    fun getName() = getName(0)
    fun getName(index: Int) = attributes[index].name

    var locallyAllocated = 0L

    fun upload() {

        GFX.check()

        if (buffer < 0) buffer = glGenBuffers()
        if (buffer < 0) throw IllegalStateException()

        bindBuffer(GL_ARRAY_BUFFER, buffer)

        if (nioBuffer == null) {
            createNioBuffer()
        }

        val nio = nioBuffer!!
        val stride = attributes.first().stride
        drawLength = nio.position() / stride
        nio.limit(nio.position())
        nio.position(0)
        val newLimit = nio.limit().toLong()
        if (locallyAllocated > 0 && newLimit in (locallyAllocated / 2)..locallyAllocated) {
            // just keep the buffer
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, nio)
        } else {
            locallyAllocated = allocate(locallyAllocated, newLimit)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, nio, usage)
        }

        GFX.check()
        isUpToDate = true

    }

    abstract fun createNioBuffer()

    fun quads(): Buffer {
        drawMode = GL11.GL_QUADS
        return this
    }

    fun lines(): Buffer {
        drawMode = GL11.GL_LINES
        return this
    }

    var vao = -1

    fun bindAttribute(shader: Shader, attr: Attribute, instanced: Boolean) {
        val instanceDivisor = if (instanced) 1 else 0
        val index = shader.getAttributeLocation(attr.name)
        if (index > -1) {
            val type = attr.type
            if (attr.isNativeInt) {
                glVertexAttribIPointer(index, attr.components, type.glType, attr.stride, attr.offset)
            } else {
                glVertexAttribPointer(index, attr.components, type.glType, type.normalized, attr.stride, attr.offset)
            }
            glVertexAttribDivisor(index, instanceDivisor)
            glEnableVertexAttribArray(index)
        }
    }

    fun ensureVAO() {
        if (useVAOs) {
            if (vao < 0) vao = glGenVertexArrays()
            if (vao < 0) throw IllegalStateException()
        }
    }

    open fun createVAO(shader: Shader) {

        ensureVAO()
        ensureBuffer()

        if (useVAOs) glBindVertexArray(vao)
        bindBuffer(GL_ARRAY_BUFFER, buffer)
        for (attr in attributes) {
            bindAttribute(shader, attr, false)
        }
        // todo disable all attributes, which were not bound
    }

    open fun createVAOInstanced(shader: Shader, base: Buffer) {

        ensureVAO()
        ensureBuffer()
        base.ensureBuffer()

        if (useVAOs) glBindVertexArray(vao)
        bindBuffer(GL_ARRAY_BUFFER, buffer)
        // first the instanced attributes, so the function can be called with super.createVAOInstanced without binding the buffer again
        for (attr in attributes) {
            bindAttribute(shader, attr, true)
        }
        bindBuffer(GL_ARRAY_BUFFER, base.buffer)
        for (attr in base.attributes) {
            bindAttribute(shader, attr, false)
        }
        // todo disable all attributes, which were not bound
    }

    fun bindBufferAttributes(shader: Shader) {
        GFX.check()
        shader.potentiallyUse()
        if (vao < 0 || !useVAOs) createVAO(shader)
        else glBindVertexArray(vao)
        GFX.check()
    }

    private var baseAttributes: List<Attribute>? = null
    fun bindBufferAttributesInstanced(shader: Shader, base: Buffer) {
        GFX.check()
        shader.potentiallyUse()
        if (vao < 0 || base.attributes != baseAttributes || !useVAOs) {
            baseAttributes = base.attributes
            createVAOInstanced(shader, base)
        } else glBindVertexArray(vao)
        GFX.check()
    }

    open fun draw(shader: Shader) = draw(shader, drawMode)
    open fun draw(shader: Shader, mode: Int) {
        bind(shader) // defines drawLength
        if (drawLength > 0) {
            draw(mode, 0, drawLength)
            unbind()
            GFX.check()
        }
    }

    open fun unbind() {
        // bindBuffer(GL_ARRAY_BUFFER, 0)
        if (useVAOs) glBindVertexArray(0)
    }

    fun ensureBuffer() {
        if (!isUpToDate) upload()
    }

    open fun drawInstanced(shader: Shader, base: Buffer) = drawInstanced(shader, base, base.drawMode)
    open fun drawInstanced(shader: Shader, base: Buffer, mode: Int) {
        base.ensureBuffer()
        bindInstanced(shader, base)
        glDrawArraysInstanced(mode, 0, base.drawLength, drawLength)
        unbind()
    }

    fun bind(shader: Shader) {
        if (!isUpToDate) upload()
        // else if (drawLength > 0) bindBuffer(GL15.GL_ARRAY_BUFFER, buffer)
        if (drawLength > 0) {
            bindBufferAttributes(shader)
        }
    }

    fun bindInstanced(shader: Shader, base: Buffer) {
        if (!isUpToDate) upload()
        // else if (drawLength > 0) bindBuffer(GL15.GL_ARRAY_BUFFER, buffer)
        if (drawLength > 0) {
            bindBufferAttributesInstanced(shader, base)
        }
    }

    open fun draw(first: Int, length: Int) {
        draw(drawMode, first, length)
    }

    open fun draw(mode: Int, first: Int, length: Int) {
        glDrawArrays(mode, first, length)
    }

    open fun drawSimpleInstanced(shader: Shader, mode: Int = drawMode, count: Int){
        bind(shader) // defines drawLength
        if (drawLength > 0) {
            glDrawArraysInstanced(mode, 0, drawLength, count)
            unbind()
            GFX.check()
        }
    }

    override fun destroy() {
        val buffer = buffer
        val vao = vao
        if (buffer > -1) {
            GFX.addGPUTask(1) {
                onDestroyBuffer(buffer)
                GL15.glDeleteBuffers(buffer)
                if (vao >= 0) glDeleteVertexArrays(vao)
                locallyAllocated = allocate(locallyAllocated, 0L)
            }
        }
        this.buffer = -1
        this.vao = -1
    }

    companion object {

        var boundBuffers = IntArray(2) { -1 }
        fun bindBuffer(slot: Int, buffer: Int) {
            val index = slot - GL_ARRAY_BUFFER
            if (index in boundBuffers.indices) {
                if (boundBuffers[index] != buffer) {
                    boundBuffers[index] = buffer
                    glBindBuffer(slot, buffer)
                }
            } else glBindBuffer(slot, buffer) // unknown buffer, probably used rarely anyways ^^
        }

        fun onDestroyBuffer(buffer: Int) {
            for (index in boundBuffers.indices) {
                if (buffer == boundBuffers[index]) {
                    boundBuffers[index] = -1
                    val slot = GL_ARRAY_BUFFER + index
                    glBindBuffer(slot, 0) // unbind
                }
            }
        }

        // todo cache bound vao to save opengl calls

        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }

        var useVAOs = false

    }

}