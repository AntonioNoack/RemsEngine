package me.anno.cache

open class IgnoredException : RuntimeException() {
    override fun printStackTrace() {
    }
}