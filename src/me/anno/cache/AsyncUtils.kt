package me.anno.cache

object AsyncUtils {

    fun <V : Any> Iterator<Promise<V>>.waitForAll(size: Int = 16): Promise<List<V?>> {
        val resultList = ArrayList<V?>(size)
        val result = Promise<List<V?>>()

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
     * Waits asynchronously for all elements in that collection.
     * Used by Rem's Studio?
     * */
    fun <V : Any> Collection<Promise<V>>.waitForAll(): Promise<List<V?>> {
        for (promise in this) {
            promise.lock()
        }
        val result = iterator().waitForAll(size)
        result.waitFor {
            for (promise in this) {
                promise.unlock()
            }
        }
        return result
    }

    /**
     * Waits asynchronously for all elements in that collection/iterable.
     * Used by Rem's Studio?
     * */
    fun <V : Any, W : Any> Pair<Promise<V>, Promise<W>>.waitForAll(): Promise<Pair<V?, W?>> {
        second.lock()
        val result = Promise<Pair<V?, W?>>()
        first.waitFor { firstV ->
            second.waitFor { secondV ->
                second.unlock()
                result.value = firstV to secondV
            }
        }
        return result
    }

    /**
     * Waits asynchronously for all elements in that collection/iterable.
     * Used by Rem's Studio?
     * */
    fun <V : Any, W : Any, X : Any> Triple<Promise<V>, Promise<W>, Promise<X>>.waitForAll():
            Promise<Triple<V?, W?, X?>> {
        second.lock()
        third.lock()
        val result = Promise<Triple<V?, W?, X?>>()
        first.waitFor { firstV ->
            second.waitFor { secondV ->
                third.waitFor { thirdV ->
                    result.value = Triple(firstV, secondV, thirdV)
                    second.unlock()
                    third.unlock()
                }
            }
        }
        return result
    }
}