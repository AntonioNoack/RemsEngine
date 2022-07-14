package me.anno.maths

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// is this still used anywhere?
class ContinuousMedian(val min: Float, val max: Float, numBuckets: Int = 16) {

    var total = 0f
    val bucketWeights = FloatArray(numBuckets)

    private val numM1 = numBuckets - 1
    private val invScale = (max - min) / numBuckets
    val scale = numBuckets / (max - min)

    var median = (min + max) * 0.5f
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

}