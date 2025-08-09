package me.anno.maths.optimization

import me.anno.Time
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

object GradientDescent {

    var ctr = 0

    fun simplexAlgorithm(
        v0: FloatArray,
        firstStepSize: Float,
        goodEnoughError: Float,
        maxSteps: Int,
        err: FloatTargetFunction
    ): Pair<Float, FloatArray> {
        val expansion = 1.3f
        val contraction = -0.5f
        return simplexAlgorithm(v0, firstStepSize, goodEnoughError, maxSteps, expansion, contraction, err)
    }

    fun simplexAlgorithm(
        v0: FloatArray,
        firstStepSize: Float,
        goodEnoughError: Float,
        maxSteps: Int,
        expansion: Float,
        contraction: Float,
        err: FloatTargetFunction
    ): Pair<Float, FloatArray> {

        val l = v0.size

        val steps = FloatArray(l) {
            firstStepSize
        }

        var lastError = err.eval(v0)

        // 1e-7, but there may be numerical issues;
        // which cause stair-stepping, which would be an issue
        val precision = 1e-6f

        var stepCtr = 0
        do {
            var wasChanged = false
            for (axis in 0 until l) {
                // quickly alternating the axes results in not
                // needing to follow straightly to the hole
                // (25: 303 steps, 3: 48 steps for Himmelblau's function, and 1e-6 error)
                for (i in 0 until 3) {
                    ctr++
                    val step = steps[axis]
                    val lastX = v0[axis]
                    val nextX = lastX + step
                    v0[axis] = nextX
                    val nextError = err.eval(v0)
                    if (nextError <= goodEnoughError) return Pair(nextError, v0)
                    if (nextError < lastError) {
                        // better: expand and keep
                        steps[axis] = step * expansion
                        lastError = nextError
                        wasChanged = true
                    } else {
                        // worse: contract and reset
                        val newStep = step * contraction
                        val minStepAllowed = abs(lastX * precision)
                        val allowedNewStep = if (abs(newStep) < minStepAllowed) {
                            if (step < 0) minStepAllowed else -minStepAllowed // alternate sign
                        } else {
                            newStep
                        }
                        // what, if the next step is just too small
                        // -> see steps as a change
                        steps[axis] = allowedNewStep
                        if (abs(allowedNewStep) < abs(newStep)) {
                            wasChanged = true
                        }
                        v0[axis] = lastX
                    }
                }
            }
        } while (wasChanged && stepCtr++ < maxSteps)

        return Pair(lastError, v0)
    }

    fun simplexAlgorithm(
        v0: DoubleArray,
        firstStepSize: Double,
        goodEnoughError: Double,
        maxSteps: Int,
        err: DoubleTargetFunction
    ): Pair<Double, DoubleArray> {
        val expansion = 1.3
        val contraction = -0.5
        return simplexAlgorithm(v0, firstStepSize, goodEnoughError, maxSteps, expansion, contraction, err)
    }

    // https://en.wikipedia.org/wiki/Random_search
    fun randomSearch(
        v0: DoubleArray,
        firstStepSize: Double,
        goodEnoughError: Double,
        maxSteps: Int,
        maxTrials: Int,
        err: DoubleTargetFunction
    ): Pair<Double, DoubleArray> {
        var bestValue = v0
        val dims = bestValue.size
        var bestError = err.eval(bestValue)
        val dv = DoubleArray(dims)
        var testValue = DoubleArray(dims)
        val rnd = Random(Time.nanoTime)
        var radius = firstStepSize
        var trials = maxTrials
        for (i in 0 until maxSteps) {
            var w = 0.0
            for (j in 0 until dims) {
                val rj = rnd.nextDouble() * 2.0 - 1.0
                dv[j] = rj
                w += rj * rj
            }
            w = radius / sqrt(w)
            bestValue.copyInto(testValue)
            for (j in 0 until dims) {
                testValue[j] += w * dv[j]
            }
            val e1 = err.eval(testValue)
            if (e1 < bestError) {
                // good :)
                bestError = e1
                val tmp = bestValue
                bestValue = testValue
                testValue = tmp
                if (bestError <= goodEnoughError) break
                radius *= 1.05
                trials = maxTrials
            } else {
                // bad :/
                radius *= 0.98
                if (radius < 1e-308 || trials-- <= 0) break
            }
        }
        return bestError to bestValue
    }

    fun simplexAlgorithm(
        v0: DoubleArray,
        firstStepSize: Double,
        goodEnoughError: Double,
        maxSteps: Int,
        expansion: Double,
        contraction: Double,
        err: DoubleTargetFunction
    ): Pair<Double, DoubleArray> {

        val l = v0.size

        val steps = DoubleArray(l) {
            firstStepSize
        }

        var lastError = err.eval(v0)

        // 1e-16, but there may be numerical issues,
        // which cause stair-stepping, which would be an issue
        val precision = 1e-14

        var stepCtr = 0
        do {
            var wasChanged = false
            for (axis in 0 until l) {
                // quickly alternating the axes results in not
                // needing to follow straightly to the hole
                // (25: 303 steps, 3: 48 steps for Himmelblau's function, and 1e-6 error)
                for (i in 0 until 3) {
                    ctr++
                    val step = steps[axis]
                    val lastX = v0[axis]
                    val nextX = lastX + step
                    v0[axis] = nextX
                    val nextError = err.eval(v0)
                    if (nextError <= goodEnoughError) return Pair(nextError, v0)
                    if (nextError < lastError) {
                        // better: expand and keep
                        steps[axis] = step * expansion
                        lastError = nextError
                        wasChanged = true
                    } else {
                        // worse: contract and reset
                        val newStep = step * contraction
                        val minStepAllowed = abs(lastX * precision)
                        val allowedNewStep = if (abs(newStep) < minStepAllowed) {
                            if (step < 0) minStepAllowed else -minStepAllowed // alternate sign
                        } else {
                            newStep
                        }
                        // what, if the next step is just too small
                        // -> see steps as a change
                        steps[axis] = allowedNewStep
                        if (abs(allowedNewStep) < abs(newStep)) {
                            wasChanged = true
                        }
                        v0[axis] = lastX
                    }
                }
            }
        } while (wasChanged && stepCtr++ < maxSteps)

        return Pair(lastError, v0)
    }
}