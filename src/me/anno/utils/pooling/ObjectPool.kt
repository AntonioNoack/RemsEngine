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

    constructor(size: Int, create: () -> V) : this(create, {}, {}, {}, false, 16, size)
    constructor(create: () -> V) : this(create, {}, {}, {}, false)
    constructor(clazz: Class<V>) : this({ clazz.newInstance() })

    private val doubleReturnMap = if (checkDoubleReturns) HashSet<V>(initialSize) else null
    private val cachedInstances = ArrayList<V>(initialSize)

    val size get() = cachedInstances.size

    fun create(): V {
        return synchronized(this) {
            val element = cachedInstances.removeLastOrNull()
            if (element != null) {
                doubleReturnMap?.remove(element)
                resetInstance(element)
            }
            element ?: createInstance()
        }
    }

    fun destroy(v: V) {
        if (doubleReturnMap != null) {
            synchronized(this) {
                if (v in doubleReturnMap) {
                    LOGGER.warn("Returned element twice! $v")
                } else doubleReturnMap.add(v)
            }
        }
        destroyFunc(v)
        synchronized(this) {
            if (cachedInstances.size >= maxSize) {
                // we're at the limit, and can't destroy elements
                freeInstance(v)
            } else {
                cachedInstances.add(v)
            }
        }
    }

    /**
     * remove all instances
     * */
    fun gc() {
        synchronized(this) {
            for (i in cachedInstances.indices) {
                freeInstance(cachedInstances[i])
            }
            cachedInstances.clear()
        }
    }
}