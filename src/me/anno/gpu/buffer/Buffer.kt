package me.anno.gpu.buffer

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.buffer.Attribute.Companion.computeOffsets
import me.anno.gpu.shader.Shader
import me.anno.input.Input
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import java.nio.ByteBuffer
import kotlin.math.max

abstract class Buffer(val attributes: List<Attribute>, val usage: Int) :
    ICacheData, Drawable {

    constructor(attributes: List<Attribute>) : this(attributes, GL15.GL_STATIC_DRAW)

    val stride = computeOffsets(attributes)

    var drawMode = GL_TRIANGLES
    var nioBuffer: ByteBuffer? = null

    var drawLength = 0

    var pointer = -1
    var session = 0

    var isUpToDate = false

    fun getName() = getName(0)
    fun getName(index: Int) = attributes[index].name

    var locallyAllocated = 0L

    fun checkSession() {
        if (session != OpenGL.session) {
            session = OpenGL.session
            onSessionChange()
        }
    }

    open fun onSessionChange() {
        pointer = -1
        isUpToDate = false
        locallyAllocated = allocate(locallyAllocated, 0L)
        vao = -1
    }

    fun upload(allowResize: Boolean = true) {

        checkSession()

        GFX.check()

        if (nioBuffer == null) {
            createNioBuffer()
        }

        if (pointer <= 0) pointer = glGenBuffers()
        if (pointer <= 0) throw OutOfMemoryError("Could not generate OpenGL Buffer")

        bindBuffer(GL_ARRAY_BUFFER, pointer)

        val nio = nioBuffer!!
        val newLimit = nio.position()
        drawLength = max(drawLength, newLimit / stride)
        nio.position(0)
        nio.limit(drawLength * stride)
        if (allowResize && locallyAllocated > 0 && newLimit in locallyAllocated / 2..locallyAllocated) {
            // just keep the buffer
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, nio)
        } else {
            locallyAllocated = allocate(locallyAllocated, newLimit.toLong())
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, nio, usage)
        }

        GFX.check()
        isUpToDate = true

    }

    abstract fun createNioBuffer()

    @Deprecated("Not supported on mobile platforms nor modern OpenGL")
    fun quads(): Buffer {
        drawMode = GL11C.GL_QUADS
        return this
    }

    fun lines(): Buffer {
        drawMode = GL11C.GL_LINES
        return this
    }

    private var vao = -1

    private fun ensureVAO() {
        if (useVAOs) {
            if (vao <= 0) vao = glGenVertexArrays()
            if (vao <= 0) throw IllegalStateException()
        }
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
        bindVAO(0)
        if (!useVAOs) {
            for (index in attributes.indices) {
                val attr = attributes[index]
                val loc = shader.getAttributeLocation(attr.name)
                if (loc >= 0) glDisableVertexAttribArray(loc)
            }
        }
    }

    fun ensureBuffer() {
        checkSession()
        if (!isUpToDate) upload()
    }

    fun ensureBufferWithoutResize() {
        checkSession()
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
        this.pointer = -1
        this.vao = -1
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
        }
        nioBuffer = null
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Buffer::class)

        // monkey & stuff is invisible with vaos
        // because VAOs need default values (probably)

        // todo change back to use it constantly, or to be configurable
        var useVAOs
            get() = Input.isShiftDown
            set(_) {}

        var renewVAOs = true

        var alwaysBindBuffer = true

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

        private var boundVAO = -1
        fun bindVAO(vao: Int) {
            val vao2 = if (useVAOs) vao else 0
            if (vao2 >= 0 && (alwaysBindBuffer || boundVAO != vao)) {
                boundVAO = vao2
                glBindVertexArray(vao2)
            }
        }

        // element buffer is stored in VAO -> cannot cache it here
        // (at least https://www.khronos.org/opengl/wiki/Vertex_Specification says so)
        var boundBuffers = IntArray(1) { 0 }
        fun bindBuffer(slot: Int, buffer: Int, force: Boolean = false) {
            val index = slot - GL_ARRAY_BUFFER
            if (alwaysBindBuffer || index !in boundBuffers.indices) {
                glBindBuffer(slot, buffer)
            } else {
                if (boundBuffers[index] != buffer || force) {
                    if (buffer < 0) throw IllegalArgumentException("Buffer is invalid!")
                    boundBuffers[index] = buffer
                    glBindBuffer(slot, buffer)
                }
            }
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