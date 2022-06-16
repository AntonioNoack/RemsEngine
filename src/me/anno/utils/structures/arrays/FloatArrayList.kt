package me.anno.utils.structures.arrays

import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class FloatArrayList(val capacity: Int, val defaultValue: Float = 0f) {

    private val buffers = ArrayList<FloatArray>()
    var size = 0

    fun clear() {
        size = 0
        buffers.clear()
    }

    operator fun get(index: Int) = buffers[index / capacity][index % capacity]
    operator fun get(index: Int, defaultValue: Float): Float {
        val buffer = buffers.getOrNull(index / capacity) ?: return defaultValue
        return buffer[index % capacity]
    }

    operator fun plusAssign(value: Int) = plusAssign(value.toFloat())
    operator fun plusAssign(value: Float) {
        val index = size % capacity
        if (index == 0) addBuffer()
        buffers.last()[index] = value
        size++
    }

    private fun addBuffer() {
        buffers.add(
            if (defaultValue == 0f) FloatArray(capacity)
            else FloatArray(capacity) { defaultValue }
        )
    }

    operator fun set(index: Int, value: Float) {
        val bufferIndex = index / capacity
        for (i in buffers.size..bufferIndex) {
            addBuffer()
        }
        val localIndex = index % capacity
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
            val lastSize = size % capacity
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

    fun toFloatArray(): FloatArray {
        val dst = FloatArray(size)
        for (i in 0 until (size + capacity - 1) / capacity) {
            val src = buffers[i]
            val offset = i * capacity
            System.arraycopy(src, 0, dst, offset, min(capacity, size - offset))
        }
        return dst
    }

    override fun toString(): String {
        return toFloatArray().joinToString()
    }

}