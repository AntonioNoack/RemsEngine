package me.anno.tests.utils

import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Booleans.toInt
import kotlin.random.Random

// inspired by https://www.youtube.com/watch?v=RwgKqA9j3JE
// where I think that the problem is kind of trivial, because generation time is on the same time consumption level as solving it
//  -> optimization is kind of pointless
// (and I was kind of offended that he said recursion was slow without any evidence)
//  -> it's 1.4x slower than my custom-stack version, but that's it

// a way to optimize it would be to use 62-bit long masks

fun main() {
    val sx = 64
    val sy = 64
    val probability = 0.595f
    val prob8 = (probability * 256).toInt()
    var dt0 = 0L
    var dt1 = 0L
    var sum = 0
    val total = 10000
    val field = BooleanArray(sx * sy)
    val tasks = ExpandingIntArray(64)
    val random = Random(System.nanoTime())
    for (i in 0 until total) {
        val t0 = System.nanoTime()
        for (fi in field.indices step 4) {
            val rnd = random.nextInt()
            field[fi] = rnd.ushr(24) < prob8
            field[fi + 1] = rnd.shl(8).ushr(24) < prob8
            field[fi + 2] = rnd.shl(16).ushr(24) < prob8
            field[fi + 3] = rnd.shl(24).ushr(24) < prob8
        }
        val t1 = System.nanoTime()
        tasks.clear()
        sum += isConnectedCustomStack(field, sx, sy, tasks).toInt()
        // sum += isConnectedRecursive(field, sx, sy).toInt()
        val t2 = System.nanoTime()
        dt0 += t1 - t0
        dt1 += t2 - t1
    }
    println("gen: ${dt0 / 1e9f} vs solve: ${dt1 / 1e9f}, $sum/$total")
}

/**
 * check if the top is connected to the bottom over set fields
 * */
fun isConnectedCustomStack(field: BooleanArray, sx: Int, sy: Int, stack: ExpandingIntArray): Boolean {
    val goal = sx * (sy - 1)
    var isFinished = false
    fun stepOn(j: Int) {
        if (field[j]) {
            if (j < goal) {
                field[j] = false
                stack.add(j)
            } else {
                isFinished = true
            }
        }
    }
    for (j in 0 until sx) {
        stepOn(j)
    }
    while (stack.isNotEmpty() && !isFinished) {
        val i = stack.removeLast()
        val x = i % sx
        // check all neighbors;
        // down last, so we prefer it
        if (i >= sx) stepOn(i - sx)
        if (x > 0) stepOn(i - 1)
        if (x + 1 < sx) stepOn(i + 1)
        stepOn(i + sx)
    }
    return isFinished
}

object Done : Throwable()

fun isConnectedRecursive(field: BooleanArray, sx: Int, sy: Int): Boolean {
    val goal = sx * (sy - 1)
    lateinit var check: (Int, Int) -> Unit
    fun stepOn(x: Int, y: Int) {
        val j = x + y * sx
        if (field[j]) {
            if (j < goal) {
                field[j] = false
                check(x, y)
            } else throw Done
        }
    }
    check = { x, y ->
        // check all neighbors;
        // down last, so we prefer it
        stepOn(x, y + 1)
        if (x + 1 < sx) stepOn(x + 1, y)
        if (x > 0) stepOn(x - 1, y)
        if (y > 0) stepOn(x, y - 1)
    }
    return try {
        for (j in 0 until sx) {
            stepOn(j, 0)
        }
        false
    } catch (ignored: Done) {
        true
    }
}