package me.anno.bench

import me.anno.maths.FastInvSqrt.fastInvSqrt1
import me.anno.maths.FastInvSqrt.fastInvSqrt2
import me.anno.maths.Maths.sq
import me.anno.utils.Clock
import org.junit.jupiter.api.Test
import kotlin.math.max
import kotlin.math.sqrt

object FastInvSqrtBenchmark {

    @JvmStatic
    @Deprecated("In my testing, fastInvSqrt1 is just as fast but has much better precision. Use that instead!")
    fun fastInvSqrt0(number: Float): Float { // 0.58ns
        val y = number
        var bits = y.toRawBits()
        bits = 0x5f3759df - (bits shr 1)
        return Float.fromBits(bits)
    }

    @JvmStatic
    @Deprecated("In my testing, the error of fastInvSqrt2 is already very, very good (1e-6 relative)!")
    fun fastInvSqrt3(number: Float): Float { // 1.29ns, so just 1.2x faster than baseline
        val x2 = number * 0.5f
        var y = number
        var bits = y.toRawBits()
        bits = 0x5f3759df - (bits shr 1)
        y = Float.fromBits(bits)
        y = y * (1.5f - (x2 * y * y)) // 1st iteration
        y = y * (1.5f - (x2 * y * y)) // 2nd iteration
        y = y * (1.5f - (x2 * y * y)) // 3rd iteration
        return y
    }

    @JvmStatic
    fun baseline(number: Float): Float {
        // 1.59ns -> 2-3x slower -> probably neglectable unless we do lots and lots of them
        return 1f / sqrt(number)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val clock = Clock("FastInvSqrt")
        val numbers = FloatArray(1024 * 256) { it + 1024f }
        var sum1 = 0f
        var sum2 = 0f
        var sum3 = 0f
        var sumB = 0f
        clock.benchmark(50, 1000, numbers.size, "FastInvSqrt0") {
            for (i in numbers) sum1 += fastInvSqrt0(i)
        }
        clock.benchmark(50, 1000, numbers.size, "FastInvSqrt1") {
            for (i in numbers) sum2 += fastInvSqrt1(i)
        }
        clock.benchmark(50, 1000, numbers.size, "FastInvSqrt2") {
            for (i in numbers) sum3 += fastInvSqrt2(i)
        }
        clock.benchmark(50, 1000, numbers.size, "FastInvSqrt3") {
            for (i in numbers) sum3 += fastInvSqrt3(i)
        }
        clock.benchmark(50, 1000, numbers.size, "Baseline") {
            for (i in numbers) sumB += baseline(i)
        }
        println("done $sum1 vs $sum2 vs $sum3 vs $sumB")
    }

    @Test
    fun testPrecision() {
        val n = 1024
        val xs = FloatArray(n) { (n + it).toFloat() }
        for (method in listOf(::fastInvSqrt0, ::fastInvSqrt1, ::fastInvSqrt2, ::fastInvSqrt3)) {
            var avg = 0.0
            var sq = 0.0
            for (i in 0 until n) {
                val x = xs[i]
                val baseline = baseline(x)
                val actual = method(x)
                val err = (baseline - actual) / baseline
                avg += err
                sq += sq(err)
            }
            println("Result: Bias: ${avg / n}, StdDev: ${sqrt(max(avg * avg - sq, 0.0)) / n}")
            // Result: Bias: -0.016798849895874213, Stddev: 0.01678188908720228
            // Result: Bias: 8.814230879604298E-4, Stddev: 8.808323043486228E-4
            // Result: Bias: 1.5987505701184346E-6, Stddev: 1.5973780629690055E-6
            // Result: Bias: -1.685440120802184E-9, Stddev: 0.0
        }
    }
}