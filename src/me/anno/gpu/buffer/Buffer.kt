package me.anno.gpu.buffer

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.utils.LOGGER
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import java.nio.ByteBuffer

abstract class Buffer(val attributes: List<Attribute>, val usage: Int) :
    ICacheData, Drawable {

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

    fun upload(allowResize: Boolean = true) {

        GFX.check()

        if (nioBuffer == null) {
            createNioBuffer()
        }

        if (buffer <= 0) buffer = glGenBuffers()
        if (buffer <= 0) throw IllegalStateException()

        bindBuffer(GL_ARRAY_BUFFER, buffer)

        val nio = nioBuffer!!
        val stride = attributes.first().stride
        val newLimit = nio.position().toLong()
        drawLength = (newLimit / stride).toInt()
        nio.flip()
        val minAcceptedSize = if (allowResize) locallyAllocated / 2 else 0
        if (locallyAllocated > 0 && newLimit in minAcceptedSize..locallyAllocated) {
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

    private var vao = -1

    private fun ensureVAO() {
        if (vao <= 0) vao = glGenVertexArrays()
        if (vao <= 0) throw IllegalStateException()
    }

    private var hasWarned = false
    open fun createVAO(shader: Shader) {

        ensureBuffer()

        ensureVAO()

        bindVAO(vao)
        bindBuffer(GL_ARRAY_BUFFER, buffer)
        var hasAttr = false
        val attributes = attributes
        for (index in attributes.indices) {
            hasAttr = bindAttribute(shader, attributes[index], false) || hasAttr
        }
        if (!hasAttr && !hasWarned) {
            hasWarned = true
            LOGGER.warn("VAO does not have attribute!, $attributes, ${shader.vertexSource}")
        }
        // todo disable all attributes, which were not bound
    }

    open fun createVAOInstanced(shader: Shader, instanceData: Buffer) {

        ensureVAO()
        ensureBuffer()

        bindVAO(vao)
        bindBuffer(GL_ARRAY_BUFFER, buffer)
        // first the instanced attributes, so the function can be called with super.createVAOInstanced without binding the buffer again
        for (attr in attributes) {
            bindAttribute(shader, attr, false)
        }

        instanceData.ensureBuffer()
        bindBuffer(GL_ARRAY_BUFFER, instanceData.buffer)
        for (attr in instanceData.attributes) {
            bindAttribute(shader, attr, true)
        }
    }

    private var lastShader: Shader? = null
    private fun bindBufferAttributes(shader: Shader) {
        GFX.check()
        shader.potentiallyUse()
        // todo cache vao by shader? typically, we only need 4 shaders for a single mesh
        // todo alternatively, we could specify the location in the shader
        if (vao <= 0 || shader !== lastShader) createVAO(shader)
        else bindVAO(vao)
        lastShader = shader
        GFX.check()
    }

    private var baseAttributes: List<Attribute>? = null
    private var instanceAttributes: List<Attribute>? = null
    private fun bindBufferAttributesInstanced(shader: Shader, instanceData: Buffer) {
        GFX.check()
        shader.potentiallyUse()
        if (vao < 0 ||
            attributes != baseAttributes ||
            instanceAttributes != instanceData.attributes ||
            shader !== lastShader
        ) {
            lastShader = shader
            baseAttributes = attributes
            instanceAttributes = instanceData.attributes
            createVAOInstanced(shader, instanceData)
        } else bindVAO(vao)
        GFX.check()
    }

    override fun draw(shader: Shader) = draw(shader, drawMode)
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
        // bindVAO(0)
    }

    fun ensureBuffer() {
        if (!isUpToDate) upload()
    }

    fun ensureBufferWithoutResize() {
        if (!isUpToDate) upload(false)
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer) {
        drawInstanced(shader, instanceData, drawMode)
    }

    open fun drawInstanced(shader: Shader, instanceData: Buffer, mode: Int) {
        ensureBuffer()
        instanceData.ensureBuffer()
        bindInstanced(shader, instanceData)
        glDrawArraysInstanced(mode, 0, drawLength, instanceData.drawLength)
        unbind()
    }

    fun bind(shader: Shader) {
        if (!isUpToDate) upload()
        // else if (drawLength > 0) bindBuffer(GL15.GL_ARRAY_BUFFER, buffer)
        if (drawLength > 0) {
            bindBufferAttributes(shader)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer) {
        if (!isUpToDate) upload()
        // else if (drawLength > 0) bindBuffer(GL15.GL_ARRAY_BUFFER, buffer)
        if (drawLength > 0) {
            bindBufferAttributesInstanced(shader, instanceData)
        }
    }

    open fun draw(first: Int, length: Int) {
        draw(drawMode, first, length)
    }

    open fun draw(mode: Int, first: Int, length: Int) {
        glDrawArrays(mode, first, length)
    }

    open fun drawSimpleInstanced(shader: Shader, mode: Int = drawMode, count: Int) {
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
                if (vao >= 0) {
                    bindVAO(0)
                    glDeleteVertexArrays(vao)
                }
                locallyAllocated = allocate(locallyAllocated, 0L)
            }
        }
        this.buffer = -1
        this.vao = -1
        if (nioBuffer != null) {
            // todo this crashes... why???
            // MemoryUtil.memFree(nioBuffer)
        }
        nioBuffer = null
    }

    companion object {

        fun bindAttribute(shader: Shader, attr: Attribute, instanced: Boolean): Boolean {
            val instanceDivisor = if (instanced) 1 else 0
            val index = shader.getAttributeLocation(attr.name)
            return if (index > -1) {
                val type = attr.type
                if (attr.isNativeInt) {
                    glVertexAttribIPointer(index, attr.components, type.glType, attr.stride, attr.offset)
                } else {
                    glVertexAttribPointer(
                        index,
                        attr.components,
                        type.glType,
                        type.normalized,
                        attr.stride,
                        attr.offset
                    )
                }
                glVertexAttribDivisor(index, instanceDivisor)
                glEnableVertexAttribArray(index)
                true
            } else false
        }

        var boundVAO = -1
        fun bindVAO(vao: Int) {
            if (boundVAO != vao) {
                boundVAO = vao
                glBindVertexArray(vao)
            }
        }

        var boundBuffers = IntArray(2) { 0 }
        fun bindBuffer(slot: Int, buffer: Int, force: Boolean = false) {
            val index = slot - GL_ARRAY_BUFFER
            if (index in boundBuffers.indices) {
                if (boundBuffers[index] != buffer || force) {
                    if (buffer < 0) throw IllegalArgumentException("Buffer is invalid!")
                    boundBuffers[index] = buffer
                    glBindBuffer(slot, buffer)
                }
            } else glBindBuffer(slot, buffer) // unknown buffer, probably used rarely anyways ^^
        }

        fun onDestroyBuffer(buffer: Int) {
            for (index in boundBuffers.indices) {
                if (buffer == boundBuffers[index]) {
                    val slot = index + GL_ARRAY_BUFFER
                    bindBuffer(slot, 0)
                }
            }
        }

        fun invalidateBinding() {
            boundBuffers.fill(0)
            boundVAO = -1
        }

        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }

    }

}