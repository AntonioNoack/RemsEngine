package me.anno.utils.search

import me.anno.maths.Maths.clamp
import me.anno.maths.MinMax.min
import me.anno.utils.types.Booleans.toInt

object Histogram {

    fun getHistogramIndex(values: IntArray, percentile: Float): Float {
        val total = values.sumOf { it.toLong() }
        val target = (total * percentile).toLong()
        var sum = 0L
        for (i in values.indices) {
            val binI = values[i]
            sum += binI
            if (sum >= target && binI > 0) {
                val relativeI = clamp(1f - (sum - target).toFloat() / binI.toFloat())
                // println("$sum,$target,$binI -> $relativeI + $i")
                return i + relativeI
            }
        }
        return values.size.toFloat()
    }

    fun getHistogramIndexI(values: IntArray, percentile: Float): Int {
        val total = values.sumOf { it.toLong() }
        val target = (total * percentile).toLong()
        var sum = 0L
        for (i in values.indices) {
            val binI = values[i]
            sum += binI
            if (sum >= target && binI > 0) {
                return i + (sum == target).toInt()
            }
        }
        return values.size
    }

    fun boxBlur(src: IntArray, dst: IntArray, radius: Int) {
        val size = src.size
        val windowSize = radius * 2 + 1

        var sum = radius.toLong() // for blurring
        for (i in 0 until size) {
            if (i == 0) {
                sum += src[0] * (radius + 1L)
                for (j in 1..radius) {
                    sum += src[min(j, size - 1)]
                }
            } else {
                sum += src[clamp(i + radius, 0, size - 1)]
                sum -= src[clamp(i - radius - 1, 0, size - 1)]
            }
            dst[i] = (sum / windowSize).toInt()
        }
    }

}