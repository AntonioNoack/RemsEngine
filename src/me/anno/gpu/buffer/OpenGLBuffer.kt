package me.anno.gpu.buffer

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.buffer.Attribute.Companion.computeOffsets
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.maths.Maths
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Floats.roundToIntOr
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL46C.glDeleteBuffers
import org.lwjgl.opengl.GL46C.glMapBuffer
import org.lwjgl.opengl.GL46C.glUnmapBuffer
import org.lwjgl.opengl.GL46C.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.GL46C.glFlushMappedBufferRange
import org.lwjgl.opengl.GL46C.GL_DYNAMIC_STORAGE_BIT
import org.lwjgl.opengl.GL46C.glBufferStorage
import org.lwjgl.opengl.GL46C
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.roundToInt

abstract class OpenGLBuffer(
    val name: String, val type: Int,
    var attributes: List<Attribute>, val usage: BufferUsage
) : ICacheData {

    constructor(name: String, type: Int, attributes: List<Attribute>) : this(name, type, attributes, BufferUsage.STATIC)

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

    private fun prepareUpload() {
        checkSession()

        GFX.check()

        if (nioBuffer == null) {
            createNioBuffer()
        }

        if (pointer == 0) pointer = GL46C.glGenBuffers()
        if (pointer == 0) throw OutOfMemoryError("Could not generate OpenGL Buffer")
    }

    private fun finishUpload() {
        GFX.check()
        isUpToDate = true

        if (Build.isDebug) {
            DebugGPUStorage.buffers.add(this)
            GL46C.glObjectLabel(GL46C.GL_BUFFER, pointer, name)
            GFX.check()
        }
    }

    private fun keepBuffer(allowResize: Boolean, newLimit: Int, keepLarge: Boolean): Boolean {
        return allowResize && locallyAllocated > 0 && newLimit <= locallyAllocated && (keepLarge || (newLimit >= locallyAllocated / 2 - 65536))
    }

    open fun upload(allowResize: Boolean = true, keepLarge: Boolean = false) {
        prepareUpload()
        bindBuffer(type, pointer)

        val nio = nioBuffer!!
        val newLimit = nio.position()
        elementCount = max(elementCount, newLimit / stride)
        nio.position(0)
        nio.limit(elementCount * stride)
        if (keepBuffer(allowResize, newLimit, keepLarge)) {
            // just keep the buffer
            GL46C.glBufferSubData(type, 0, nio)
        } else {
            locallyAllocated = allocate(locallyAllocated, newLimit.toLong())
            GL46C.glBufferData(type, nio, usage.id)
        }

        finishUpload()
    }

    open fun uploadAsync(
        callback: () -> Unit,
        allowResize: Boolean = true, keepLarge: Boolean = false,
    ) {
        prepareUpload()
        bindBuffer(type, pointer)

        val nio = nioBuffer!!
        val newLimit = nio.position()
        elementCount = max(elementCount, newLimit / stride)
        nio.position(0)
        nio.limit(elementCount * stride)
        if (!keepBuffer(allowResize, newLimit, keepLarge)) {
            glBufferStorage(type, newLimit.toLong(), GL_DYNAMIC_STORAGE_BIT or GL_MAP_WRITE_BIT)
            locallyAllocated = allocate(locallyAllocated, newLimit.toLong())
        }

        val dst = glMapBuffer(type, pointer, newLimit.toLong(), nio)!!
        val name = "OpenGLBuffer.uploadAsync('$name', $newLimit)"
        thread(name = name) {
            // copy all data
            if (dst !== nio) {
                dst.put(nio)
            }
            addGPUTask(name, newLimit) {
                if (pointer >= 0) {
                    GFX.check()
                    bindBuffer(type, pointer)
                    glFlushMappedBufferRange(type, 0, newLimit.toLong())
                    glUnmapBuffer(type)

                    finishUpload()
                    callback()
                    GFX.check()
                } else LOGGER.warn("Buffer was deleted while async upload")
            }
        }
    }

    open fun uploadEmpty(newLimit: Long) {

        if (pointer >= 0) glDeleteBuffers(pointer)

        prepareUpload()
        bindBuffer(type, pointer)

        GL46C.glBufferStorage(type, newLimit, GL_DYNAMIC_STORAGE_BIT)
        // GL46C.glBufferData(type, newLimit, usage.id)
        locallyAllocated = allocate(locallyAllocated, newLimit)

        finishUpload()
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Copying from $name to ${toBuffer.name}: from $from to $to, x$size")
        }

        if (locallyAllocated < from + size || toBuffer.locallyAllocated < to + size) {
            throw IllegalStateException("Illegal copy $from to $to, $from + $size vs $locallyAllocated / $to + $size vs ${toBuffer.locallyAllocated}")
        } else if (pointer == 0 || toBuffer.pointer == 0) {
            throw IllegalStateException("Buffer hasn't been created yet")
        }

        GFX.check()
        GL46C.glBindBuffer(GL46C.GL_COPY_READ_BUFFER, pointer)
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, toBuffer.pointer)
        GL46C.glCopyBufferSubData(
            GL46C.GL_COPY_READ_BUFFER,
            GL46C.GL_COPY_WRITE_BUFFER,
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

    fun ensureBufferAsync(callback: () -> Unit) {
        checkSession()
        if (!isUpToDate) uploadAsync(callback)
        else callback()
    }

    fun ensureBufferWithoutResize() {
        checkSession()
        if (!isUpToDate) upload(false)
    }

    fun put(v: Vector2f): OpenGLBuffer {
        return put(v.x, v.y)
    }

    fun put(v: FloatArray): OpenGLBuffer {
        for (vi in v) {
            put(vi)
        }
        return this
    }

    fun put(v: FloatArray, index: Int, length: Int): OpenGLBuffer {
        for (i in index until index + length) {
            put(v[i])
        }
        return this
    }

    fun put(v: Vector3f): OpenGLBuffer {
        return put(v.x, v.y, v.z)
    }

    fun put(v: Vector4f): OpenGLBuffer {
        return put(v.x, v.y, v.z, v.w)
    }

    fun put(x: Float, y: Float, z: Float, w: Float, a: Float): OpenGLBuffer {
        return put(x).put(y).put(z).put(w).put(a)
    }

    fun put(x: Float, y: Float, z: Float, w: Float): OpenGLBuffer {
        return put(x).put(y).put(z).put(w)
    }

    fun put(x: Float, y: Float, z: Float): OpenGLBuffer {
        return put(x).put(y).put(z)
    }

    fun put(x: Float, y: Float): OpenGLBuffer {
        return put(x).put(y)
    }

    fun put(f: Float): OpenGLBuffer {
        nioBuffer!!.putFloat(f)
        isUpToDate = false
        return this
    }

    fun putByte(b: Byte): OpenGLBuffer {
        nioBuffer!!.put(b)
        isUpToDate = false
        return this
    }

    fun putRGBA(c: Int): OpenGLBuffer {
        val buffer = nioBuffer!!
        buffer
            .put(c.r().toByte())
            .put(c.g().toByte())
            .put(c.b().toByte())
            .put(c.a().toByte())
        isUpToDate = false
        return this
    }

    fun putByte(f: Float): OpenGLBuffer {
        val asInt = Maths.clamp(f * 127f, -127f, +127f).roundToIntOr()
        return putByte(asInt.toByte())
    }

    fun putShort(b: Short): OpenGLBuffer {
        nioBuffer!!.putShort(b)
        isUpToDate = false
        return this
    }

    fun putInt(b: Int): OpenGLBuffer {
        nioBuffer!!.putInt(b)
        isUpToDate = false
        return this
    }

    override fun destroy() {
        if (Build.isDebug) DebugGPUStorage.buffers.remove(this)
        val buffer = pointer
        if (buffer > -1) {
            addGPUTask("OpenGLBuffer.destroy()", 1) {
                onDestroyBuffer(buffer)
                GL46C.glDeleteBuffers(buffer)
                locallyAllocated = allocate(locallyAllocated, 0L)
            }
        }
        this.pointer = 0
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
            nioBuffer = null
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(OpenGLBuffer::class)

        // element buffer is stored in VAO -> cannot cache it here
        // (at least https://www.khronos.org/opengl/wiki/Vertex_Specification says so)
        var boundBuffers = IntArray(1)
        fun bindBuffer(slot: Int, buffer: Int, force: Boolean = false) {
            val index = slot - GL46C.GL_ARRAY_BUFFER
            if (index !in boundBuffers.indices) {
                GL46C.glBindBuffer(slot, buffer)
            } else {
                if (boundBuffers[index] != buffer || force) {
                    if (buffer < 0) throw IllegalArgumentException("Buffer is invalid!")
                    boundBuffers[index] = buffer
                    GL46C.glBindBuffer(slot, buffer)
                }
            }
        }

        fun invalidateBinding() {
            boundBuffers.fill(0)
        }

        fun onDestroyBuffer(buffer: Int) {
            for (index in boundBuffers.indices) {
                if (buffer == boundBuffers[index]) {
                    val slot = index + GL46C.GL_ARRAY_BUFFER
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