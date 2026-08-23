package me.anno.gpu.query

import me.anno.utils.types.Floats.roundToLongOr

abstract class QueryBase {

    /**
     * actually measured last value, or -1 if never measured before;
     * this value is asynchronous, so it may lag behind a few frames
     * */
    var result = -1L
        private set

    /**
     * after how many samples we reduce the effect of old samples
     * (gliding average)
     * */
    var maxAverageSamples: Int = 100

    private var accuResult = 0L
    private var accuWeight = 0

    /**
     * gliding average over the last values
     * */
    val average
        get(): Long {
            val w = accuWeight
            return if (w <= 0L) 0L else accuResult / w
        }

    open fun reset() {
        result = -1L
        accuResult = 0L
        accuWeight = 0
    }

    fun addSample(result: Long) {
        this.result = result
        accuResult += result
        accuWeight++

        if (accuWeight > 3 &&
            accuResult > Long.MAX_VALUE.shr(2)
        ) {
            // danger of overflow, and change to recover
            accuResult = accuResult.shr(1)
            accuWeight = accuWeight.shr(1)
        } else if (accuWeight > maxAverageSamples) {
            // use a double to avoid overflows
            val factor = maxAverageSamples.toDouble() / accuWeight // < 1
            accuResult = (accuResult * factor).roundToLongOr()
            accuWeight = maxAverageSamples
        }
    }
}