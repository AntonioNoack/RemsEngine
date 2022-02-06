package me.anno.utils.test

import me.anno.utils.Clock

fun <V> measure(name: String, func: () -> V): V {
    val c = Clock()
    val value = func()
    c.stop(name)
    return value
}