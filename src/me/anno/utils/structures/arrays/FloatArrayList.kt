package me.anno.utils.structures.arrays

import me.anno.cache.ICacheData
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.pooling.FloatArrayPool
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FloatArrayList(capacity: Int) : ICacheData {

    companion object {
        val pool = FloatArrayPool(64)
    }

    val shift = log2(capacity.toFloat()).roundToInt()
    val capacity = 1 shl shift
    val mask = capacity - 1

    private val buffers = ArrayList<FloatArray>()
    var size = 0

    fun clear() {
        size = 0
        destroy()
    }

    operator fun get(index: Int) = buffers[index ushr shift][index and mask]
    operator fun get(index: Int, defaultValue: Float): Float {
        val buffer = buffers.getOrNull(index ushr shift) ?: return defaultValue
        return buffer[index and mask]
    }

    operator fun plusAssign(value: Int) = plusAssign(value.toFloat())
    operator fun plusAssign(value: Float) {
        val index = size and mask
        if (index == 0) addBuffer()
        buffers.last()[index] = value
        size++
    }

    private fun addBuffer() {
        buffers.add(pool.createBuffer(capacity))
    }

    operator fun set(index: Int, value: Float) {
        val bufferIndex = index ushr shift
        for (i in buffers.size..bufferIndex) {
            addBuffer()
        }
        val localIndex = index and mask
        buffers[bufferIndex][localIndex] = value
        size = max(index + 1, size)
    }

    operator fun plusAssign(v: Vector2f) {
        this += v.x
        this += v.y
    }

    operator fun plusAssign(v: Vector3f) {
        this += v.x
        this += v.y
        this += v.z
    }

    operator fun plusAssign(v: Vector4f) {
        this += v.x
        this += v.y
        this += v.z
        this += v.w
    }

    fun addRGB(c: Int) {
        this += c.r01()
        this += c.g01()
        this += c.b01()
    }

    fun addRGBA(c: Int) {
        this += c.r01()
        this += c.g01()
        this += c.b01()
        this += c.a01()
    }

    fun putInto(dst0: ByteBuffer) {
        if (size > 0) {
            val pos0 = dst0.position()
            val dst = dst0.asFloatBuffer()
            // dst.position(pos0/4)
            val lastSize = size and mask
            if (lastSize == 0) {
                for (buffer in buffers) {
                    dst.put(buffer)
                }
            } else {
                for (i in 0 until buffers.size - 1) {
                    dst.put(buffers[i])
                }
                val lastBuffer = buffers.last()
                dst.put(lastBuffer, 0, lastSize)
            }
            dst0.position(pos0 + size * 4)
            // dst0.position(dst.position()*4)
        }
    }

    fun add(l: FloatArrayList, srcStartIndex: Int, srcLength: Int) {
        for (i in srcStartIndex until srcStartIndex + srcLength) {
            plusAssign(l[i])
        }
    }

    fun add(l: FloatArray, srcStartIndex: Int, srcLength: Int) {
        for (i in srcStartIndex until srcStartIndex + srcLength) {
            plusAssign(l[i])
        }
    }

    fun ensureExtra(size: Int) {}
    fun addUnsafe(x: Float) {
        plusAssign(x)
    }

    fun addUnsafe(x: Float, y: Float) {
        plusAssign(x)
        plusAssign(y)
    }

    fun addUnsafe(x: Float, y: Float, z: Float) {
        plusAssign(x)
        plusAssign(y)
        plusAssign(z)
    }

    fun add(x: Float) {
        plusAssign(x)
    }

    fun add(x: Float, y: Float) {
        plusAssign(x)
        plusAssign(y)
    }

    fun add(x: Float, y: Float, z: Float) {
        plusAssign(x)
        plusAssign(y)
        plusAssign(z)
    }


    fun toFloatArray(): FloatArray {
        val dst = FloatArray(size)
        for (i in 0 until ((size + mask) ushr shift)) {
            val src = buffers[i]
            val offset = i shl shift
            src.copyInto(dst, offset, 0, min(capacity, size - offset))
        }
        return dst
    }

    override fun toString(): String {
        return toFloatArray().joinToString()
    }

    override fun destroy() {
        for (i in buffers.indices) {
            pool.returnBuffer(buffers[i])
        }
        buffers.clear()
    }
}