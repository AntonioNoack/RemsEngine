package com.bulletphysics.util

import java.lang.reflect.Array
import java.util.*

/**
 * Object pool for arrays.
 *
 * @author jezek2
 */
class ArrayPool<T>(private val componentType: Class<*>) {

    private val list = ArrayList<T>()
    private val comparator: Comparator<in Any?> =
        if (componentType == Double::class.javaPrimitiveType) {
            floatComparator
        } else if (componentType == Int::class.javaPrimitiveType) {
            intComparator
        } else if (!componentType.isPrimitive) {
            objectComparator
        } else {
            throw UnsupportedOperationException("unsupported type $componentType")
        }

    private val key = IntValue()

    private fun create(length: Int): T {
        @Suppress("UNCHECKED_CAST")
        return Array.newInstance(componentType, length) as T
    }

    /**
     * Returns array of exactly the same length as demanded, or create one if not
     * present in the pool.
     */
    fun getFixed(length: Int): T {
        key.value = length
        val index = list.binarySearch(key, comparator)
        if (index < 0) {
            return create(length)
        }
        return list.removeAt(index)
    }

    /**
     * Releases array into object pool.
     *
     * @param array previously obtained array from this pool
     */
    fun release(array: T) {
        var index = list.binarySearch(array, comparator)
        if (index < 0) index = -index - 1
        list.add(index, array)

        // remove references from object arrays:
        if (comparator === objectComparator) {
            Arrays.fill(array as kotlin.Array<*>, null)
        }
    }

    private class IntValue {
        var value: Int = 0
    }

    companion object {
        /** ///////////////////////////////////////////////////////////////////////// */
        private val floatComparator = Comparator { o1: Any?, o2: Any? ->
            val len1 = if (o1 is IntValue) o1.value else (o1 as DoubleArray).size
            val len2 = if (o2 is IntValue) o2.value else (o2 as DoubleArray).size
            len1.compareTo(len2)
        }

        private val intComparator = Comparator { o1: Any?, o2: Any? ->
            val len1 = if (o1 is IntValue) o1.value else (o1 as IntArray).size
            val len2 = if (o2 is IntValue) o2.value else (o2 as IntArray).size
            len1.compareTo(len2)
        }

        private val objectComparator = Comparator { o1: Any?, o2: Any? ->
            val len1 = if (o1 is IntValue) o1.value else (o1 as kotlin.Array<*>).size
            val len2 = if (o2 is IntValue) o2.value else (o2 as kotlin.Array<*>).size
            len1.compareTo(len2)
        }

        private val threadLocal = ThreadLocal.withInitial { HashMap<Class<*>, ArrayPool<*>>() }

        /**
         * Returns per-thread array pool for given type, or create one if it doesn't exist.
         *
         * @param cls type
         * @return object pool
         */
        fun <T> get(cls: Class<*>): ArrayPool<T> {
            @Suppress("UNCHECKED_CAST")
            return threadLocal.get().getOrPut(cls) {
                ArrayPool<T>(cls)
            } as ArrayPool<T>
        }

        @JvmStatic
        fun cleanCurrentThread() {
            threadLocal.remove()
        }
    }
}
