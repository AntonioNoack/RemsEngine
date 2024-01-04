package me.anno.tests.bench

import me.anno.Time
import me.anno.tests.LOGGER
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun dot0(data: IntArray) = data.sum()

fun dot1(data: IntArray): Int {
    var sum = 0
    for (element in data) {
        sum += element
    }
    return sum
}

fun dot2(data: IntArray): Int {
    var sum = 0
    val steps = 16
    for (j in 0 until steps) {
        for (i in j until data.size step steps) {
            sum += data[i]
        }
    }
    return sum
}

fun dot3(data: IntArray): Int {
    val sum = AtomicInteger()
    processBalanced(0, data.size, 1) { i0, i1 ->
        var sumI = 0
        for (i in i0 until i1) {
            sumI += data[i]
        }
        sum.addAndGet(sumI)
    }
    return sum.get()
}

fun measure(name: String, data: IntArray, func: (IntArray) -> Int) {
    val t0 = Time.nanoTime
    val sum = func(data)
    val t1 = Time.nanoTime
    LOGGER.info("$name: ${"%.4f".format(Locale.ENGLISH, (t1 - t0) * 1e-9)}, $sum")
}

fun main() {
    val length = 1 shl 26
    val data = IntArray(length) { it }
    for (i in 0 until 3) {
        measure("dot0", data, ::dot0)
        measure("dot1", data, ::dot1)
        measure("dot2", data, ::dot2)
        measure("dot3", data, ::dot3)
    }
}