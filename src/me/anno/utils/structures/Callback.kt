package me.anno.utils.structures

import me.anno.cache.ICacheData
import me.anno.utils.structures.lists.Lists.createArrayList

/**
 * most loader functions return a value eventually (the modern web doesn't have synchronous IO),
 * and might throw an exception, e.g., if the loaded file isn't valid;
 *
 * this interface represents a typical callback from such a function,
 * and should be used whenever you have such a use-case.
 * */
fun interface Callback<V> {
    fun call(value: V?, exception: Exception?)
    fun ok(value: V) = call(value, null)
    fun err(exception: Exception?) = call(null, exception)

    companion object {

        fun <V, W : Any> List<V>.mapCallback(
            process: (Int, V, Callback<W>) -> Unit,
            callback: Callback<List<W>>
        ) {
            indices
                .associateWith { this[it] }
                .mapCallback(process) { resultMap, err ->
                    val resultList = if (resultMap != null) createArrayList(size) { resultMap[it]!! } else null
                    callback.call(resultList, err)
                }
        }

        fun <V, W : Any> Set<V>.mapCallback(
            process: (Int, V, Callback<W>) -> Unit,
            callback: Callback<Set<W>>
        ) {
            toList().mapCallback(process) { res, err ->
                callback.call(res?.toSet(), err)
            }
        }

        fun <K, V, W : Any> Map<K, V>.mapCallback(
            process: (K, V, Callback<W>) -> Unit,
            callback: Callback<Map<K, W>>
        ) {
            val outputs = HashMap<K, W>()
            var hasError = false
            for ((k, v) in this) {
                process(k, v) { w, err ->
                    synchronized(outputs) {
                        if (!hasError && err != null) {
                            hasError = true
                            outputs.cleanup()
                            callback.err(err)
                        } else {
                            outputs[k] = w!!
                            if (outputs.size == size) {
                                callback.ok(outputs)
                            }
                        }
                    }
                }
            }
        }

        fun <K, W : Any> Map<K, W>.cleanup() {
            // cleanup: destroy all temporary results
            for (wi in values) {
                if (wi is ICacheData) {
                    wi.destroy()
                }
            }
        }
    }
}