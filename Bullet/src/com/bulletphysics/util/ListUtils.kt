package com.bulletphysics.util

object ListUtils {

    fun <V> MutableList<V>.swapRemove(value: V) {
        swapRemove(indexOf(value))
    }

    fun <V> MutableList<V>.swapRemove(i: Int) {
        swap(i, lastIndex)
        removeLast()
    }

    fun <V> MutableList<V>.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}