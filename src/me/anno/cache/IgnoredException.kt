package me.anno.cache

open class IgnoredException: RuntimeException() {
    override fun printStackTrace() {
        throw IllegalArgumentException("Cannot print IgnoredException!")
    }
}