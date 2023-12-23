package me.anno.gpu.buffer

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Attribute.Companion.computeOffsets
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.maths.Maths
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL31C
import org.lwjgl.opengl.GL43C.GL_BUFFER
import org.lwjgl.opengl.GL43C.glObjectLabel
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

abstract class OpenGLBuffer(val name: String, val type: Int, var attributes: List<Attribute>, val usage: Int) :
    ICacheData {

    constructor(name: String, type: Int, attributes: List<Attribute>) : this(name, type, attributes, GL_STATIC_DRAW)

    val stride = computeOffsets(attributes)

    var nioBuffer: ByteBuffer? = null

    var pointer = 0
    var session = 0

    var isUpToDate = false

    var locallyAllocated = 0L
    var elementCount = 0

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            onSessionChange()
        }
    }

    open fun onSessionChange() {
        pointer = 0
        isUpToDate = false
        locallyAllocated = allocate(locallyAllocated, 0L)
    }

    open fun upload(allowResize: Boolean = true, keepLarge: Boolean = false) {

        checkSession()

        GFX.check()

        if (nioBuffer == null) {
            createNioBuffer()
        }

        if (pointer == 0) pointer = glGenBuffers()
        if (pointer == 0) throw OutOfMemoryError("Could not generate OpenGL Buffer")

        bindBuffer(type, pointer)

        val nio = nioBuffer!!
        val newLimit = nio.position()
        elementCount = max(elementCount, newLimit / stride)
        nio.position(0)
        nio.limit(elementCount * stride)
        if (allowResize && locallyAllocated > 0 && newLimit <= locallyAllocated && (keepLarge || (newLimit >= locallyAllocated / 2 - 65536))) {
            // just keep the buffer
            glBufferSubData(type, 0, nio)
        } else {
            locallyAllocated = allocate(locallyAllocated, newLimit.toLong())
            glBufferData(type, nio, usage)
        }

        GFX.check()
        isUpToDate = true

        if (Build.isDebug) {
            DebugGPUStorage.buffers.add(this)
            glObjectLabel(GL_BUFFER, pointer, name)
            GFX.check()
        }
    }

    open fun uploadEmpty(newLimit: Long) {

        checkSession()

        GFX.check()

        if (pointer == 0) pointer = glGenBuffers()
        if (pointer == 0) throw OutOfMemoryError("Could not generate OpenGL Buffer")

        bindBuffer(type, pointer)

        locallyAllocated = allocate(locallyAllocated, newLimit)
        glBufferData(type, newLimit, usage)

        GFX.check()
        isUpToDate = true

        if (Build.isDebug) {
            DebugGPUStorage.buffers.add(this)
            glObjectLabel(GL_BUFFER, pointer, name)
            GFX.check()
        }
    }

    fun copyElementsTo(toBuffer: OpenGLBuffer, from: Long, to: Long, size: Long) {
        if (size == 0L) return
        if (stride != toBuffer.stride) {
            throw IllegalArgumentException("Buffers have mismatched strides")
        }
        copyBytesTo(toBuffer, from * stride, to * stride, size * stride)
    }

    fun copyBytesTo(toBuffer: OpenGLBuffer, from: Long, to: Long, size: Long) {

        // initial checks
        if (size == 0L) return
        if (toBuffer === this && from == to) return
        if (size < 0) {
            throw IllegalArgumentException("Size must be non-negative")
        }

        // just in case, ensure the buffers have data;
        // might fail us on Android, where OpenGL can lose its session
        ensureBuffer()
        toBuffer.ensureBuffer()

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Copying from $name to ${toBuffer.name}: from $from to $to, x$size")
        }

        if (locallyAllocated < from + size || toBuffer.locallyAllocated < to + size) {
            throw IllegalStateException("Illegal copy $from to $to, $from + $size vs $locallyAllocated / $to + $size vs ${toBuffer.locallyAllocated}")
        } else if (pointer == 0 || toBuffer.pointer == 0) {
            throw IllegalStateException("Buffer hasn't been created yet")
        }

        GFX.check()
        glBindBuffer(GL31C.GL_COPY_READ_BUFFER, pointer)
        glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, toBuffer.pointer)
        GL31C.glCopyBufferSubData(
            GL31C.GL_COPY_READ_BUFFER,
            GL31C.GL_COPY_WRITE_BUFFER,
            from, to, size
        )
        GFX.check()
    }

    /**
     * free this buffer on the CPU side;
     * you will no longer be able to implicitly, partially update it (-> state frozen)
     *
     * if you just need a GPU-only buffer, use uploadEmpty()
     *
     * todo except explicitly, implement that
     * */
    fun freeze(force: Boolean = false) {
        if (force || !OS.isAndroid) {
            if (nioBuffer != null) {
                ByteBufferPool.free(nioBuffer)
                nioBuffer = null
            }
        } else LOGGER.warn("Freezing is not supported on Android, because the context might reset")
    }

    fun simpleBind() {
        ensureBuffer()
        bindBuffer(type, pointer)
    }

    open fun unbind() {
        bindBuffer(type, 0)
    }

    abstract fun createNioBuffer()

    fun ensureBuffer() {
        checkSession()
        if (!isUpToDate) upload()
    }

    fun ensureBufferWithoutResize() {
        checkSession()
        if (!isUpToDate) upload(false)
    }

    fun put(v: Vector2f) {
        put(v.x, v.y)
    }

    fun put(v: FloatArray) {
        for (vi in v) {
            put(vi)
        }
    }

    fun put(v: FloatArray, index: Int, length: Int) {
        for (i in index until index + length) {
            put(v[i])
        }
    }

    fun put(v: Vector3f) {
        put(v.x, v.y, v.z)
    }

    fun put(v: Vector4f) {
        put(v.x, v.y, v.z, v.w)
    }

    fun put(x: Float, y: Float, z: Float, w: Float, a: Float) {
        put(x)
        put(y)
        put(z)
        put(w)
        put(a)
    }

    fun put(x: Float, y: Float, z: Float, w: Float) {
        put(x)
        put(y)
        put(z)
        put(w)
    }

    fun put(x: Float, y: Float, z: Float) {
        put(x)
        put(y)
        put(z)
    }

    fun put(x: Float, y: Float) {
        put(x)
        put(y)
    }

    fun put(f: Float) {
        nioBuffer!!.putFloat(f)
        isUpToDate = false
    }

    fun putByte(b: Byte) {
        nioBuffer!!.put(b)
        isUpToDate = false
    }

    fun putByte(f: Float) {
        val asInt = Maths.clamp(f * 127f, -127f, +127f).roundToInt()
        putByte(asInt.toByte())
    }

    fun putUByte(b: Int) {
        nioBuffer!!.put(b.toByte())
        isUpToDate = false
    }

    fun putShort(b: Short) {
        nioBuffer!!.putShort(b)
        isUpToDate = false
    }

    fun putUShort(b: Int) {
        nioBuffer!!.putShort(b.toShort())
        isUpToDate = false
    }

    fun putInt(b: Int) {
        nioBuffer!!.putInt(b)
        isUpToDate = false
    }

    override fun destroy() {
        if (Build.isDebug) DebugGPUStorage.buffers.remove(this)
        val buffer = pointer
        val vao = if (this is Buffer) vao else -1
        if (buffer > -1) {
            GFX.addGPUTask("OpenGLBuffer.destroy()", 1) {
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
        if (this is Buffer) this.vao = -1
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
            nioBuffer = null
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(OpenGLBuffer::class)

        // monkey & stuff is invisible with vaos
        // because VAOs need default values (probably)

        // todo working? looks like it
        val useVAOs = false
        val renewVAOs = false

        private var boundVAO = -1
        fun bindVAO(vao: Int) {
            val vao2 = if (useVAOs) vao else 0
            if (vao2 >= 0 && boundVAO != vao2) {
                boundVAO = vao2
                glBindVertexArray(vao2)
            }
        }

        // element buffer is stored in VAO -> cannot cache it here
        // (at least https://www.khronos.org/opengl/wiki/Vertex_Specification says so)
        var boundBuffers = IntArray(1)
        fun bindBuffer(slot: Int, buffer: Int, force: Boolean = false) {
            val index = slot - GL_ARRAY_BUFFER
            if (index !in boundBuffers.indices) {
                glBindBuffer(slot, buffer)
            } else {
                if (boundBuffers[index] != buffer || force) {
                    if (buffer < 0) throw IllegalArgumentException("Buffer is invalid!")
                    boundBuffers[index] = buffer
                    glBindBuffer(slot, buffer)
                }
            }
        }

        fun invalidateBinding() {
            boundBuffers.fill(0)
            boundVAO = -1
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
            private set

        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }
    }
}