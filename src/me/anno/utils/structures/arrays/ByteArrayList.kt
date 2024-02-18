package me.anno.utils.structures.arrays

import org.apache.logging.log4j.LogManager
import kotlin.math.max

@Suppress("unused")
open class ByteArrayList(private val initCapacity: Int) {

    companion object {
        private val LOGGER = LogManager.getLogger(ByteArrayList::class)
    }

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
        src.copyInto(array!!, size, startIndex, length)
        size += length
    }

    fun addUnsafe(src: Byte) {
        array!![size++] = src
    }

    fun addUnsafe(src: ByteArrayList, startIndex: Int, length: Int) {
        src.array?.copyInto(array!!, size, startIndex, length)
        size += length
    }

    fun addAll(src: ByteArray, srcStartIndex: Int = 0, length: Int = src.size) {
        ensureExtra(length)
        addUnsafe(src, srcStartIndex, length)
    }

    fun addAll(src: ByteArrayList, startIndex: Int, length: Int) {
        if (length == 0) return
        ensureExtra(length)
        addUnsafe(src, startIndex, length)
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Byte) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = ByteArray(if (array == null) initCapacity else max(array.size * 2, 16))
            array?.copyInto(newArray)
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
            array?.copyInto(newArray)
            this.array = newArray
        }
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun toByteArray(size1: Int = size): ByteArray {
        val array = array
        if (array != null && size == array.size) return array
        return array?.copyOf(size1) ?: ByteArray(size1)
    }
}