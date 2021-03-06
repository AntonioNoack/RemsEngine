package me.anno.gpu.buffer

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.shader.Shader
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL31C.*
import org.lwjgl.system.MemoryUtil

// todo probably should be an OpenGLBuffer
class IndexBuffer(
    val base: Buffer,
    indices: IntArray,
    val usage: Int = GL_STATIC_DRAW
) : ICacheData, Drawable {

    var indices: IntArray = indices
        set(value) {
            if (field !== value) {
                field = value
                invalidate()
            }
        }

    var pointer = -1
    var elementsType = GL_UNSIGNED_INT
    var isUpToDate = false
    var session = 0
    var locallyAllocated = 0L

    var drawMode = -1

    private var hasWarned = false

    private var vao = -1

    fun checkSession() {
        if (session != OpenGL.session) {
            session = OpenGL.session
            pointer = -1
            isUpToDate = false
            locallyAllocated = OpenGLBuffer.allocate(locallyAllocated, 0L)
            vao = -1
        }
    }

    private fun ensureVAO() {
        if (OpenGLBuffer.useVAOs) {
            if (vao <= 0) vao = glGenVertexArrays()
            if (vao <= 0) throw OutOfMemoryError("Could not allocate vertex array")
        }
    }

    fun createVAO(shader: Shader) {

        base.ensureBuffer()

        ensureVAO()

        OpenGLBuffer.bindVAO(vao)
        OpenGLBuffer.bindBuffer(GL_ARRAY_BUFFER, base.pointer)
        var hasAttr = false
        val attributes = base.attributes
        for (index in attributes.indices) {
            hasAttr = Buffer.bindAttribute(shader, attributes[index], false) || hasAttr
        }
        if (!hasAttr && !hasWarned) {
            hasWarned = true
            LOGGER.warn("VAO does not have attribute!, ${base.attributes}, ${shader.vertexSource}")
        }

        // disable all attributes, which were not bound
        // not required
        updateElementBuffer()

    }

    fun createVAOInstanced(shader: Shader, instanceData: Buffer) {

        ensureVAO()
        base.ensureBuffer()

        OpenGLBuffer.bindVAO(vao)
        OpenGLBuffer.bindBuffer(GL_ARRAY_BUFFER, base.pointer)
        // first the instanced attributes, so the function can be called with super.createVAOInstanced without binding the buffer again
        for (attr in base.attributes) {
            Buffer.bindAttribute(shader, attr, false)
        }

        instanceData.ensureBuffer()
        OpenGLBuffer.bindBuffer(GL_ARRAY_BUFFER, instanceData.pointer)
        for (attr in instanceData.attributes) {
            Buffer.bindAttribute(shader, attr, true)
        }

        updateElementBuffer()
    }

    private fun updateElementBuffer() {

        // GFX.check()
        // extra: element buffer

        val indices = indices
        if (indices.isEmpty()) return

        val target = GL_ELEMENT_ARRAY_BUFFER

        if (pointer <= 0) pointer = glGenBuffers()
        if (pointer <= 0) throw OutOfMemoryError("Could not generate OpenGL buffer")
        OpenGLBuffer.bindBuffer(target, pointer)

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
                GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer, usage)
            }*/
            maxIndex < 65536 -> {
                elementsType = GL_UNSIGNED_SHORT
                val buffer = MemoryUtil.memAllocShort(indices.size)
                for (i in indices) buffer.put(i.toShort())
                buffer.flip()
                if (indices.size * 2L == locallyAllocated) {
                    glBufferSubData(target, 0, buffer)
                } else {
                    glBufferData(target, buffer, usage)
                }
                locallyAllocated = OpenGLBuffer.allocate(locallyAllocated, indices.size * 2L)
                MemoryUtil.memFree(buffer)
            }
            else -> {
                elementsType = GL_UNSIGNED_INT
                if (indices.size * 4L == locallyAllocated) {
                    glBufferSubData(target, 0, indices)
                } else {
                    glBufferData(target, indices, usage)
                }
                locallyAllocated = OpenGLBuffer.allocate(locallyAllocated, indices.size * 4L)
            }
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
        if (vao <= 0 || shader !== lastShader || !OpenGLBuffer.useVAOs) {
            createVAO(shader)
        } else OpenGLBuffer.bindVAO(vao)
        lastShader = shader
        GFX.check()
    }

    private var baseAttributes: List<Attribute>? = null
    private var instanceAttributes: List<Attribute>? = null
    private var lastInstanceBuffer: Buffer? = null
    private fun bindBufferAttributesInstanced(shader: Shader, instanceData: Buffer) {
        GFX.check()
        shader.potentiallyUse()
        checkSession()
        if (vao <= 0 ||
            lastInstanceBuffer !== instanceData ||
            shader !== lastShader ||
            base.attributes != baseAttributes ||
            instanceAttributes != instanceData.attributes ||
            !OpenGLBuffer.useVAOs || OpenGLBuffer.renewVAOs
        ) {
            lastShader = shader
            baseAttributes = base.attributes
            instanceAttributes = instanceData.attributes
            lastInstanceBuffer = instanceData
            createVAOInstanced(shader, instanceData)
        } else OpenGLBuffer.bindVAO(vao)
        GFX.check()
    }

    fun bind(shader: Shader) {
        checkSession()
        if (!base.isUpToDate) base.upload()
        if (base.drawLength > 0) {
            bindBufferAttributes(shader)
        }
    }

    fun bindInstanced(shader: Shader, instanceData: Buffer) {
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

    fun drawSimpleInstanced(shader: Shader, mode: Int, count: Int) {
        bind(shader) // defines drawLength
        if (base.drawLength > 0) {
            glDrawElementsInstanced(mode, indices.size, elementsType, 0, count)
            unbind()
            GFX.check()
        }
    }

    fun unbind() {
        // bindBuffer(GL_ARRAY_BUFFER, 0)
        // bindVAO(0)
    }

    override fun destroy() {
        val buffer = pointer
        if (buffer >= 0) {
            GFX.addGPUTask("IndexBuffer.destroy()", 1) {
                OpenGLBuffer.onDestroyBuffer(buffer)
                glDeleteBuffers(buffer)
            }
            pointer = -1
            locallyAllocated = OpenGLBuffer.allocate(locallyAllocated, 0)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(IndexBuffer::class)
    }

}