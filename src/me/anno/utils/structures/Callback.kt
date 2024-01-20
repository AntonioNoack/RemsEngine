package me.anno.utils.structures

fun interface Callback<V> {
    fun call(value: V?, exception: Exception?)
    fun ok(value: V) = call(value, null)
    fun err(exception: Exception?) = call(null, exception)
}