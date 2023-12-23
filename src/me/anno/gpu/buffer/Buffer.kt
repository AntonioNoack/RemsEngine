package me.anno.gpu.buffer

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.maths.Maths.hasFlag
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.structures.lists.Lists.none2
import org.lwjgl.opengl.GL33C.*

abstract class Buffer(name: String, attributes: List<Attribute>, usage: Int) :
    OpenGLBuffer(name, GL_ARRAY_BUFFER, attributes, usage), Drawable {

    constructor(name: String, attributes: List<Attribute>) : this(name, attributes, GL_STATIC_DRAW)

    var drawMode = DrawMode.TRIANGLES
    var drawLength
        get() = elementCount
        set(value) {
            elementCount = value
        }

    var vao = -1

    private fun ensureVAO() {
        if (useVAOs) {
            if (vao <= 0) vao = glGenVertexArrays()
        }
    }

    override fun onSessionChange() {
        super.onSessionChange()
        vao = -1
    }

    private var hasWarned = false
    open fun createVAO(shader: Shader, instanceData: Buffer? = null) {

        ensureVAO()
        ensureBuffer()

        bindVAO(vao)
        bindBuffer(type, pointer)

        // first the instanced attributes, so the function can be called with super.createVAOInstanced without binding the buffer again
        val attrs1 = attributes
        for (i in attrs1.indices) {
            bindAttribute(shader, attrs1[i], false)
        }

        val attr2 = instanceData?.attributes
        if (instanceData != null) {
            instanceData.ensureBuffer()
            bindBuffer(type, instanceData.pointer)
            for (i in attr2!!.indices) {
                bindAttribute(shader, attr2[i], true)
            }
        }

        val attrs2 = shader.attributes
        for (i in attrs2.indices) {
            val attr = attrs2[i]
            // check if name is bound in attr1/attr2
            val attrName = attr.name
            if (attrs1.none2 { it.name == attrName } && (attr2 == null || attr2.none2 { it.name == attrName })) {
                // disable attribute
                unbindAttribute(shader, attrName)
            }
        }
    }

    private var lastShader: Shader? = null
    private fun bindBufferAttributes(shader: Shader) {
        GFX.check()
        shader.potentiallyUse()
        GFX.check()
        // todo cache vao by shader? typically, we only need 4-8 shaders for a single mesh
        //  alternatively, we could specify the location in the shader
        if (vao <= 0 || shader !== lastShader || !useVAOs) {
            createVAO(shader)
        } else {
            bindVAO(vao)
        }
        lastShader = shader
        GFX.check()
    }

    private var baseAttributes: List<Attribute>? = null
    private var instanceAttributes: List<Attribute>? = null
    private fun bindBufferAttributesInstanced(shader: Shader, instanceData: Buffer?) {
        GFX.check()
        shader.potentiallyUse()
        if (vao <= 0 ||
            attributes != baseAttributes ||
            instanceAttributes != instanceData?.attributes ||
            shader !== lastShader || !useVAOs || renewVAOs
        ) {
            lastShader = shader
            baseAttributes = attributes
            instanceAttributes = instanceData?.attributes
            createVAO(shader, instanceData)
        } else {
            bindVAO(vao)
        }
        GFX.check()
    }

    override fun draw(shader: Shader) = draw(shader, drawMode)
    open fun draw(shader: Shader, drawMode: DrawMode) {
        bind(shader) // defines drawLength
        if (drawLength > 0) {
            draw(drawMode, 0, drawLength)
            unbind(shader)
            GFX.check()
        }
    }

    open fun unbind(shader: Shader) {
        bindBuffer(GL_ARRAY_BUFFER, 0)
        if (!useVAOs) {
            for (index in attributes.indices) {
                val attr = attributes[index]
                unbindAttribute(shader, attr.name)
            }
        }
        bindVAO(0)
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer) {
        drawInstanced(shader, instanceData, drawMode)
    }

    open fun drawInstanced(shader: Shader, instanceData: Buffer, drawMode: DrawMode) {
        ensureBuffer()
        instanceData.ensureBuffer()
        bindInstanced(shader, instanceData)
        GFXState.bind()
        glDrawArraysInstanced(drawMode.id, 0, drawLength, instanceData.drawLength)
        unbind(shader)
    }

    override fun drawInstanced(shader: Shader, instanceCount: Int) {
        ensureBuffer()
        bindInstanced(shader, null)
        GFXState.bind()
        glDrawArraysInstanced(drawMode.id, 0, drawLength, instanceCount)
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

    fun bindInstanced(shader: Shader, instanceData: Buffer?) {
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

    open fun draw(drawMode: DrawMode, first: Int, length: Int) {
        GFXState.bind()
        glDrawArrays(drawMode.id, first, length)
    }

    override fun destroy() {
        if (Build.isDebug) DebugGPUStorage.buffers.remove(this)
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
        this.pointer = 0
        this.vao = -1
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
        }
        nioBuffer = null
    }

    companion object {

        private var enabledAttributes = 0

        @JvmStatic
        fun bindAttribute(shader: Shader, attr: Attribute, instanced: Boolean): Boolean {
            val instanceDivisor = if (instanced) 1 else 0
            val index = shader.getAttributeLocation(attr.name)
            return if (index > -1) {
                val type = attr.type
                if (attr.isNativeInt) {
                    glVertexAttribIPointer(
                        index, attr.components, type.id,
                        attr.stride, attr.offset
                    )
                } else {
                    glVertexAttribPointer(
                        index, attr.components, type.id,
                        type.normalized, attr.stride, attr.offset
                    )
                }
                glVertexAttribDivisor(index, instanceDivisor)
                enable(index)
                true
            } else false
        }

        private fun enable(index: Int){
            if (useVAOs || !enabledAttributes.hasFlag(1 shl index)) {
                glEnableVertexAttribArray(index)
                enabledAttributes = enabledAttributes or (1 shl index)
            }
        }

        private fun disable(index: Int){
            if (useVAOs || enabledAttributes.hasFlag(1 shl index)) {
                glDisableVertexAttribArray(index)
                enabledAttributes = enabledAttributes and (1 shl index).inv()
            }
        }

        @JvmStatic
        fun unbindAttribute(shader: Shader, attr: String) {
            val index = shader.getAttributeLocation(attr)
            if (index > -1) {
                disable(index)
                when (shader.attributes[index].type) {
                    GLSLType.V1B, GLSLType.V2B, GLSLType.V3B, GLSLType.V4B,
                    GLSLType.V1I, GLSLType.V2I, GLSLType.V3I, GLSLType.V4I -> glVertexAttribI1i(index, 0)
                    else -> glVertexAttrib1f(index, 0f)
                }
            }
        }
    }
}