package me.anno.utils.pooling

import me.anno.utils.LOGGER

/**
 * inspired by Unity's object pools,
 * a pool to keep a certain amount of objects to prevent frequent allocation and frees/garbage collection
 * */
class ObjectPool<V>(
    val create: () -> V,
    val reset: (V) -> Unit,
    val destroy: (V) -> Unit,
    val free: (V) -> Unit,
    checkDoubleReturns: Boolean,
    initialSize: Int = 16,
    val maxSize: Int = 10_000,
) {

    private val map = if (checkDoubleReturns) HashSet<V>(initialSize) else null
    private val data = ArrayList<V>(initialSize)

    fun create(): V {
        return if (data.isEmpty()) {
            create()
        } else synchronized(this) {
            val element = data.removeAt(data.lastIndex)
            map?.remove(element)
            reset(element)
            element
        }
    }

    fun destroy(v: V) {
        if (map != null) {
            synchronized(this) {
                if (v in map) {
                    LOGGER.warn("Returned element twice! $v")
                } else map.add(v)
            }
        }
        destroy(v)
        synchronized(this) {
            if (data.size >= maxSize) {
                // we're at the limit, and can't destroy elements
                free(v)
            } else {
                data.add(v)
            }
        }
    }

}