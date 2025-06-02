package me.anno.utils.async

import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD

@Deprecated(USE_COROUTINES_INSTEAD)
class LazyPromise<V : Any>(private val initialize: (Callback<V>) -> Unit) : Promise<V>() {

    override fun ensureLoading() {
        initializer.value
    }

    val initializer = lazy {
        initialize(::setValue)
    }
}