package me.anno.utils.async

inline fun <V : Any> promise(initialize: (Callback<V>) -> Unit): Promise<V> {
    val value = Promise<V>()
    initialize(value::setValue)
    return value
}