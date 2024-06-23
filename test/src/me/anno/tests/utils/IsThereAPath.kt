package me.anno.tests.utils

import me.anno.Time
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Done
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Booleans.toInt
import kotlin.random.Random

// inspired by https://www.youtube.com/watch?v=RwgKqA9j3JE
// where I think that the problem is kind of trivial, because generation time is on the same time consumption level as solving it
//  -> optimization is kind of pointless
// (and I was kind of offended that he said recursion was slow without any evidence)
//  -> it's 1.4x slower than my custom-stack version, but that's it

// a way to optimize it would be to use 64-bit long masks
//  -> yes, that's 1.9x faster (surprisingly slow, had expected a bigger speedup)

val probability = 0.595f
val prob8 = (probability * 256).toInt()

fun main() {
    val sx = 512
    val sy = 512
    var dt0 = 0L
    var dt1 = 0L
    var sum = 0
    val total = 1000
    val field = BooleanArray(sx * sy)
    val tasks = IntArrayList(64)
    val random = Random(Time.nanoTime)
    val fieldI = LongArray(ceilDiv(sx, 64) * sy)
    val reachedI = LongArray(fieldI.size)
    for (i in 0 until total) {
        val t0 = Time.nanoTime
        // fill(random, field)
        fill(random, fieldI, sx, sy)
        val t1 = Time.nanoTime
        sum += isConnectedBitMasks(fieldI, reachedI, sx, sy, tasks).toInt()
        // sum += isConnectedCustomStack(field, sx, sy, tasks).toInt()
        // sum += isConnectedRecursive(field, sx, sy).toInt()
        val t2 = Time.nanoTime
        dt0 += t1 - t0
        dt1 += t2 - t1
    }
    println("gen: ${dt0 / 1e9f} vs solve: ${dt1 / 1e9f}, $sum/$total")
}

fun fill(random: Random, field: BooleanArray) {
    for (fi in field.indices step 4) {
        val rnd = random.nextInt()
        field[fi] = rnd.ushr(24) < prob8
        field[fi + 1] = rnd.shl(8).ushr(24) < prob8
        field[fi + 2] = rnd.shl(16).ushr(24) < prob8
        field[fi + 3] = rnd.shl(24).ushr(24) < prob8
    }
}

fun Boolean.toLong(v: Long): Long {
    return if (this) v else 0L
}

fun fill(random: Random, fieldI: LongArray, sx: Int, sy: Int) {
    val sxi = ceilDiv(sx, 64)
    fieldI.fill(0L)
    for (y in 0 until sy) {
        for (x in 0 until sx step 4) {
            val fi = y * sxi + (x / 64)
            val bi = (x % 64)
            val rnd = random.nextInt()
            fieldI[fi] = fieldI[fi] or
                    (rnd.ushr(24) < prob8).toLong(1L shl bi) or
                    (rnd.shl(8).ushr(24) < prob8).toLong(2L shl bi) or
                    (rnd.shl(16).ushr(24) < prob8).toLong(4L shl bi) or
                    (rnd.shl(24).ushr(24) < prob8).toLong(8L shl bi)
        }
    }
}

/**
 * check if the top is connected to the bottom over set fields
 * */
fun isConnectedCustomStack(field: BooleanArray, sx: Int, sy: Int, stack: IntArrayList): Boolean {
    stack.clear()
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

fun LongArray.getOrZero(i: Int, condition: Boolean): Long {
    return if (condition) this[i] else 0L
}

fun LongArray.getOrZero(i: Int): Long {
    return if (i in indices) this[i] else 0L
}

fun isConnectedBitMasks(fieldI: LongArray, reachedI: LongArray, sx: Int, sy: Int, stack: IntArrayList): Boolean {

    reachedI.fill(0L)
    stack.clear()

    val sxi = ceilDiv(sx, 64)

    /*
    fun line(sx: Int, fieldI: LongArray, offset: Int): String {
        return (0 until sx).joinToString("") { x ->
            if (fieldI[(x / 64) + offset].hasFlag(1L shl (x % 64))) "X" else "."
        }
    }
    fun printField(name: String) {
        println(name)
        for (y in 0 until sy) {
            val offset = y * sxi
            println("  ${line(sx, fieldI, offset)} ${line(sx, reachedI, offset)}")
        }
    }

    // fill in data
    for (y in 0 until sy) {
        for (x in 0 until sx) {
            if (field[x + y * sx]) {
                val xi = (x / 64) + sxi * y
                val xr = x % 64
                fieldI[xi] = fieldI[xi] or (1L shl xr)
            }
        }
    }*/

    // first row was reached
    for (x in 0 until sxi) {
        reachedI[x] = fieldI[x]
    }

    val goal = sxi * (sy - 1)
    var isFinished = false
    fun updateCell(j: Int) {
        val x = j % sxi
        var runs = 0
        val pxi = reachedI.getOrZero(j + 1, x + 1 < sxi).ushr(63)
        val mxi = reachedI.getOrZero(j - 1, x > 0).shl(63)
        val py = reachedI.getOrZero(j + sxi)
        val my = reachedI.getOrZero(j - sxi)
        var currValue = reachedI[j]
        do {
            val prevValue = currValue
            // overlay from left/right neighbors
            val px = prevValue.shl(1) or pxi
            val mx = prevValue.ushr(1) or mxi
            val neighbors = (px or mx) or (py or my) // brackets to reduce data dependencies
            val newValue = prevValue or (fieldI[j] and neighbors)
            currValue = newValue
            runs++
        } while (newValue != prevValue)
        if (runs > 1) {
            reachedI[j] = currValue
            // println("Updating $j: $prevValue -> $newValue")
            if (j >= goal) {
                isFinished = true
            } else {
                // printField("Update[$j]:")
                // invalidate all neighbors
                if (j >= sxi) stack.add(j - sxi)
                if (x > 0) stack.add(j - 1)
                if (x + 1 < sxi) stack.add(j + 1)
                stack.add(j + sxi)
            }
        }
    }

    // printField("Original:")
    for (j in 0 until sxi) {
        updateCell(j + sxi)
    }
    while (stack.isNotEmpty() && !isFinished) {
        updateCell(stack.removeLast())
    }
    return isFinished
}