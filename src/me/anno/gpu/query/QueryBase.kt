package me.anno.gpu.query

import kotlin.math.max

abstract class QueryBase {

    var lastResult = -1L

    var sum = 0L
    var weight = 0L

    val average
        get(): Long {
            val w = weight
            return if (w <= 0L) 0L else sum / w
        }

    val result
        get(): Long {
            return lastResult
        }

    fun scaleWeight(multiplier: Float = 0.1f) {
        val oldWeight = weight
        if (oldWeight > 1L || multiplier > 1f) {
            val newWeight = max(1L, (oldWeight * multiplier).toLong())
            sum = sum * newWeight / oldWeight
            weight = newWeight
        }
    }

}