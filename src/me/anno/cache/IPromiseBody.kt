package me.anno.cache

interface IPromiseBody<V : Any> : ICacheData {

    val hasExpired: Boolean
    val timeoutCacheTime: Long
    val hasValue: Boolean
    val hasBeenDestroyed: Boolean

    var value: V?

    val waitForCallbacks: List<(V?) -> Unit>

    fun addCallback(callback: (V?) -> Unit)
    fun addCallbacks(callbacks: List<(V?) -> Unit>)
    fun update(timeoutMillis: Long)
}