package me.anno.gpu.buffer

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.Buffer.Companion.findClickIdAttr
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import org.lwjgl.opengl.GL46C.GL_BUFFER
import org.lwjgl.opengl.GL46C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL46C.glBufferData
import org.lwjgl.opengl.GL46C.glBufferSubData
import org.lwjgl.opengl.GL46C.glDeleteBuffers
import org.lwjgl.opengl.GL46C.glDrawElements
import org.lwjgl.opengl.GL46C.glDrawElementsInstanced
import org.lwjgl.opengl.GL46C.glGenBuffers
import org.lwjgl.opengl.GL46C.glObjectLabel
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class IndexBuffer(name: String, val base: Buffer, indices: IntArray, usage: BufferUsage = BufferUsage.STATIC) :
    OpenGLBuffer(name, GL_ELEMENT_ARRAY_BUFFER, int32Attrs, usage), Drawable {

    var indices = indices
        set(value) {
            field = value
            isUpToDate = false
        }

    var elementsType = AttributeType.UINT32
    var drawMode: DrawMode? = null

    override fun createNioBuffer(): ByteBuffer {
        throw NotImplementedError("You are using this class wrong")
    }

    fun createVAO(shader: Shader, instanceData: Buffer? = null) {
        base.createVAO(shader, instanceData)
        updateElementBuffer()
    }

    override fun upload(allowResize: Boolean, keepLarge: Boolean) {
        updateElementBuffer()
    }

    private fun updateElementBuffer() {

        // GFX.check()
        // extra: element buffer

        val indices = indices
        if (indices.isEmpty()) return

        val target = GL_ELEMENT_ARRAY_BUFFER

        if (pointer == 0) pointer = glGenBuffers()
        if (pointer == 0) throw OutOfMemoryError("Could not generate OpenGL buffer")
        bindBuffer(target, pointer)

        if (isUpToDate) return
        isUpToDate = true

        val maxIndex = indices.maxOrNull() ?: 0
        when {// optimize the size usage on the gpu side
            // how do we find out, what is optimal?
            // we don't, we just assume (because it probably will be true), that 16 and 32 bits are always optimal
            // RX 580 Message:
            // glDrawElements uses element index type 'GL_UNSIGNED_BYTE' that is not optimal for the current hardware configuration;
            // consider using 'GL_UNSIGNED_SHORT' instead, source: API, type: PERFORMANCE, id: 102, severity: MEDIUM
            /*maxIndex < 256 -> {
                elementsType = GL_UNSIGNED_BYTE
                val buffer = ByteBufferPool.allocate(indices.size)
                for (i in indices) buffer.put(i.toByte())
                buffer.flip()
                GL46C.glBufferData(GL46C.GL_ELEMENT_ARRAY_BUFFER, buffer, usage)
            }*/
            maxIndex < 65536 -> {
                elementsType = AttributeType.UINT16
                attributes = int16Attrs
                val buffer = MemoryUtil.memAllocShort(indices.size)
                for (i in indices) buffer.put(i.toShort())
                buffer.flip()
                if (indices.size * 2L == locallyAllocated) {
                    glBufferSubData(target, 0, buffer)
                } else {
                    glBufferData(target, buffer, usage.id)
                }
                locallyAllocated = allocate(locallyAllocated, indices.size * 2L)
                MemoryUtil.memFree(buffer)
            }
            else -> {
                elementsType = AttributeType.UINT32
                attributes = int32Attrs
                if (indices.size * 4L == locallyAllocated) {
                    glBufferSubData(target, 0, indices)
                } else {
                    glBufferData(target, indices, usage.id)
                }
                locallyAllocated = allocate(locallyAllocated, indices.size * 4L)
            }
        }
        elementCount = indices.size
        if (Build.isDebug) {
            DebugGPUStorage.buffers.add(this)
            glObjectLabel(GL_BUFFER, pointer, name)
        }
        // GFX.check()
    }

    override fun draw(shader: Shader) = draw(shader, drawMode ?: base.drawMode)
    fun draw(shader: Shader, drawMode: DrawMode) {
        bind(shader) // defines drawLength
        if (base.drawLength > 0) {
            GFXState.bind()
            glDrawElements(drawMode.id, indices.size, elementsType.glslId, 0)
            unbind()
            GFX.check()
        }
    }

    private fun bindBufferAttributes(shader: Shader) {
        shader.potentiallyUse()
        checkSession()
        createVAO(shader)
    }

    private fun bindBufferAttributesInstanced(shader: Shader, instanceData: Buffer?) {
        shader.potentiallyUse()
        checkSession()
        createVAO(shader, instanceData)
    }

    fun bind(shader: Shader) {
        checkSession()
        if (!base.isUpToDate) base.upload()
        if (base.drawLength > 0) {
            bindBufferAttributes(shader)
            // it would be nice if we could remove debugging-related code from shipped builds...
            shader.v1b("isIndexed", true)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer?) {
        checkSession()
        if (!base.isUpToDate) base.upload()
        if (base.drawLength > 0) {
            bindBufferAttributesInstanced(shader, instanceData)
            shader.v1b("isIndexed", true)
        }
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer) {
        drawInstanced(shader, instanceData, drawMode ?: base.drawMode)
    }

    fun drawInstanced(shader: Shader, instanceData: Buffer, drawMode: DrawMode) {
        ensureBuffer()
        GFX.check()
        instanceData.ensureBuffer()
        bindInstanced(shader, instanceData)
        GFXState.bind()
        GFX.check()
        val culling = Pipeline.currentInstance?.getOcclusionCulling()
        val clickIdAttr = if (culling != null) findClickIdAttr(instanceData) else -1
        if (culling != null && clickIdAttr >= 0) {
            culling.drawElementsInstanced(shader, instanceData, clickIdAttr, 0, indices.size, drawMode, elementsType)
        } else {
            glDrawElementsInstanced(drawMode.id, indices.size, elementsType.glslId, 0, instanceData.drawLength)
        }
        GFX.check()
        unbind()
    }

    override fun drawInstanced(shader: Shader, instanceCount: Int) {
        ensureBuffer()
        bindInstanced(shader, null)
        val drawMode = (drawMode ?: base.drawMode)
        GFXState.bind()
        glDrawElementsInstanced(drawMode.id, indices.size, elementsType.glslId, 0, instanceCount)
        unbind()
    }

    override fun destroy() {
        if (Build.isDebug) DebugGPUStorage.buffers.remove(this)
        val buffer = pointer
        if (buffer > 0) {
            addGPUTask("IndexBuffer.destroy()", 1) {
                indices = i0
                elementCount = 0
                onDestroyBuffer(buffer)
                glDeleteBuffers(buffer)
            }
            pointer = 0
            locallyAllocated = allocate(locallyAllocated, 0)
        }
    }

    companion object {
        private val i0 = IntArray(0)
        private val int32Attrs = bind(Attribute("index", AttributeType.UINT32, 1))
        private val int16Attrs = bind(Attribute("index", AttributeType.UINT16, 1))
    }
}