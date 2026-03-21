package me.anno.utils.search

import me.anno.maths.Maths.clamp
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

}