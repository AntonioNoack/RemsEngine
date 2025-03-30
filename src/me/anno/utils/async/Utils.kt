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
fun <K, V : Any> firstPromise(samples: Iterable<K>, process: (K, Callback<V>) -> Unit): Promise<V> {
    return firstPromise(samples.iterator(), process)
}

/**
 * find the first sample that succeeds the processing
 * */
fun <K, V : Any> firstPromise(samples: Iterator<K>, process: (K, Callback<V>) -> Unit): Promise<V> {
    return if (samples.hasNext()) {
        val sample = samples.next()
        val promise = promise { cb ->
            process(sample, cb)
        }
        promise.catch {
            firstPromise(samples, process)
                .thenFulfill(promise)
        }
    } else promise { cb ->
        cb.err(null)
    }
}