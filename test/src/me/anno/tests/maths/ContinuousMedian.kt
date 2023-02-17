package me.anno.tests.maths

import me.anno.maths.ContinuousMedian
import me.anno.maths.Maths
import me.anno.utils.types.Floats.f6
import me.anno.utils.types.Floats.f6s
import me.anno.utils.types.Strings.withLength
import kotlin.math.sqrt

fun main() {
    testDirect()
    testDiscrete()
    testContinuous()
}

fun testDirect() {
    val instance = ContinuousMedian(0f, 3000f, 6)
    instance.bucketWeights[0] = 6f
    instance.bucketWeights[1] = 7f
    instance.bucketWeights[2] = 9f
    instance.bucketWeights[3] = 8f
    instance.bucketWeights[4] = 4f
    instance.bucketWeights[5] = 6f
    instance.total = instance.bucketWeights.sum()
    instance.median = Float.NaN
    println(instance.median) // shall be 1388.889
}

fun testDiscrete() {
    val instance = ContinuousMedian(0f, 3000f, 6)
    instance.add(250f, 6f)
    instance.add(750f, 7f)
    instance.add(1250f, 9f)
    instance.add(1750f, 8f)
    instance.add(2250f, 4f)
    instance.add(2750f, 6f)
    instance.total = instance.bucketWeights.sum()
    instance.median = Float.NaN
    println(instance.median)
    // shall be 1388.889
    // actual, with sharpness 1.1: 1393.785 -> good enough :)
}

fun testContinuous() {
    val samples = 1
    val testsEach = 23
    val min = 0f
    val max = 1f
    for (numBuckets in 3..4) {
        val thinness = 0.01f / numBuckets
        val instance = ContinuousMedian(min, max, numBuckets)
        var errSum = 0f
        var errSum2 = 0f
        val t = 0.5f
        val minValue = t / numBuckets
        val maxValue = 1f - minValue
        println("$numBuckets buckets, test values: $minValue .. $maxValue")
        for (i in 0 until testsEach) {
            instance.reset()
            val testValue = Maths.mix(min, max, Maths.mix(minValue, maxValue, i / (testsEach - 1f)))
            for (j in 0 until samples) {
                val rv = if (samples == 1) 0.5f else j / (samples - 1f) // random.nextFloat() //
                instance.add(testValue + (rv - 0.5f) * thinness)
            }
            val median = instance.median
            val error = median - testValue
            val relErr = error / thinness
            println(
                "relErr: ${relErr.f6s().withLength(12)}, " +
                        "got ${median.f6()} instead of ${testValue.f6()}, " +
                        "target index: ${((testValue - min) * instance.scale).f6()}, " +
                        "data: ${instance.bucketWeights.joinToString()}"
            )
            // LOGGER.info("$testValue +/- $thinness*0.5 -> ${instance.bucketWeights.joinToString()}")
            errSum += relErr * relErr
            errSum2 += relErr
        }
        println("stdDev: ${sqrt(errSum / testsEach)}, bias: ${errSum2 / testsEach}\n")
    }
}
