package me.anno.utils

import java.lang.RuntimeException

fun Any.anyToFloat(): Float {
    return when(this){
        is Int -> this.toFloat()
        is Long -> this.toFloat()
        is Float -> this
        is Double -> this.toFloat()
        else -> throw RuntimeException()
    }
}