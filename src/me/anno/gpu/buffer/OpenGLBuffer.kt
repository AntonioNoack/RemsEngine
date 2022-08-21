package me.anno.gpu.buffer

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Attribute.Companion.computeOffsets
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.maths.Maths
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.joml.Vector2fc
import org.joml.Vector3fc
import org.joml.Vector4fc
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

abstract class OpenGLBuffer(val type: Int, var attributes: List<Attribute>, val usage: Int) :
    ICacheData {

    constructor(type: Int, attributes: List<Attribute>) : this(type, attributes, GL_STATIC_DRAW)

    val stride = computeOffsets(attributes)

    var nioBuffer: ByteBuffer? = null

    var pointer = -1
    var session = 0

    var isUpToDate = false

    var locallyAllocated = 0L
    var elementCount = 0

    fun getName() = getName(0)
    fun getName(index: Int) = attributes[index].name

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            onSessionChange()
        }
    }

    open fun onSessionChange() {
        pointer = -1
        isUpToDate = false
        locallyAllocated = allocate(locallyAllocated, 0L)
    }

    open fun upload(allowResize: Boolean = true) {

        checkSession()

        GFX.check()

        if (nioBuffer == null) {
            createNioBuffer()
        }

        if (pointer <= 0) pointer = glGenBuffers()
        if (pointer <= 0) throw OutOfMemoryError("Could not generate OpenGL Buffer")

        bindBuffer(type, pointer)

        val nio = nioBuffer!!
        val newLimit = nio.position()
        elementCount = max(elementCount, newLimit / stride)
        nio.position(0)
        nio.limit(elementCount * stride)
        if (allowResize && locallyAllocated > 0 && newLimit in (locallyAllocated / 2 - 65536)..locallyAllocated) {
            // just keep the buffer
            glBufferSubData(type, 0, nio)
        } else {
            locallyAllocated = allocate(locallyAllocated, newLimit.toLong())
            glBufferData(type, nio, usage)
        }

        GFX.check()
        isUpToDate = true

        if (DebugGPUStorage.buffers.add(this)) {
            /*val title = "Created buffer of size ${locallyAllocated.formatFileSize()}"
            if (locallyAllocated > 1e6) RuntimeException(title).printStackTrace()
            else LOGGER.debug(title)*/
        }

    }

    /**
     * free this buffer on the CPU side;
     * you will no longer be able to implicitly, partially update it (-> state frozen)
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

    fun put(v: Vector2fc) {
        put(v.x(), v.y())
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

    fun put(v: Vector3fc) {
        put(v.x(), v.y(), v.z())
    }

    fun put(v: Vector4fc) {
        put(v.x(), v.y(), v.z(), v.w())
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

    fun putDouble(d: Double) {
        nioBuffer!!.putDouble(d)
        isUpToDate = false
    }

    override fun destroy() {
        DebugGPUStorage.buffers.remove(this)
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
        this.pointer = -1
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

        // todo change back to use it constantly, or to be configurable
        // todo this currently crashes the engine, why ever...
        var useVAOs // works on most meshes, but not all :/
            get() = false // Input.isShiftDown
            set(_) {}

        var renewVAOs = true
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
            // GFX.checkIsGFXThread()
            allocated += newValue - oldValue
            return newValue
        }

    }

}