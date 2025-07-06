package me.anno.cache

object AsyncUtils {

    fun <V : Any> Iterator<AsyncCacheData<V>>.waitForAll(size: Int = 16): AsyncCacheData<List<V?>> {
        val resultList = ArrayList<V?>(size)
        val result = AsyncCacheData<List<V?>>()

        fun handleNext() {
            if (hasNext()) {
                val element = next()
                element.waitFor { handleNext() }
            } else {
                result.value = resultList
            }
        }

        handleNext()
        return result
    }

    /**
     * Waits asynchronously for all elements in that collection/iterable.
     * Used by Rem's Engine
     * */
    fun <V : Any> Iterable<AsyncCacheData<V>>.waitForAll(): AsyncCacheData<List<V?>> {
        val size = if (this is Collection<*>) size else 16
        return iterator().waitForAll(size)
    }
}