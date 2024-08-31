package me.anno.utils.async

inline fun <V: Any> promise(initialize: (Callback<V>) -> Unit): AbstractPromise<V> {
    val value = AbstractPromise<V>()
    initialize(value::setValue)
    return value
}