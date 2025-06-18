package me.anno.gpu.buffer

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.utils.InternalAPI
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.withFlag
import me.anno.utils.types.Booleans.withoutFlag
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL46C.glVertexAttribDivisor
import org.lwjgl.opengl.GL46C.glVertexAttribIPointer
import org.lwjgl.opengl.GL46C.glVertexAttribPointer

abstract class Buffer(name: String, attributes: AttributeLayout, usage: BufferUsage) :
    OpenGLBuffer(name, GL_ARRAY_BUFFER, attributes, usage), Drawable {

    constructor(name: String, attributes: List<Attribute>) :
            this(name, bind(attributes), BufferUsage.STATIC)

    var drawMode = DrawMode.TRIANGLES
    var drawLength
        get() = elementCount
        set(value) {
            elementCount = value
        }

    private fun forceBind() {
        ensureBuffer()
        bindBuffer(type, pointer, true)
    }

    open fun createVAO(shader: Shader, instanceData: Buffer? = null) {
        bindAttributes(shader, false)
        instanceData?.bindAttributes(shader, true)
        unbindAttributes(shader, attributes, instanceData?.attributes)
    }

    @InternalAPI
    private fun unbindAttributes(
        shader: Shader, instancedAttributes: AttributeLayout,
        nonInstancedAttributes: AttributeLayout?
    ) {
        val declaredAttributes = shader.attributes
        for (i in declaredAttributes.indices) {
            val attr = declaredAttributes[i]
            // check if name is bound in attr1/attr2
            val attrName = attr.name
            if ((nonInstancedAttributes == null || nonInstancedAttributes.indexOf(attrName) < 0) &&
                instancedAttributes.indexOf(attrName) < 0
            ) {
                // disable attribute
                unbindAttribute(shader, attrName)
            }
        }
        GFX.check()
    }

    @InternalAPI
    fun bindAttributes(shader: Shader, instanced: Boolean) {
        forceBind()
        val attrs = attributes
        for (i in 0 until attrs.size) {
            bindAttribute(shader, attrs, i, instanced)
        }
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
        bindBuffer(GL_ARRAY_BUFFER, 0)
        val attributes = attributes
        for (i in 0 until attributes.size) {
            val attr = attributes
            unbindAttribute(shader, attr.name(i))
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
        val culling = Pipeline.currentInstance?.getOcclusionCulling()
        val clickIdAttr = if (culling != null) findClickIdAttr(instanceData) else -1
        if (culling != null && clickIdAttr >= 0) {
            culling.drawArraysInstanced(shader, instanceData, clickIdAttr, 0, drawLength, drawMode)
        } else {
            GL46C.glDrawArraysInstanced(drawMode.id, 0, drawLength, instanceData.drawLength)
        }
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
            shader.v1b("isIndexed", false)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer?) {
        checkSession()
        if (!isUpToDate) upload()
        // else if (drawLength > 0) bindBuffer(GL_ARRAY_BUFFER, buffer)
        if (drawLength > 0) {
            bindBufferAttributesInstanced(shader, instanceData)
            shader.v1b("isIndexed", false)
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
            addGPUTask("Buffer.destroy()", 1) {
                onDestroyBuffer(buffer)
                GL46C.glDeleteBuffers(buffer)
                locallyAllocated = allocate(locallyAllocated, 0L)
            }
        }
        pointer = 0
        isUpToDate = false
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
        }
        nioBuffer = null
    }

    companion object {

        private var enabledAttributes = 0

        // todo this doesn't really belong here, move it somewhere else
        fun findClickIdAttr(instanceData: Buffer): Int {
            return instanceData.attributes.indexOf("instanceFinalId")
        }

        @JvmStatic
        fun bindAttribute(shader: Shader, layout: AttributeLayout, i: Int, instanced: Boolean): Boolean {
            val instanceDivisor = if (instanced) 1 else 0
            val index = shader.getAttributeLocation(layout.name(i))
            return if (index in 0 until GFX.maxAttributes) {
                val type = layout.type(i)
                val numComponents = layout.components(i)
                val stride = layout.stride(i)
                val offset = layout.offset(i).toLong() and 0xffffffffL
                val typeId = type.glslId
                if (shader.isAttributeNative(index)) {
                    // defined as integer and to be used as integer
                    glVertexAttribIPointer(
                        index, numComponents, typeId,
                        stride, offset
                    )
                } else {
                    glVertexAttribPointer(
                        index, numComponents, typeId,
                        type.normalized, stride, offset
                    )
                }
                glVertexAttribDivisor(index, instanceDivisor)
                enable(index)
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