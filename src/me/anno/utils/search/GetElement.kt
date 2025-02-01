package me.anno.utils.search

fun interface GetElement<V> {
    operator fun get(idx: Int): V
}