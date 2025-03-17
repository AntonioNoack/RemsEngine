package me.anno.utils.async

import me.anno.cache.IgnoredException

inline fun <V : Any> promise(initialize: (Callback<V>) -> Unit): Promise<V> {
    val promise = Promise<V>()
    try {
        initialize(promise::setValue)
    } catch (ignored: IgnoredException) {
    } catch (e: Exception) {
        promise.setValue(null, e)
    }
    return promise
}

/**
 * find the first sample that succeeds the processing
 * */
fun <K, V : Any> firstPromise(samples: List<K>, process: (K, Callback<V>) -> Unit): Promise<V> {
    return promiseStep(samples, 0, process)
}

private fun <K, V : Any> promiseStep(samples: List<K>, i: Int, initialize: (K, Callback<V>) -> Unit): Promise<V> {
    return if (i < samples.size) {
        val sample = samples[i]
        val promise = promise { cb ->
            initialize(sample, cb)
        }
        promise.catch {
            promiseStep(samples, i + 1, initialize)
                .thenFulfill(promise)
        }
    } else promise { cb ->
        cb.err(null)
    }
}