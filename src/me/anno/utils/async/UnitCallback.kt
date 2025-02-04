package me.anno.utils.async

/**
 * callback without value
 * */
fun interface UnitCallback {
    fun call(exception: Exception?)
}