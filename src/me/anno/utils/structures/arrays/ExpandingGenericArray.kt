package me.anno.utils.structures.arrays

import org.apache.logging.log4j.LogManager
import kotlin.math.max

open class ExpandingGenericArray<V>(private val initCapacity: Int) {

    companion object {
        private val LOGGER = LogManager.getLogger(ExpandingGenericArray::class)
    }

    var size = 0

    var array: Array<Any?>? = null

    fun clear() {
        size = 0
    }

    fun add(value: V) = plusAssign(value)

    fun add(index: Int, value: V) {
        ensureExtra(1)
        val array = array!!
        array.copyInto(array, index + 1, index, size)
        array[index] = value
        size++
    }

    operator fun set(index: Int, value: V) {
        array!![index] = value
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
                arrayOfNulls<Any>(newSize)
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Failed to allocated $newSize bytes for ExpandingByteArray")
                throw e
            }
            array?.copyInto(newArray)
            this.array = newArray
        }
    }


    operator fun get(index: Int): V = array!![index] as V
    operator fun plusAssign(value: V) {
        ensureExtra(1)
        array!![size++] = value
    }
}