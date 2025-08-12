package me.anno.gpu.buffer

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFX.INVALID_SESSION
import me.anno.gpu.GFX.isPointerValid
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.maths.Maths
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.Threads
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertGreaterThanEquals
import me.anno.utils.assertions.assertIs
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.WrapDirect.wrapDirect
import me.anno.utils.types.Floats.roundToIntOr
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL15C.glGetBufferSubData
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_DYNAMIC_STORAGE_BIT
import org.lwjgl.opengl.GL46C.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.GL46C.glBufferStorage
import org.lwjgl.opengl.GL46C.glDeleteBuffers
import org.lwjgl.opengl.GL46C.glFlushMappedBufferRange
import org.lwjgl.opengl.GL46C.glMapBuffer
import org.lwjgl.opengl.GL46C.glUnmapBuffer
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

abstract class OpenGLBuffer(
    val name: String, val target: Int,
    var attributes: AttributeLayout,
    val usage: BufferUsage
) : ICacheData {

    /**
     * if attributes is StridedAttributeLayout, you probably shouldn't access this property
     * */
    val stride: Int
        get() = (attributes as? CompactAttributeLayout)?.stride ?: 1

    var nioBuffer: ByteBuffer? = null

    var pointer = INVALID_POINTER
    var session = INVALID_SESSION

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
        pointer = INVALID_POINTER
        isUpToDate = false
        locallyAllocated = allocate(locallyAllocated, 0L)
    }

    private fun prepareUpload() {
        checkSession()

        GFX.check()
        if (!isPointerValid(pointer)) pointer = GL46C.glGenBuffers()
        if (!isPointerValid(pointer)) throw OutOfMemoryError("Could not generate OpenGL Buffer")
    }

    private fun finishUpload() {
        isUpToDate = true
        if (Build.isDebug) {
            DebugGPUStorage.buffers.add(this)
            GL46C.glObjectLabel(GL46C.GL_BUFFER, pointer, name)
        }
    }

    private fun keepBuffer(allowResize: Boolean, newLimit: Int, keepLarge: Boolean): Boolean {
        return allowResize && locallyAllocated > 0 && newLimit <= locallyAllocated && (keepLarge || (newLimit >= locallyAllocated / 2 - 65536))
    }

    open fun upload(allowResize: Boolean = true, keepLarge: Boolean = false) {
        prepareUpload()
        assertTrue(isPointerValid(pointer))
        bindBuffer(target, pointer)

        val nio = getOrCreateNioBuffer()
        val newLimit = nio.position()
        elementCount = max(elementCount, newLimit / stride)
        nio.position(0)
        nio.limit(elementCount * stride)
        if (keepBuffer(allowResize, newLimit, keepLarge)) {
            // just keep the buffer
            GL46C.glBufferSubData(target, 0, nio)
        } else {
            locallyAllocated = allocate(locallyAllocated, newLimit.toLong())
            assertTrue(nio.isDirect, "Buffers for OpenGL must be native. Use ByteBufferPool!")
            GL46C.glBufferData(target, nio, usage.id)
        }

        finishUpload()
    }

    open fun uploadAsync(
        callback: () -> Unit,
        allowResize: Boolean = true, keepLarge: Boolean = false,
    ) {
        prepareUpload()
        bindBuffer(target, pointer)

        val nio = getOrCreateNioBuffer()
        val newLimit = nio.position()
        elementCount = max(elementCount, newLimit / stride)
        nio.position(0)
        nio.limit(elementCount * stride)

        if (!keepBuffer(allowResize, newLimit, keepLarge)) {
            glBufferStorage(target, newLimit.toLong(), GL_DYNAMIC_STORAGE_BIT or GL_MAP_WRITE_BIT)
            locallyAllocated = allocate(locallyAllocated, newLimit.toLong())
        }

        val dst = glMapBuffer(target, pointer, newLimit.toLong(), nio)!!
        val name = "OpenGLBuffer.uploadAsync('$name', $newLimit)"
        Threads.runTaskThread(name) {
            // copy all data
            if (dst !== nio) {
                dst.put(nio)
            }
            addGPUTask(name, newLimit) {
                if (isPointerValid(pointer)) {
                    GFX.check()
                    bindBuffer(target, pointer)
                    glFlushMappedBufferRange(target, 0, newLimit.toLong())
                    glUnmapBuffer(target)

                    finishUpload()
                    callback()
                    GFX.check()
                } else LOGGER.warn("Buffer was deleted while async upload")
            }
        }
    }

    open fun uploadEmpty(newLimit: Long) {

        GFX.checkIsGFXThread()

        assertTrue(newLimit > 0)
        if (isPointerValid(pointer)) {
            glDeleteBuffers(pointer)
            pointer = 0
        }

        prepareUpload()
        bindBuffer(target, pointer)

        glBufferStorage(target, newLimit, GL_DYNAMIC_STORAGE_BIT)
        // GL46C.glBufferData(type, newLimit, usage.id)
        locallyAllocated = allocate(locallyAllocated, newLimit)

        finishUpload()
    }

    fun copyElementsTo(toBuffer: OpenGLBuffer, from: Long, to: Long, size: Long) {
        if (size == 0L) return
        val fromLayout = assertIs(CompactAttributeLayout::class, attributes)
        val toLayout = assertIs(CompactAttributeLayout::class, toBuffer.attributes)
        assertEquals(fromLayout.stride, toLayout.stride)
        val stride = toLayout.stride
        copyBytesTo(toBuffer, from * stride, to * stride, size * stride)
    }

    // todo test this
    fun uploadElementsPartially(fromOffsetInDataI: Int, fromData: Any, sizeInElements: Int, toOffsetInElements: Int) {
        if (sizeInElements == 0) return
        assertTrue(sizeInElements > 0)
        assertTrue(toOffsetInElements >= 0)
        uploadBytesPartially(
            fromOffsetInDataI, fromData, sizeInElements.toLong() * stride,
            toOffsetInElements.toLong() * stride
        )
    }

    fun uploadBytesPartially(fromOffsetInDataI: Int, fromData: Any, sizeInBytes: Long, toOffsetInBytes: Long) {

        val toBuffer = this
        val size = sizeInBytes

        // initial checks
        if (size == 0L) return
        assertGreaterThanEquals(size, 0, "Size must be non-negative")

        // just in case, ensure the buffers have data;
        // might fail us on Android, where OpenGL can lose its session
        ensureBuffer()
        assertTrue(isPointerValid(pointer))

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Copying to ${toBuffer.name}: from $fromOffsetInDataI to $toOffsetInBytes, x$size")
        }

        if (toOffsetInBytes < 0 || locallyAllocated < toOffsetInBytes + size) {
            throw IllegalStateException("Illegal copy $fromOffsetInDataI to $toOffsetInBytes [from], $fromOffsetInDataI + $size vs $locallyAllocated")
        }

        // actual copy
        toBuffer.bind()
        // println("glBufferSubData(${toBuffer.target},#${toBuffer.pointer},offset $to,$fromBuffer)")
        val fromOffsetInDataL = fromOffsetInDataI.toLong()
        if (fromData is Buffer) assertEquals(0, fromOffsetInDataL)
        when (fromData) {

            // easy
            is ByteBuffer -> {
                assertEquals(fromData.position(), fromOffsetInDataI)
                assertEquals(sizeInBytes, fromData.remaining().toLong())
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, fromData)
            }
            is ShortBuffer -> {
                assertEquals(fromData.position(), fromOffsetInDataI)
                assertEquals(sizeInBytes, fromData.remaining().toLong() shl 1)
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, fromData)
            }
            is IntBuffer -> {
                assertEquals(fromData.position(), fromOffsetInDataI)
                assertEquals(sizeInBytes, fromData.remaining().toLong() shl 2)
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, fromData)
            }
            is FloatBuffer -> {
                assertEquals(fromData.position(), fromOffsetInDataI)
                assertEquals(sizeInBytes, fromData.remaining().toLong() shl 2)
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, fromData)
            }

            is ByteArray -> {
                val (tmp, clean) = fromData.wrapDirect(fromOffsetInDataI, sizeInBytes.toInt())
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, tmp)
                clean()
            }
            is ShortArray -> {
                val (tmp, clean) = fromData.wrapDirect(fromOffsetInDataI, (sizeInBytes shr 1).toInt())
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, tmp)
                clean()
            }
            is IntArray -> {
                val (tmp, clean) = fromData.wrapDirect(fromOffsetInDataI, (sizeInBytes shr 2).toInt())
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, tmp)
                clean()
            }
            is FloatArray -> {
                val (tmp, clean) = fromData.wrapDirect(fromOffsetInDataI, (sizeInBytes shr 2).toInt())
                GL46C.glBufferSubData(toBuffer.target, toOffsetInBytes, tmp)
                clean()
            }
            else -> throw NotImplementedError("Unknown type for uploadBytesPartially")
        }
        GFX.check()
    }

    fun copyBytesTo(toBuffer: OpenGLBuffer, from: Long, to: Long, size: Long) {

        // initial checks
        if (size == 0L) return
        if (toBuffer === this && from == to) return
        assertGreaterThanEquals(size, 0, "Size must be non-negative")

        // just in case, ensure the buffers have data;
        // might fail us on Android, where OpenGL can lose its session
        // ensureBuffer()
        toBuffer.ensureBuffer()

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Copying from $name to ${toBuffer.name}: from $from to $to, x$size")
        }

        if (isPointerValid(pointer) && locallyAllocated < from + size) {
            throw IllegalStateException("Illegal copy $from to $to [from], $from + $size vs $locallyAllocated")
        }

        if (toBuffer.locallyAllocated < to + size) {
            throw IllegalStateException("Illegal copy $from to $to [to], $to + $size vs ${toBuffer.locallyAllocated}")
        } else if (toBuffer.pointer == 0) {
            throw IllegalStateException("Buffer hasn't been created yet")
        }

        if (!isPointerValid(pointer)) {
            // buffer hasn't been uploaded to GPU yet,
            // and we don't need to -> just sub-upload it (saves an OpenGL buffer)

            val fromBuffer = getOrCreateNioBuffer()

            // backup & update state
            val oldLimit = fromBuffer.limit()
            val oldPosition = fromBuffer.position()
            fromBuffer.position(from.toInt())
            fromBuffer.limit((from + size).toInt())

            // actual copy
            toBuffer.bind()
            // println("glBufferSubData(${toBuffer.target},#${toBuffer.pointer},offset $to,$fromBuffer)")
            GL46C.glBufferSubData(toBuffer.target, to, fromBuffer)
            GFX.check()

            // restore state
            fromBuffer.limit(oldLimit)
            fromBuffer.position(oldPosition)
        } else {

            if (this === toBuffer) {
                assertTrue(from + size <= to || to + size <= from, "Copied ranges must not overlap!")
                // according to https://registry.khronos.org/OpenGL-Refpages/gl4/html/glCopyBufferSubData.xhtml
            }

            GFX.check()
            bindBuffer(GL46C.GL_COPY_READ_BUFFER, pointer)
            bindBuffer(GL46C.GL_COPY_WRITE_BUFFER, toBuffer.pointer)
            // println("glCopyBufferSubData(#${pointer}->#${toBuffer.pointer},$from->$to,x$size)")
            GL46C.glCopyBufferSubData(
                GL46C.GL_COPY_READ_BUFFER,
                GL46C.GL_COPY_WRITE_BUFFER,
                from, to, size
            )
            GFX.check()
        }
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

    fun bind() {
        ensureBuffer()
        bindBuffer(target, pointer)
    }

    open fun unbind() {
        bindBuffer(target, 0)
    }

    abstract fun createNioBuffer(): ByteBuffer

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

    fun getOrCreateNioBuffer(): ByteBuffer {
        var buffer = nioBuffer
        if (buffer != null) return buffer
        buffer = createNioBuffer()
        nioBuffer = buffer
        return buffer
    }

    fun put(v: Vector2f): OpenGLBuffer {
        return put(v.x, v.y)
    }

    fun put(v: FloatArray): OpenGLBuffer {
        return put(v, 0, v.size)
    }

    fun put(v: FloatArray, index: Int, length: Int): OpenGLBuffer {
        val nio = getOrCreateNioBuffer()
        assertTrue(nio.remaining().shr(2) >= length)
        isUpToDate = false
        val pos = nio.position()
        nio.asFloatBuffer().put(v, index, length)
        nio.position(pos + length * 4)
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
        getOrCreateNioBuffer().putFloat(f)
        isUpToDate = false
        return this
    }

    fun putByte(b: Byte): OpenGLBuffer {
        getOrCreateNioBuffer().put(b)
        isUpToDate = false
        return this
    }

    fun putRGBA(c: Int): OpenGLBuffer {
        getOrCreateNioBuffer()
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
        getOrCreateNioBuffer().putShort(b)
        isUpToDate = false
        return this
    }

    fun putInt(b: Int): OpenGLBuffer {
        getOrCreateNioBuffer().putInt(b)
        isUpToDate = false
        return this
    }

    override fun destroy() {
        if (Build.isDebug) DebugGPUStorage.buffers.remove(this)
        val buffer = pointer
        if (buffer > -1) {
            addGPUTask("OpenGLBuffer.destroy()", 1) {
                onDestroyBuffer(buffer)
                glDeleteBuffers(buffer)
                locallyAllocated = allocate(locallyAllocated, 0L)
            }
        }
        this.pointer = INVALID_POINTER
        if (nioBuffer != null) {
            ByteBufferPool.free(nioBuffer)
            nioBuffer = null
        }
    }

    fun readAsFloatArray(
        startIndex: Long = 0L,
        values: FloatArray = FloatArray(((elementCount - startIndex) * stride.shr(2)).toInt())
    ): FloatArray {
        assertEquals(0, stride.and(3))
        ensureBuffer()
        bindBuffer(target, pointer)
        GFX.check()
        glGetBufferSubData(target, startIndex * stride, values)
        GFX.check()
        return values
    }

    fun readAsIntArray(
        startIndex: Long = 0L,
        values: IntArray = IntArray(((elementCount - startIndex) * stride.shr(2)).toInt())
    ): IntArray {
        assertEquals(0, stride.and(3))
        ensureBuffer()
        bindBuffer(target, pointer)
        GFX.check()
        glGetBufferSubData(target, startIndex * stride, values)
        GFX.check()
        return values
    }

    fun readAsByteBuffer(
        startIndex: Long = 0L,
        values: ByteBuffer// = ByteBufferPool.allocateDirect(((elementCount - startIndex) * stride).toInt())
    ): ByteBuffer {
        ensureBuffer()
        bindBuffer(target, pointer)
        GFX.check()
        glGetBufferSubData(target, startIndex * stride, values)
        GFX.check()
        return values
    }

    fun readAsShortArray(
        startIndex: Long = 0L,
        values: ShortArray = ShortArray(((elementCount - startIndex) * stride.shr(1)).toInt())
    ): ShortArray {
        assertEquals(0, stride.and(1))
        ensureBuffer()
        bindBuffer(target, pointer)
        GFX.check()
        glGetBufferSubData(target, startIndex * stride, values)
        GFX.check()
        return values
    }

    companion object {

        private val LOGGER = LogManager.getLogger(OpenGLBuffer::class)

        // element buffer is stored in VAO -> cannot cache it here
        // (at least https://www.khronos.org/opengl/wiki/Vertex_Specification says so)
        val boundBuffers = IntArray(1)
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

        val allocated = AtomicLong()

        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated.addAndGet(newValue - oldValue)
            return newValue
        }
    }
}