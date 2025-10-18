package me.anno.cache

open class GetterCacheContent<V : Any, B : Any>(
    val base: IPromiseBody<B>,
    val getter: (B) -> V?
) : IPromiseBody<V> {

    override val hasExpired: Boolean get() = base.hasExpired
    override val timeoutCacheTime: Long get() = base.timeoutCacheTime
    override val hasValue: Boolean get() = base.hasValue
    override val hasBeenDestroyed: Boolean get() = base.hasBeenDestroyed

    override var value: V?
        get() {
            val baseValue = base.value
            return if (baseValue != null) getter(baseValue) else null
        }
        set(value) {
            throw NotImplementedError()
        }

    override val waitForCallbacks: List<(V?) -> Unit> = emptyList()

    override fun addCallback(callback: (V?) -> Unit) {
        base.addCallback {
            val mapped = if (it != null) getter(it) else null
            callback(mapped)
        }
    }

    override fun addCallbacks(callbacks: List<(V?) -> Unit>) {
        for (callback in callbacks) {
            addCallback(callback)
        }
    }

    override fun update(timeoutMillis: Long) = base.update(timeoutMillis)
}