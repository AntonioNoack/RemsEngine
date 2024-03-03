package me.anno.utils.structures.arrays

import org.apache.logging.log4j.LogManager
import kotlin.math.max

@Suppress("unused")
open class ByteArrayList(initialCapacity: Int) {

    companion object {
        private val LOGGER = LogManager.getLogger(ByteArrayList::class)
    }

    var size = 0
    var array: ByteArray = ByteArray(initialCapacity)

    fun clear() {
        size = 0
    }

    fun add(value: Byte) = plusAssign(value)

    operator fun set(index: Int, value: Byte) {
        array[index] = value
    }

    fun addUnsafe(src: ByteArray?, startIndex: Int, length: Int) {
        src?.copyInto(array, size, startIndex, startIndex + length)
        size += length
    }

    fun addUnsafe(src: Byte) {
        array[size++] = src
    }

    fun addUnsafe(src: ByteArrayList, startIndex: Int, length: Int) {
        addUnsafe(src.array, startIndex, length)
    }

    fun addAll(src: ByteArray?, startIndex: Int, length: Int) {
        ensureExtra(length)
        addUnsafe(src, startIndex, length)
    }

    fun addAll(src: ByteArrayList, startIndex: Int, length: Int) {
        addAll(src.array, startIndex, length)
    }

    operator fun get(index: Int): Byte = array[index]
    operator fun plusAssign(value: Byte) {
        ensureExtra(1)
        addUnsafe(value)
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
                ByteArray(newSize)
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Failed to allocated $newSize bytes for ExpandingByteArray")
                throw e
            }
            array.copyInto(newArray)
            this.array = newArray
        }
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun toByteArray(dstSize: Int = size): ByteArray {
        val array = array
        if (size == array.size) return array
        return array.copyOf(dstSize)
    }
}