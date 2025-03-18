package me.anno.cache

open class IgnoredException(msg: String = "") : RuntimeException(msg) {
    override fun printStackTrace() {
    }
}