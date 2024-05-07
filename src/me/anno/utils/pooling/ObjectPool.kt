package me.anno.utils.pooling

import org.apache.logging.log4j.LogManager

/**
 * inspired by Unity's object pools,
 * a pool to keep a certain amount of objects to prevent frequent allocation and frees/garbage collection
 * */
class ObjectPool<V>(
    private val createInstance: () -> V,
    private val resetInstance: (V) -> Unit,
    private val destroyFunc: (V) -> Unit,
    private val freeInstance: (V) -> Unit,
    checkDoubleReturns: Boolean,
    initialSize: Int = 16,
    private var maxSize: Int = 64,
) {

    companion object {
        private val LOGGER = LogManager.getLogger(ObjectPool::class)
    }

    constructor(create: () -> V) : this(create, {}, {}, {}, false)

    private val map = if (checkDoubleReturns) HashSet<V>(initialSize) else null
    private val data = ArrayList<V>(initialSize)

    fun create(): V {
        return if (data.isEmpty()) {
            createInstance()
        } else synchronized(this) {
            val element = data.removeAt(data.lastIndex)
            map?.remove(element)
            resetInstance(element)
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
        destroyFunc(v)
        synchronized(this) {
            if (data.size >= maxSize) {
                // we're at the limit, and can't destroy elements
                freeInstance(v)
            } else {
                data.add(v)
            }
        }
    }

}