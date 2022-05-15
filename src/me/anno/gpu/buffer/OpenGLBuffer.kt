package me.anno.gpu.buffer

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.buffer.Attribute.Companion.computeOffsets
import me.anno.gpu.shader.Shader
import me.anno.input.Input
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer
import kotlin.math.max

abstract class OpenGLBuffer(val type: Int, val attributes: List<Attribute>, val usage: Int) :
    ICacheData {

    constructor(type: Int, attributes: List<Attribute>) : this(type, attributes, GL_STATIC_DRAW)

    val stride = computeOffsets(attributes)

    var nioBuffer: ByteBuffer? = null

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
    }

    var elementCount = 0

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
        elementCount = max(elementCount, newLimit / stride)
        nio.position(0)
        nio.limit(elementCount * stride)
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

    open fun unbind(shader: Shader) {
        bindBuffer(GL_ARRAY_BUFFER, 0)
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

    override fun destroy() {
        val buffer = pointer
        val vao = if (this is Buffer) vao else -1
        if (buffer > -1) {
            GFX.addGPUTask("OpenGLBuffer.destroy()", 1) {
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
        if (this is Buffer) this.vao = -1
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
        }
        nioBuffer = null
    }

    companion object {

        private val LOGGER = LogManager.getLogger(OpenGLBuffer::class)

        // monkey & stuff is invisible with vaos
        // because VAOs need default values (probably)

        // todo change back to use it constantly, or to be configurable
        var useVAOs
            get() = Input.isShiftDown
            set(_) {}

        var alwaysBindBuffer = true

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

        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }

    }

}