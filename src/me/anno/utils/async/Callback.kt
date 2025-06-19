package me.anno.utils.async

import me.anno.cache.AsyncCacheData
import me.anno.cache.ICacheData
import me.anno.utils.structures.lists.Lists.createArrayList

/**
 * most loader functions return a value eventually (the modern web doesn't have synchronous IO),
 * and might throw an exception, e.g., if the loaded file isn't valid;
 *
 * this interface represents a typical callback from such a function,
 * and should be used whenever you have such a use-case.
 * */
fun interface Callback<in V> {

    fun call(value: V?, exception: Exception?)
    fun ok(value: V) = call(value, null)
    fun err(exception: Exception?) = call(null, exception)

    companion object {

        fun <V> printError(): Callback<V> {
            return onSuccessImpl(null)
        }

        fun <V> onSuccess(callback: (V) -> Unit): Callback<V> {
            return onSuccessImpl(callback)
        }

        fun finish(callback: () -> Unit): Callback<Any?> {
            return Callback { v, err ->
                if (v == null) err?.printStackTrace()
                callback()
            }
        }

        private fun <V> onSuccessImpl(callback: ((V) -> Unit)? = null): Callback<V> {
            return Callback { v, err ->
                if (v == null) err?.printStackTrace()
                else callback?.invoke(v)
            }
        }

        /**
         * returns a callback, this calls the original callback after mapping the value synchronously
         * */
        fun <V, W> Callback<V>.map(valueMapping: (W) -> V): Callback<W> {
            return Callback { value, err ->
                call(if (value != null) valueMapping(value) else null, err)
            }
        }

        /**
         * returns a callback, this calls the original callback after mapping the value asynchronously
         * */
        fun <V, W> Callback<V>.mapAsync(then: (W, Callback<V>) -> Unit): Callback<W> {
            return Callback { value, errI ->
                if (value != null) then(value, this)
                else err(errI)
            }
        }

        /**
         * returns a callback, this calls the original callback after mapping the value synchronously
         * */
        fun <V: Any> Callback<V>.waitFor(): Callback<AsyncCacheData<V>> {
            val self = this
            return Callback { value, err ->
                if (value != null) value.waitFor(self)
                else self.err(err)
            }
        }

        fun <V: Any> Callback<V>.wait(): Callback<AsyncCacheData<V>> {
            val self = this
            return Callback { value, err ->
                if (value != null) {
                    value.waitFor { self.call(it, err) }
                } else self.err(err)
            }
        }

        /**
         * joins all callbacks; starts generator functions serially, but you could easily make them
         * multithreaded by starting a thread for each task in process()
         * */
        fun <V, W : Any> List<V>.mapCallback(
            process: (Int, V, Callback<W>) -> Unit,
            callback: Callback<List<W>>
        ) {
            if (isEmpty()) {
                callback.ok(emptyList())
            } else {
                indices
                    .associateWith { this[it] }
                    .mapCallback(process) { resultMap, err ->
                        val resultList = if (resultMap != null) createArrayList(size) { resultMap[it]!! } else null
                        callback.call(resultList, err)
                    }
            }
        }

        /**
         * joins all callbacks; starts generator functions serially, but you could easily make them
         * multithreaded by starting a thread for each task in process()
         * */
        fun <V, W : Any> Set<V>.mapCallback(
            process: (Int, V, Callback<W>) -> Unit,
            callback: Callback<Set<W>>
        ) {
            toList().mapCallback(process) { res, err ->
                callback.call(res?.toSet(), err)
            }
        }

        /**
         * joins all callbacks; starts generator functions serially, but you could easily make them
         * multithreaded by starting a thread for each task in process()
         * */
        fun <K, V, W : Any> Map<K, V>.mapCallback(
            process: (K, V, Callback<W>) -> Unit,
            callback: Callback<Map<K, W>>
        ) {
            if (isEmpty()) {
                callback.ok(emptyMap())
            } else {
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
        }

        private fun <K, W : Any> Map<K, W>.cleanup() {
            // cleanup: destroy all temporary results
            for (wi in values) {
                if (wi is ICacheData) {
                    wi.destroy()
                }
            }
        }
    }
}