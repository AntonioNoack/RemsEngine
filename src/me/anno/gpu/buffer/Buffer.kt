package me.anno.gpu.buffer

import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL33C.*

abstract class Buffer(attributes: List<Attribute>, usage: Int) :
    OpenGLBuffer(GL_ARRAY_BUFFER, attributes, usage), Drawable {

    constructor(attributes: List<Attribute>) : this(attributes, GL_STATIC_DRAW)

    var drawMode = GL_TRIANGLES
    var drawLength
        get() = elementCount
        set(value) {
            elementCount = value
        }

    @Deprecated("Not supported on mobile platforms nor modern OpenGL")
    fun quads(): Buffer {
        drawMode = GL_QUADS
        return this
    }

    fun lines(): Buffer {
        drawMode = GL_LINES
        return this
    }

    var vao = -1

    private fun ensureVAO() {
        if (useVAOs) {
            if (vao <= 0) vao = glGenVertexArrays()
            if (vao <= 0) throw IllegalStateException()
        }
    }

    override fun onSessionChange() {
        super.onSessionChange()
        vao = -1
    }

    private var hasWarned = false
    open fun createVAO(shader: Shader) {

        ensureBuffer()
        ensureVAO()

        bindVAO(vao)
        bindBuffer(GL_ARRAY_BUFFER, pointer)

        var hasAttr = false
        val attributes = attributes
        for (index in attributes.indices) {
            hasAttr = bindAttribute(shader, attributes[index], false) || hasAttr
        }
        if (!hasAttr && !hasWarned) {
            hasWarned = true
            LOGGER.warn("VAO does not have attribute!, $attributes, ${shader.vertexSource}")
        }

        // disable all attributes, which were not bound? no, not required

    }

    open fun createVAOInstanced(shader: Shader, instanceData: Buffer) {

        ensureVAO()
        ensureBuffer()

        bindVAO(vao)
        bindBuffer(GL_ARRAY_BUFFER, pointer)
        // first the instanced attributes, so the function can be called with super.createVAOInstanced without binding the buffer again
        val attributes = attributes
        for (attrIndex in attributes.indices) {
            bindAttribute(shader, attributes[attrIndex], false)
        }

        instanceData.ensureBuffer()
        bindBuffer(GL_ARRAY_BUFFER, instanceData.pointer)
        val attributes2 = instanceData.attributes
        for (attrIndex in attributes2.indices) {
            bindAttribute(shader, attributes2[attrIndex], true)
        }
    }

    private var lastShader: Shader? = null
    private fun bindBufferAttributes(shader: Shader) {
        GFX.check()
        shader.potentiallyUse()
        GFX.check()
        // todo cache vao by shader? typically, we only need 4-8 shaders for a single mesh
        // todo alternatively, we could specify the location in the shader
        if (vao <= 0 || shader !== lastShader || !useVAOs) createVAO(shader)
        else bindVAO(vao)
        lastShader = shader
        GFX.check()
    }

    private var baseAttributes: List<Attribute>? = null
    private var instanceAttributes: List<Attribute>? = null
    private fun bindBufferAttributesInstanced(shader: Shader, instanceData: Buffer) {
        GFX.check()
        shader.potentiallyUse()
        if (vao <= 0 ||
            attributes != baseAttributes ||
            instanceAttributes != instanceData.attributes ||
            shader !== lastShader || !useVAOs || renewVAOs
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
            unbind(shader)
            GFX.check()
        }
    }

    open fun unbind(shader: Shader) {
        bindBuffer(GL_ARRAY_BUFFER, 0)
        if (!useVAOs) {
            for (index in attributes.indices) {
                val attr = attributes[index]
                val loc = shader.getAttributeLocation(attr.name)
                if (loc >= 0) glDisableVertexAttribArray(loc)
            }
        }
        bindVAO(0)
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer) {
        drawInstanced(shader, instanceData, drawMode)
    }

    open fun drawInstanced(shader: Shader, instanceData: Buffer, mode: Int) {
        ensureBuffer()
        instanceData.ensureBuffer()
        bindInstanced(shader, instanceData)
        glDrawArraysInstanced(mode, 0, drawLength, instanceData.drawLength)
        unbind(shader)
    }

    fun bind(shader: Shader) {
        checkSession()
        if (!isUpToDate) upload()
        // else if (drawLength > 0) bindBuffer(GL15.GL_ARRAY_BUFFER, buffer)
        if (drawLength > 0) {
            bindBufferAttributes(shader)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer) {
        checkSession()
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
            unbind(shader)
            GFX.check()
        }
    }

    override fun destroy() {
        val buffer = pointer
        val vao = vao
        if (buffer > -1) {
            GFX.addGPUTask("Buffer.destroy()", 1) {
                onDestroyBuffer(buffer)
                glDeleteBuffers(buffer)
                if (vao >= 0) {
                    bindVAO(0)
                    glDeleteVertexArrays(vao)
                }
                locallyAllocated = allocate(locallyAllocated, 0L)
            }
        }
        this.pointer = -1
        this.vao = -1
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
        }
        nioBuffer = null
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Buffer::class)

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


    }

}