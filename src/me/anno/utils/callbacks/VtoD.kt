package me.anno.utils.callbacks

fun interface VtoD<V> {
    fun call(instance: V): Double
}