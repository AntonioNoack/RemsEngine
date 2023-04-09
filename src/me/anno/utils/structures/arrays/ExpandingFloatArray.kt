package me.anno.utils.structures.arrays

import me.anno.cache.ICacheData
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.LOGGER
import me.anno.utils.pooling.FloatArrayPool
import org.joml.*
import kotlin.math.max
import kotlin.math.min

open class ExpandingFloatArray(initCapacity: Int, val pool: FloatArrayPool? = null) :
    Saveable(), ICacheData {

    var size = 0

    var array = alloc(initCapacity)

    val capacity get() = array.size

    fun alloc(size: Int): FloatArray {
        return if (pool != null) pool[size, true, false] else FloatArray(size)
    }

    fun clear() {
        size = 0
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("size", size)
        val array = array
        // clear the end, so we can save it with less space wasted
        for (i in size until array.size) {
            array[i] = 0f
        }
        writer.writeFloatArray("values", array)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "size" -> size = value
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
        if (requestedSize >= array.size) {
            val suggestedSize = max(array.size * 2, 16)
            val newSize = max(suggestedSize, requestedSize)
            val newArray = try {
                alloc(newSize)
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Failed to allocated ${newSize * 4L} bytes for ExpandingFloatArray")
                throw e
            }
            System.arraycopy(array, 0, newArray, 0, this.size)
            pool?.returnBuffer(array)
            this.array = newArray
        }
    }

    fun add(value: Float) {
        ensureExtra(1)
        array[size++] = value
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun addUnsafe(x: Float) {
        array[size++] = x
    }

    fun addUnsafe(x: Float, y: Float) {
        val array = array
        var size = size
        array[size++] = x
        array[size++] = y
        this.size = size
    }

    fun addUnsafe(x: Float, y: Float, z: Float) {
        val array = array
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    fun addUnsafe(x: Float, y: Float, z: Float, w: Float) {
        val array = array
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        array[size++] = w
        this.size = size
    }

    operator fun set(index: Int, value: Float) {
        array[index] = value
    }

    fun add(v: Vector2f) {
        ensureExtra(2)
        val array = array
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        this.size = size
    }

    fun add(v: Vector3f) {
        ensureExtra(3)
        val array = array
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        array[size++] = v.z
        this.size = size
    }

    fun add(v: Vector3d) {
        ensureExtra(3)
        val array = array
        var size = size
        array[size++] = v.x.toFloat()
        array[size++] = v.y.toFloat()
        array[size++] = v.z.toFloat()
        this.size = size
    }

    fun add(v: Vector4f) {
        ensureExtra(4)
        val array = array
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        array[size++] = v.z
        array[size++] = v.w
        this.size = size
    }

    fun add(v: Quaternionf) {
        ensureExtra(4)
        val array = array
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        array[size++] = v.z
        array[size++] = v.w
        this.size = size
    }

    fun add(v: Quaterniond) {
        ensureExtra(4)
        val array = array
        var size = size
        array[size++] = v.x.toFloat()
        array[size++] = v.y.toFloat()
        array[size++] = v.z.toFloat()
        array[size++] = v.w.toFloat()
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

    fun add(l: FloatArray, srcStartIndex: Int, srcLength: Int) {
        ensureExtra(srcLength)
        System.arraycopy(l, srcStartIndex, array, size, srcLength)
        size += srcLength
    }

    fun addAll(v: FloatArray, srcStartIndex: Int = 0, length: Int = v.size - srcStartIndex) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun addAll(v: ExpandingFloatArray, startIndex: Int, length: Int) {
        ensureExtra(length)
        addUnsafe(v, startIndex, length)
    }

    fun addUnsafe(src: FloatArray, startIndex: Int = 0, length: Int = src.size - startIndex) {
        System.arraycopy(src, startIndex, array, size, length)
        size += length
    }

    fun addUnsafe(src: ExpandingFloatArray, startIndex: Int, length: Int) {
        System.arraycopy(src.array, startIndex, array, size, length)
        size += length
    }

    operator fun get(index: Int) = array[index]
    operator fun plusAssign(value: Float) {
        add(value)
    }

    fun toFloatArray(canReturnSelf: Boolean = true, exact: Boolean = true) = toFloatArray(size, canReturnSelf, exact)

    fun toFloatArray(size1: Int, canReturnSelf: Boolean = true, exact: Boolean = true): FloatArray {
        val array = array
        if (canReturnSelf && (size1 == array.size || (!exact && size1 <= array.size)))
            return array
        val tmp = alloc(size1)
        System.arraycopy(array, 0, tmp, 0, min(size, size1))
        return tmp
    }

    override fun destroy() {
        pool?.returnBuffer(array)
        size = 0
    }

    operator fun plusAssign(v: Vector2f) {
        ensureExtra(2)
        val array = array
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        this.size = size
    }

    operator fun plusAssign(v: Vector3f) {
        add(v)
    }

    fun sum(): Float {
        val array = array
        var sum = 0.0
        for (i in 0 until size) {
            sum += array[i]
        }
        return sum.toFloat()
    }

    fun scale(s: Float) {
        val array = array
        for (i in 0 until size) {
            array[i] *= s
        }
    }

    fun fill(s: Float) {
        val array = array
        array.fill(s, 0, size)
    }

}