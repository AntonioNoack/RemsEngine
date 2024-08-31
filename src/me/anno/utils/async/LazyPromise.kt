package me.anno.utils.async

class LazyPromise<V : Any>(private val initialize: (Callback<V>) -> Unit) : AbstractPromise<V>() {

    override fun ensureLoading() {
        initializer.value
    }

    val initializer = lazy {
        initialize(::setValue)
    }
}