package com.bulletphysics.util

/**
 * Object pool.
 *
 * @author jezek2
 */
class ObjectPool<T>(private val cls: Class<T>) {
    private val list = ArrayList<T>()

    private fun create(): T {
        return cls.newInstance()
    }

    /**
     * Returns instance from pool, or create one if pool is empty.
     *
     * @return instance
     */
    fun get(): T {
        return if (list.isNotEmpty()) {
            list.removeLast()
        } else {
            create()
        }
    }

    /**
     * Release instance into pool.
     *
     * @param obj previously obtained instance from pool
     */
    fun release(obj: T) {
        list.add(obj)
    }

    companion object {
        /** ///////////////////////////////////////////////////////////////////////// */
        private val threadLocal =
            ThreadLocal.withInitial { HashMap<Class<*>, ObjectPool<*>>() }

        /**
         * Returns per-thread object pool for given type, or create one if it doesn't exist.
         *
         * @param cls type
         * @return object pool
         */
        fun <T> get(cls: Class<T>): ObjectPool<T> {
            @Suppress("UNCHECKED_CAST")
            return threadLocal.get().getOrPut(cls) {
                ObjectPool(cls)
            } as ObjectPool<T>
        }

        @JvmStatic
        fun cleanCurrentThread() {
            threadLocal.remove()
        }
    }
}
