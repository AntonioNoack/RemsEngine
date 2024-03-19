package me.anno.utils.structures

/**
 * most loader functions return a value eventually (the modern web doesn't have synchronous IO),
 * and might throw an exception, e.g., if the loaded file isn't valid;
 *
 * this interface represents a typical callback from such a function,
 * and should be used whenever you have such a use-case.
 * */
fun interface Callback<V> {
    fun call(value: V?, exception: Exception?)
    fun ok(value: V) = call(value, null)
    fun err(exception: Exception?) = call(null, exception)
}