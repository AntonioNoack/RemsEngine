package me.anno.utils.structures.arrays

import me.anno.utils.LOGGER
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
open class ExpandingByteArray(private val initCapacity: Int) {

    var size = 0

    fun clear() {
        size = 0
    }

    var array: ByteArray? = null

    fun add(value: Byte) = plusAssign(value)

    operator fun set(index: Int, value: Byte) {
        array!![index] = value
    }

    fun addUnsafe(src: ByteArray, startIndex: Int = 0, length: Int = src.size) {
        System.arraycopy(src, startIndex, array!!, size, length)
        size += length
    }

    fun addUnsafe(src: Byte) {
        array!![size++] = src
    }

    fun addUnsafe(src: ExpandingByteArray, startIndex: Int, length: Int) {
        System.arraycopy(src.array!!, startIndex, array!!, size, length)
        size += length
    }

    fun add(v: ByteArray, srcStartIndex: Int = 0, length: Int = v.size) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun add(v: ExpandingByteArray, startIndex: Int, length: Int) {
        if (length == 0) return
        ensureExtra(length)
        addUnsafe(v, startIndex, length)
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Byte) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = ByteArray(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
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
                ByteArray(newSize)
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Failed to allocated $newSize bytes for ExpandingByteArray")
                throw e
            }
            if (array != null) System.arraycopy(array, 0, newArray, 0, this.size)
            this.array = newArray
        }
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun toByteArray(size1: Int): ByteArray {
        val array = array
        val size = size
        if (array != null && size == array.size) return array
        val tmp = ByteArray(size1)
        if (size > 0) System.arraycopy(array!!, 0, tmp, 0, min(size, size1))
        return tmp
    }

    fun toByteArray(): ByteArray {
        val array = array
        val size = size
        if (array != null && size == array.size) return array
        val tmp = ByteArray(size)
        if (size > 0) System.arraycopy(array!!, 0, tmp, 0, size)
        return tmp
    }


}