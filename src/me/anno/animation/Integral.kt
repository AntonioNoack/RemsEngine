package me.anno.animation

import kotlin.math.max

object Integral {

    /**
     * find the position, where I[t1]-I[t0] = target
     * with accuracy
     * */
    fun <N : Number> AnimatedProperty<N>.findIntegralX(t0: Double, t1: Double, target: Double = 1.0, accuracy: Double = 1e-6): Double {
        val allowNegativeValues = false
        val int0 = getIntegral<N>(t0, allowNegativeValues)
        // bisection
        var min = t0
        var max = t1
        // double has 53 bits mantissa -> should be enough most times
        val maxIterations = 53
        for (i in 0 until maxIterations) {
            if (max - min > accuracy) {
                val middle = (max + min) * 0.5
                val intI = getIntegral<N>(middle, allowNegativeValues) - int0
                if (intI < target) {// right side
                    min = middle
                } else {// left side
                    max = middle
                }
            } else break
        }
        return (max + min) * 0.5
    }

    fun <N : Number> AnimatedProperty<N>.getIntegral(t0: Double, t1: Double, allowNegativeValues: Boolean): Double {
        val int0 = getIntegral<N>(t0, allowNegativeValues)
        val int1 = getIntegral<N>(t1, allowNegativeValues)
        return int1 - int0
    }

    fun <N : Number> AnimatedProperty<N>.getIntegral(time: Double, allowNegativeValues: Boolean): Double {
        synchronized(this) {
            val minValue = if (allowNegativeValues) Double.NEGATIVE_INFINITY else 0.0
            val size = keyframes.size
            return when {
                size == 0 -> max(minValue, defaultValue.toDouble()) * time
                size == 1 || !isAnimated -> max(minValue, keyframes[0].value.toDouble()) * time
                else -> {
                    val startTime: Double
                    val endTime: Double
                    if (time <= 0) {
                        startTime = time
                        endTime = 0.0
                    } else {
                        startTime = 0.0
                        endTime = time
                    }
                    var sum = 0.0
                    var lastTime = startTime
                    var lastValue = max(minValue, this[startTime].toDouble())
                    for (kf in keyframes) {
                        if (kf.time > time) break // we are done
                        if (kf.time > lastTime) {// a new value
                            val value = max(minValue, (kf.value as N).toDouble())
                            sum += (lastValue + value) * (kf.time - lastTime) * 0.5
                            lastValue = value
                            lastTime = kf.time
                        }
                    }
                    val endValue = max(minValue, this[endTime].toDouble())
                    sum += (lastValue + endValue) * (time - lastTime) * 0.5
                    sum
                }
            }
        }
    }

}