package me.anno.utils.async

/**
 * callback without value
 * */
fun interface UnitCallback {
    fun call(exception: Exception?)

    companion object {
        val default = UnitCallback { it?.printStackTrace() }
    }
}