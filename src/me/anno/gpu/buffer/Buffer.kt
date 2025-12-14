package me.anno.gpu.buffer

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.utils.InternalAPI
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_ARRAY_BUFFER

abstract class Buffer(name: String, attributes: AttributeLayout, usage: BufferUsage) :
    GPUBuffer(name, GL_ARRAY_BUFFER, attributes, usage), Drawable {

    constructor(name: String, attributes: List<Attribute>) :
            this(name, bind(attributes), BufferUsage.STATIC)

    var drawMode = DrawMode.TRIANGLES
    var drawLength
        get() = elementCount
        set(value) {
            elementCount = value
        }

    open fun createVAO(shader: Shader, instanceData: Buffer? = null) {
        BufferState.unbindAll()
        bindAttributes(shader, false)
        instanceData?.bindAttributes(shader, true)
        BufferState.bindSetState(shader)
    }

    @InternalAPI
    fun bindAttributes(shader: Shader, instanced: Boolean) {
        ensureBuffer()
        val attrs = attributes
        val divisor = if (instanced) 1 else 0
        for (i in 0 until attrs.size) {
            val shaderId = shader.getAttributeLocation(attrs.name(i))
            if (shaderId >= 0) BufferState.bind(this, shaderId, divisor, attrs, i)
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
        /*bindBuffer(GL_ARRAY_BUFFER, 0)
        val attributes = attributes
        for (i in 0 until attributes.size) {
            val attr = attributes
            disableMissingAttribute(shader, attr.name(i))
        }*/
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

    fun ensureExists(): Boolean {
        checkSession()
        if (!isUpToDate) upload()
        return drawLength > 0
    }

    fun bind(shader: Shader) {
        if (ensureExists()) {
            bindBufferAttributes(shader)
            shader.v1b("isIndexed", false)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer?) {
        if (ensureExists()) {
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
            if (GFX.isGFXThread()) doDestroy(buffer)
            else addGPUTask("Buffer.destroy()", 1) { doDestroy(buffer) }
        }
        pointer = INVALID_POINTER
        cpuSideChanged()
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
        }
        nioBuffer = null
    }

    private fun doDestroy(buffer: Int) {
        onDestroyBuffer(buffer)
        GL46C.glDeleteBuffers(buffer)
        locallyAllocated = allocate(locallyAllocated, 0L)
    }

    companion object {
        // todo this doesn't really belong here, move it somewhere else
        fun findClickIdAttr(instanceData: Buffer): Int {
            return instanceData.attributes.indexOf("instanceFinalId")
        }
    }
}