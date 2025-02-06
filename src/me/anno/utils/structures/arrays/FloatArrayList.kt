package me.anno.utils.structures.arrays

import me.anno.cache.ICacheData
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.pooling.FloatArrayPool
import org.apache.logging.log4j.LogManager
import kotlin.math.min

open class FloatArrayList(initCapacity: Int = 16, val pool: FloatArrayPool? = null) :
    Saveable(), NativeArrayList, ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(FloatArrayList::class)
    }

    constructor(data: FloatArray) : this(data.size) {
        addAll(data, 0, data.size)
    }

    constructor(data: FloatArrayList) : this(data.size) {
        addAll(data, 0, data.size)
    }

    override var size = 0
        set(value) {
            field = value
            ensureCapacity(value)
        }

    var values = allocate(initCapacity)
    override val capacity: Int get() = values.size

    fun allocate(size: Int): FloatArray {
        return if (pool != null) pool[size, true, false] else FloatArray(size)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("size", size)
        writer.writeFloatArray("values", toFloatArray())
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "size" -> size = value as? Int ?: return
            "values" -> values = value as? FloatArray ?: return
            else -> super.setProperty(name, value)
        }
    }

    override fun resize(newSize: Int) {
        val array = values
        val newArray = try {
            allocate(newSize)
        } catch (e: OutOfMemoryError) {
            LOGGER.warn("Failed to allocated ${newSize * 4L} bytes for ExpandingFloatArray")
            throw e
        }
        array.copyInto(newArray)
        pool?.returnBuffer(array)
        this.values = newArray
    }

    fun add(value: Float) {
        ensureExtra(1)
        values[size++] = value
    }

    fun addUnsafe(x: Float) {
        values[size++] = x
    }

    operator fun set(index: Int, value: Float) {
        values[index] = value
    }

    fun addAll(v: FloatArray, srcStartIndex: Int = 0, length: Int = v.size - srcStartIndex) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun addAll(v: FloatArrayList, srcStartIndex: Int, length: Int) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun addUnsafe(src: FloatArray, startIndex: Int = 0, length: Int = src.size - startIndex) {
        src.copyInto(values, size, startIndex, startIndex + length)
        size += length
    }

    fun addUnsafe(src: FloatArrayList, startIndex: Int, length: Int) {
        addUnsafe(src.values, startIndex, length)
    }

    operator fun get(index: Int) = values[index]
    operator fun plusAssign(value: Float) {
        add(value)
    }

    fun toFloatArray(canReturnSelf: Boolean = true, exact: Boolean = true) = toFloatArray(size, canReturnSelf, exact)

    fun toFloatArray(size1: Int, canReturnSelf: Boolean = true, exact: Boolean = true): FloatArray {
        val array = values
        if (canReturnSelf && (size1 == array.size || (!exact && size1 <= array.size)))
            return array
        val value = allocate(size1)
        array.copyInto(value, 0, 0, min(size, size1))
        return value
    }

    override fun destroy() {
        pool?.returnBuffer(values)
        size = 0
    }

    fun sum(): Float {
        val values = values
        var sum = 0f
        for (i in 0 until size) {
            sum += values[i]
        }
        return sum
    }

    fun scale(s: Float) {
        val values = values
        for (i in 0 until size) {
            values[i] *= s
        }
    }

    fun fill(s: Float) {
        val array = values
        array.fill(s, 0, size)
    }

    fun toList(): List<Float> = toFloatArray().toList()

    override fun equals(other: Any?): Boolean {
        return other is FloatArrayList &&
                other.size == size &&
                (0 until size).all {
                    other[it] == this[it]
                }
    }

    override fun hashCode(): Int {
        var result = 1
        for (i in 0 until size) {
            result = 31 * result + this[i].toRawBits()
        }
        return result
    }
}