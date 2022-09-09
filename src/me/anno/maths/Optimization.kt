package me.anno.maths

import kotlin.math.abs

object Optimization {

    var ctr = 0

    fun simplexAlgorithm(
        v0: FloatArray,
        firstStepSize: Float,
        goodEnoughError: Float,
        maxSteps: Int,
        err: (v1: FloatArray) -> Float
    ): FloatArray {
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
        err: (v1: FloatArray) -> Float
    ): FloatArray {

        val l = v0.size

        val steps = FloatArray(l) {
            firstStepSize
        }

        var lastError = err(v0)

        // 1e-7, but there may be numerical issues
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
                    val nextError = err(v0)
                    if (nextError <= goodEnoughError) return v0
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

        return v0

    }

    fun simplexAlgorithm(
        v0: DoubleArray,
        firstStepSize: Double,
        goodEnoughError: Double,
        maxSteps: Int,
        err: (v1: DoubleArray) -> Double
    ): DoubleArray {
        val expansion = 1.3
        val contraction = -0.5
        return simplexAlgorithm(v0, firstStepSize, goodEnoughError, maxSteps, expansion, contraction, err)
    }

    fun simplexAlgorithm(
        v0: DoubleArray,
        firstStepSize: Double,
        goodEnoughError: Double,
        maxSteps: Int,
        expansion: Double,
        contraction: Double,
        err: (v1: DoubleArray) -> Double
    ): DoubleArray {

        val l = v0.size

        val steps = DoubleArray(l) {
            firstStepSize
        }

        var lastError = err(v0)

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
                    val nextError = err(v0)
                    if (nextError <= goodEnoughError) return v0
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

        return v0

    }

}