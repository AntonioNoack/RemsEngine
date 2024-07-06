package me.anno.gpu.buffer

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.withFlag
import me.anno.utils.types.Booleans.withoutFlag
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL46C.glVertexAttribDivisor
import org.lwjgl.opengl.GL46C.glVertexAttribIPointer
import org.lwjgl.opengl.GL46C.glVertexAttribPointer

abstract class Buffer(name: String, attributes: List<Attribute>, usage: BufferUsage) :
    OpenGLBuffer(name, GL_ARRAY_BUFFER, attributes, usage), Drawable {

    constructor(name: String, attributes: List<Attribute>) : this(name, attributes, BufferUsage.STATIC)

    var drawMode = DrawMode.TRIANGLES
    var drawLength
        get() = elementCount
        set(value) {
            elementCount = value
        }

    private var hasWarned = false
    open fun createVAO(shader: Shader, instanceData: Buffer? = null) {

        ensureBuffer()
        bindBuffer(type, pointer)
        GFX.check()

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
                GFX.check()
            }
        }
        GFX.check()
    }

    private fun bindBufferAttributes(shader: Shader) {
        shader.potentiallyUse()
        createVAO(shader)
    }

    private fun bindBufferAttributesInstanced(shader: Shader, instanceData: Buffer?) {
        shader.potentiallyUse()
        createVAO(shader, instanceData)
    }

    override fun draw(shader: Shader) = draw(shader, drawMode)
    open fun draw(shader: Shader, drawMode: DrawMode) {
        bind(shader) // defines drawLength
        if (drawLength > 0) {
            draw(drawMode, 0, drawLength)
            unbind(shader)
        }
    }

    open fun unbind(shader: Shader) {
        bindBuffer(GL46C.GL_ARRAY_BUFFER, 0)
        for (index in attributes.indices) {
            val attr = attributes[index]
            unbindAttribute(shader, attr.name)
        }
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer) {
        drawInstanced(shader, instanceData, drawMode)
    }

    open fun drawInstanced(shader: Shader, instanceData: Buffer, drawMode: DrawMode) {
        ensureBuffer()
        instanceData.ensureBuffer()
        bindInstanced(shader, instanceData)
        GFXState.bind()
        GL46C.glDrawArraysInstanced(drawMode.id, 0, drawLength, instanceData.drawLength)
        unbind(shader)
    }

    override fun drawInstanced(shader: Shader, instanceCount: Int) {
        ensureBuffer()
        bindInstanced(shader, null)
        GFXState.bind()
        GL46C.glDrawArraysInstanced(drawMode.id, 0, drawLength, instanceCount)
        unbind(shader)
    }

    fun bind(shader: Shader) {
        checkSession()
        if (!isUpToDate) upload()
        if (drawLength > 0) {
            bindBufferAttributes(shader)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer?) {
        checkSession()
        if (!isUpToDate) upload()
        // else if (drawLength > 0) bindBuffer(GL_ARRAY_BUFFER, buffer)
        if (drawLength > 0) {
            bindBufferAttributesInstanced(shader, instanceData)
        }
    }

    open fun draw(first: Int, length: Int) {
        draw(drawMode, first, length)
    }

    open fun draw(drawMode: DrawMode, first: Int, length: Int) {
        GFXState.bind()
        GL46C.glDrawArrays(drawMode.id, first, length)
    }

    override fun destroy() {
        if (Build.isDebug) DebugGPUStorage.buffers.remove(this)
        val buffer = pointer
        if (buffer > -1) {
            GFX.addGPUTask("Buffer.destroy()", 1) {
                onDestroyBuffer(buffer)
                GL46C.glDeleteBuffers(buffer)
                locallyAllocated = allocate(locallyAllocated, 0L)
            }
        }
        this.pointer = 0
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
            return if (index in 0 until GFX.maxAttributes) {
                GFX.check()
                val type = attr.type
                if (attr.isNativeInt) {
                    glVertexAttribIPointer(
                        index, attr.components, type.id,
                        attr.stride, attr.offset.toLong()
                    )
                } else {
                    glVertexAttribPointer(
                        index, attr.components, type.id,
                        type.normalized, attr.stride, attr.offset.toLong()
                    )
                }
                glVertexAttribDivisor(index, instanceDivisor)
                enable(index)
                GFX.check()
                true
            } else false
        }

        private fun enable(index: Int) {
            val flag = 1 shl index
            if (!enabledAttributes.hasFlag(flag)) {
                GL46C.glEnableVertexAttribArray(index)
                enabledAttributes = enabledAttributes.withFlag(flag)
            }
        }

        private fun disable(index: Int) {
            val flag = 1 shl index
            if (enabledAttributes.hasFlag(flag)) {
                GL46C.glDisableVertexAttribArray(index)
                enabledAttributes = enabledAttributes.withoutFlag(flag)
            }
        }

        @JvmStatic
        fun unbindAttribute(shader: Shader, attr: String) {
            val index = shader.getAttributeLocation(attr)
            if (index in 0 until GFX.maxAttributes) {
                disable(index)
                when (shader.attributes[index].type) {
                    GLSLType.V1B, GLSLType.V2B, GLSLType.V3B, GLSLType.V4B,
                    GLSLType.V1I, GLSLType.V2I, GLSLType.V3I, GLSLType.V4I -> GL46C.glVertexAttribI1i(index, 0)
                    else -> GL46C.glVertexAttrib1f(index, 0f)
                }
            }
        }
    }
}