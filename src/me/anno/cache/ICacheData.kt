package me.anno.cache

/**
 * A value that may be destroyed.
 * */
interface ICacheData {
    fun destroy() {}
}