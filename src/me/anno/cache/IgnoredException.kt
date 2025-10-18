package me.anno.cache

/**
 * Use this exception, if you don't want exception-handling to spam your logs.
 * */
open class IgnoredException(msg: String = "") : RuntimeException(msg) {
    override fun printStackTrace() {
    }
}