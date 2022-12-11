package me.anno.utils.structures.arrays

import me.anno.utils.LOGGER
import kotlin.math.max

open class ExpandingGenericArray<V>(private val initCapacity: Int) {

    var size = 0

    var array: Array<Any?>? = null

    fun clear() {
        size = 0
    }

    fun add(value: V) = plusAssign(value)

    fun add(index: Int, value: V) {
        ensureExtra(1)
        val array = array!!
        System.arraycopy(array, index, array, index + 1, size - index)
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
            if (array != null) System.arraycopy(array, 0, newArray, 0, this.size)
            this.array = newArray
        }
    }


    operator fun get(index: Int): V = array!![index] as V
    operator fun plusAssign(value: V) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = arrayOfNulls<Any>(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}