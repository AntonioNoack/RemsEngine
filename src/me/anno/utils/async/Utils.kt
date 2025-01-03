package me.anno.utils.async

inline fun <V : Any> promise(initialize: (Callback<V>) -> Unit): Promise<V> {
    val value = Promise<V>()
    initialize(value::setValue)
    return value
}

fun <K, V : Any> firstPromise(samples: List<K>, initialize: (K, Callback<V>) -> Unit): Promise<V> {
    return promiseStep(samples, 0, initialize)
}

fun <K, V : Any> promiseStep(samples: List<K>, i: Int, initialize: (K, Callback<V>) -> Unit): Promise<V> {
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