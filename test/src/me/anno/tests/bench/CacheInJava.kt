package me.anno.tests.bench

import me.anno.utils.Clock
import me.anno.utils.hpc.HeavyProcessing
import java.util.concurrent.atomic.AtomicInteger

fun builtInSum(data: IntArray) = data.sum()

fun manualSum(data: IntArray): Int {
    var sum = 0
    for (element in data) {
        sum += element
    }
    return sum
}

fun stridedSum(data: IntArray): Int {
    var sum = 0
    val steps = 16
    for (j in 0 until steps) {
        for (i in j until data.size step steps) {
            sum += data[i]
        }
    }
    return sum
}

fun parallelSum(data: IntArray): Int {
    val sum = AtomicInteger()
    HeavyProcessing.processBalanced(0, data.size, 64) { i0, i1 ->
        var sumI = 0
        for (i in i0 until i1) {
            sumI += data[i]
        }
        sum.addAndGet(sumI)
    }
    return sum.get()
}

fun main() {
    val length = 1 shl 26
    val data = IntArray(length) { it }
    val clock = Clock("CacheInJava")
    clock.benchmark(3, 10, length, "built-in") { builtInSum(data) }
    clock.benchmark(3, 10, length, "manual") { manualSum(data) }
    clock.benchmark(3, 10, length, "strided") { stridedSum(data) }
    clock.benchmark(3, 10, length, "parallel") { parallelSum(data) }
}