package me.anno.utils.structures.arrays

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.LOGGER
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

class ExpandingFloatArray(
    private var initCapacity: Int
) : Saveable() {

    var size = 0

    private var array: FloatArray? = null

    val capacity get() = array?.size ?: 0

    fun clear() {
        size = 0
    }

    fun ensure() {
        if (array == null) {
            array = FloatArray(initCapacity)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("initCapacity", initCapacity)
        writer.writeInt("size", size)
        val array = array
        if (array != null) {
            // clear the end, so we can save it with less space wasted
            for (i in size until array.size) {
                array[i] = 0f
            }
            writer.writeFloatArray("values", array)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "size" -> size = value
            "initCapacity" -> initCapacity
            else -> super.readInt(name, value)
        }
    }

    override fun readFloatArray(name: String, values: FloatArray) {
        when (name) {
            "values" -> array = values
            else -> super.readFloatArray(name, values)
        }
    }

    fun ensureExtra(delta: Int) {
        ensureCapacity(size + delta)
    }

    fun ensureCapacity(requestedSize: Int) {
        val array = array
        if (array == null || requestedSize >= array.size) {
            val suggestedSize = if (array == null) initCapacity else max(array.size * 2, 16)
            val newSize = max(suggestedSize, requestedSize)
            val newArray = try {
                FloatArray(newSize)
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Failed to allocated ${newSize * 4L} bytes for ExpandingIntArray")
                throw e
            }
            if (array != null) System.arraycopy(array, 0, newArray, 0, this.size)
            this.array = newArray
        }
    }

    fun add(value: Float) {
        ensureExtra(1)
        array!![size++] = value
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun addUnsafe(x: Float) {
        array!![size++] = x
    }

    fun addUnsafe(x: Float, y: Float) {
        val array = array!!
        var size = size
        array[size++] = x
        array[size++] = y
        this.size = size
    }

    fun addUnsafe(x: Float, y: Float, z: Float) {
        val array = array!!
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    fun addUnsafe(x: Float, y: Float, z: Float, w: Float) {
        val array = array!!
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        array[size++] = w
        this.size = size
    }

    operator fun set(index: Int, value: Float) {
        array!![index] = value
    }

    fun add(v: Vector3f) {
        ensureExtra(3)
        val array = array!!
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        array[size++] = v.z
        this.size = size
    }

    fun add(x: Float, y: Float) {
        ensureExtra(2)
        addUnsafe(x)
        addUnsafe(y)
    }

    fun add(x: Float, y: Float, z: Float) {
        ensureExtra(3)
        addUnsafe(x)
        addUnsafe(y)
        addUnsafe(z)
    }

    fun add(v: FloatArray, srcStartIndex: Int = 0, length: Int = v.size - srcStartIndex) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun add(v: ExpandingFloatArray, startIndex: Int, length: Int) {
        ensureExtra(length)
        addUnsafe(v, startIndex, length)
    }

    fun addUnsafe(src: FloatArray, startIndex: Int = 0, length: Int = src.size - startIndex) {
        System.arraycopy(src, startIndex, array!!, size, length)
        size += length
    }

    fun addUnsafe(src: ExpandingFloatArray, startIndex: Int, length: Int) {
        System.arraycopy(src.array!!, startIndex, array!!, size, length)
        size += length
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Float) {
        add(value)
    }

    fun toFloatArray(size1: Int): FloatArray {
        val array = array
        val size = size
        if (array != null && size == array.size) return array
        val tmp = FloatArray(size1)
        if (size > 0) System.arraycopy(array!!, 0, tmp, 0, min(size, size1))
        return tmp
    }

    fun toFloatArray(): FloatArray {
        val array = array
        val size = size
        if (array != null && size == array.size) return array
        val tmp = FloatArray(size)
        if (size > 0) System.arraycopy(array!!, 0, tmp, 0, size)
        return tmp
    }

    operator fun plusAssign(v: Vector2f) {
        ensureExtra(2)
        val array = array!!
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        this.size = size
    }

    operator fun plusAssign(v: Vector3f) {
        add(v)
    }

}