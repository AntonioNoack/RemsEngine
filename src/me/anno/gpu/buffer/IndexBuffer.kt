package me.anno.gpu.buffer

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.shader.Shader
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL31C.*
import org.lwjgl.opengl.GL43C.GL_BUFFER
import org.lwjgl.opengl.GL43C.glObjectLabel
import org.lwjgl.system.MemoryUtil

class IndexBuffer(name: String, val base: Buffer, indices: IntArray, usage: Int = GL_STATIC_DRAW) :
    OpenGLBuffer(name, GL_ELEMENT_ARRAY_BUFFER, int32Attrs, usage), Drawable {

    var indices: IntArray = indices
        set(value) {
            if (field !== value) {
                field = value
                invalidate()
            }
        }

    var elementsType = GL_UNSIGNED_INT
    var drawMode = -1

    private var hasWarned = false

    private var vao = -1

    override fun createNioBuffer() {
        throw NotImplementedError("You are using this class wrong")
    }

    override fun onSessionChange() {
        super.onSessionChange()
        vao = -1
    }

    private fun ensureVAO() {
        if (useVAOs) {
            if (vao <= 0) vao = glGenVertexArrays()
            if (vao <= 0) throw OutOfMemoryError("Could not allocate vertex array")
        }
    }

    fun createVAO(shader: Shader, instanceData: Buffer? = null) {
        base.createVAO(shader, instanceData)
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
                GL30C.glBufferData(GL30C.GL_ELEMENT_ARRAY_BUFFER, buffer, usage)
            }*/
            maxIndex < 65536 -> {
                elementsType = GL_UNSIGNED_SHORT
                attributes = int16Attrs
                val buffer = MemoryUtil.memAllocShort(indices.size)
                for (i in indices) buffer.put(i.toShort())
                buffer.flip()
                if (indices.size * 2L == locallyAllocated) {
                    glBufferSubData(target, 0, buffer)
                } else {
                    glBufferData(target, buffer, usage)
                }
                locallyAllocated = allocate(locallyAllocated, indices.size * 2L)
                MemoryUtil.memFree(buffer)
            }
            else -> {
                elementsType = GL_UNSIGNED_INT
                attributes = int32Attrs
                if (indices.size * 4L == locallyAllocated) {
                    glBufferSubData(target, 0, indices)
                } else {
                    glBufferData(target, indices, usage)
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

    override fun draw(shader: Shader) = draw(shader, if (drawMode < 0) base.drawMode else drawMode)
    fun draw(shader: Shader, mode: Int) {
        bind(shader) // defines drawLength
        if (base.drawLength > 0) {
            glDrawElements(mode, indices.size, elementsType, 0)
            unbind()
            GFX.check()
        }
    }

    private fun invalidate() {
        lastShader = null
    }

    private var lastShader: Shader? = null
    private fun bindBufferAttributes(shader: Shader) {
        GFX.check()
        shader.potentiallyUse()
        checkSession()
        // todo cache vao by shader? typically, we only need 4 shaders for a single mesh
        // todo alternatively, we could specify the location in the shader
        if (vao <= 0 || shader !== lastShader || !useVAOs) {
            createVAO(shader)
        } else bindVAO(vao)
        lastShader = shader
        GFX.check()
    }

    private var baseAttributes: List<Attribute>? = null
    private var instanceAttributes: List<Attribute>? = null
    private var lastInstanceBuffer: Buffer? = null
    private fun bindBufferAttributesInstanced(shader: Shader, instanceData: Buffer?) {
        GFX.check()
        shader.potentiallyUse()
        checkSession()
        if (vao <= 0 ||
            lastInstanceBuffer !== instanceData ||
            shader !== lastShader ||
            base.attributes != baseAttributes ||
            instanceAttributes != instanceData?.attributes ||
            !useVAOs || renewVAOs
        ) {
            lastShader = shader
            baseAttributes = base.attributes
            instanceAttributes = instanceData?.attributes
            lastInstanceBuffer = instanceData
            createVAO(shader, instanceData)
        } else {
            bindVAO(vao)
        }
        GFX.check()
    }

    fun bind(shader: Shader) {
        checkSession()
        if (!base.isUpToDate) base.upload()
        if (base.drawLength > 0) {
            bindBufferAttributes(shader)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer?) {
        checkSession()
        if (!base.isUpToDate) base.upload()
        if (base.drawLength > 0) {
            bindBufferAttributesInstanced(shader, instanceData)
        }
    }

    override fun drawInstanced(shader: Shader, instanceData: Buffer) {
        drawInstanced(shader, instanceData, if (drawMode < 0) base.drawMode else drawMode)
    }

    fun drawInstanced(shader: Shader, instanceData: Buffer, mode: Int) {
        instanceData.ensureBuffer()
        bindInstanced(shader, instanceData)
        glDrawElementsInstanced(mode, indices.size, elementsType, 0, instanceData.drawLength)
        unbind()
    }

    override fun drawInstanced(shader: Shader, instanceCount: Int) {
        bindInstanced(shader, null)
        glDrawElementsInstanced(
            if (drawMode < 0) base.drawMode else drawMode,
            indices.size,
            elementsType,
            0,
            instanceCount
        )
        unbind()
    }

    @Suppress("unused")
    fun drawSimpleInstanced(shader: Shader, mode: Int, count: Int) {
        bind(shader) // defines drawLength
        if (base.drawLength > 0) {
            glDrawElementsInstanced(mode, indices.size, elementsType, 0, count)
            unbind()
            GFX.check()
        }
    }

    /*override fun unbind() {
        super.unbind()
        // bindVAO(0)
    }*/

    override fun destroy() {
        if (Build.isDebug) DebugGPUStorage.buffers.remove(this)
        val buffer = pointer
        if (buffer >= 0) {
            GFX.addGPUTask("IndexBuffer.destroy()", 1) {
                elementCount = 0
                onDestroyBuffer(buffer)
                glDeleteBuffers(buffer)
            }
            pointer = 0
            locallyAllocated = allocate(locallyAllocated, 0)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(IndexBuffer::class)
        private val int32Attrs = listOf(Attribute("index", AttributeType.UINT32, 1))
        private val int16Attrs = listOf(Attribute("index", AttributeType.UINT16, 1))
    }

}