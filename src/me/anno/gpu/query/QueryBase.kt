package me.anno.gpu.query

abstract class QueryBase {

    var result = -1L
        private set

    private var accuResult = 0L
    private var accuWeight = 0L

    val average
        get(): Long {
            val w = accuWeight
            return if (w <= 0L) 0L else accuResult / w
        }

    open fun reset() {
        result = -1L
        accuResult = 0L
        accuWeight = 0L
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
        }
    }
}