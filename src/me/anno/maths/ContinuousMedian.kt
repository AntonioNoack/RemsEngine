package me.anno.maths

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.utils.types.Floats.f6
import me.anno.utils.types.Floats.f6s
import me.anno.utils.types.Strings.withLength
import org.apache.logging.log4j.LogManager
import kotlin.math.*

// is this still used anywhere?
class ContinuousMedian(val min: Float, val max: Float, numBuckets: Int = 16) {

    private var total = 0f
    private val bucketWeights = FloatArray(numBuckets)

    private val numM1 = numBuckets - 1
    private val invScale = (max - min) / numBuckets
    private val scale = numBuckets / (max - min)

    private var median = (min + max) * 0.5f
        get() {
            if (field.isNaN()) {
                // compute median
                // https://www.tutorialspoint.com/statistics/continuous_series_arithmetic_median.htm
                val half = total * 0.5f
                var precedingCumSum = 0f
                for (medianClass in 0..numM1) {
                    val weight = bucketWeights[medianClass]
                    val newSum = precedingCumSum + weight
                    if (newSum >= half) {
                        val lowerLimit = min + medianClass / scale
                        field = lowerLimit + (half - precedingCumSum) / weight * invScale
                        break
                    }
                    precedingCumSum = newSum
                }
            }
            return field
        }

    fun add(value: Float, weight: Float = 1f) {

        if (value.isNaN()) return

        val bucket = (value - min) * scale
        val bucketF = clamp(floor(bucket), 0f, numM1.toFloat())
        val bucketI = bucketF.toInt()
        val dBucket = bucket - bucketF

        // pretty ideal for 3 and 4 buckets
        val sharpness = 1.1f

        val w0 = exp(-sq(dBucket + 1.5f) * sharpness)
        val w1 = exp(-sq(dBucket + 0.5f) * sharpness)
        val w2 = exp(-sq(dBucket - 0.5f) * sharpness)
        val w3 = exp(-sq(dBucket - 1.5f) * sharpness)
        val w4 = exp(-sq(dBucket - 2.5f) * sharpness)
        val f = weight / (w0 + w1 + w2 + w3 + w4)

        bucketWeights[max(0, bucketI - 2)] += w0 * f
        bucketWeights[max(0, bucketI - 1)] += w1 * f
        bucketWeights[bucketI] += w2 * f
        bucketWeights[min(bucketI + 1, numM1)] += w3 * f
        bucketWeights[min(bucketI + 2, numM1)] += w4 * f

        total += weight
        median = Float.NaN

    }

    fun reset() {
        total = 0f
        median = (min + max) * 0.5f
        bucketWeights.fill(0f)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(ContinuousMedian::class)

        @JvmStatic
        fun main(args: Array<String>) {
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
            LOGGER.info(instance.median) // shall be 1388.889
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
            LOGGER.info(instance.median)
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
                LOGGER.info("$numBuckets buckets, test values: $minValue .. $maxValue")
                for (i in 0 until testsEach) {
                    instance.reset()
                    val testValue = mix(min, max, mix(minValue, maxValue, i / (testsEach - 1f)))
                    for (j in 0 until samples) {
                        val rv = if (samples == 1) 0.5f else j / (samples - 1f) // random.nextFloat() //
                        instance.add(testValue + (rv - 0.5f) * thinness)
                    }
                    val median = instance.median
                    val error = median - testValue
                    val relErr = error / thinness
                    LOGGER.info(
                        "relErr: ${relErr.f6s().withLength(12)}, " +
                                "got ${median.f6()} instead of ${testValue.f6()}, " +
                                "target index: ${((testValue - min) * instance.scale).f6()}, " +
                                "data: ${instance.bucketWeights.joinToString()}"
                    )
                    // LOGGER.info("$testValue +/- $thinness*0.5 -> ${instance.bucketWeights.joinToString()}")
                    errSum += relErr * relErr
                    errSum2 += relErr
                }
                LOGGER.info("stdDev: ${sqrt(errSum / testsEach)}, bias: ${errSum2 / testsEach}\n")
            }
        }

    }

}