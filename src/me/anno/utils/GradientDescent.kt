package me.anno.utils

import me.anno.utils.Maths.sq

var ctr = 0

fun gradientDescent(
    v0: FloatArray,
    firstStepSize: Float,
    goodEnoughError: Float,
    err: (v1: FloatArray) -> Float
): FloatArray {

    val l = v0.size

    val steps = FloatArray(l){
        firstStepSize
    }

    val expansion = 1.3f
    val contraction = -0.5f

    var e0 = err(v0)

    var wasChanged = false
    do {
        for(axis in 0 until l){
            // quickly alternating the axes results in not
            // needing to follow straightly to the hole
            // (25: 303 steps, 3: 48 steps for Himmelblau's function, and 1e-6 error)
            for(i in 0 until 3){
                ctr++
                val step = steps[axis]
                val x0 = v0[axis]
                val x1 = x0 + step
                v0[axis] = x1
                val e1 = err(v0)
                if(e1 <= goodEnoughError) return v0
                if(e1 < e0){
                    // better: expand and keep
                    steps[axis] *= expansion
                    e0 = e1
                    wasChanged = true
                } else {
                    // worse: contract and reset
                    steps[axis] *= contraction
                    v0[axis] = x0
                }
            }
        }
    } while (wasChanged)

    return v0

}


fun gradientDescent(
    v0: DoubleArray,
    firstStepSize: Double,
    goodEnoughError: Double,
    err: (v1: DoubleArray) -> Double
): DoubleArray {

    val l = v0.size

    val steps = DoubleArray(l){
        firstStepSize
    }

    val expansion = 1.3
    val contraction = -0.5

    var e0 = err(v0)

    var wasChanged = false
    do {
        for(axis in 0 until l){
            // quickly alternating the axes results in not
            // needing to follow straightly to the hole
            // (25: 303 steps, 3: 48 steps for Himmelblau's function, and 1e-6 error)
            for(i in 0 until 3){
                ctr++
                val step = steps[axis]
                val x0 = v0[axis]
                val x1 = x0 + step
                v0[axis] = x1
                val e1 = err(v0)
                if(e1 <= goodEnoughError) return v0
                if(e1 < e0){
                    // better: expand and keep
                    steps[axis] *= expansion
                    e0 = e1
                    wasChanged = true
                } else {
                    // worse: contract and reset
                    steps[axis] *= contraction
                    v0[axis] = x0
                }
            }
        }
    } while (wasChanged)

    return v0

}

fun himmelblau(x: Double, y: Double): Double {
    return sq(x*x+y-11) + sq(x+y*y-7)
}

fun main() {
    // test gradient descent
    val t0 = System.nanoTime()
    val solution = gradientDescent(doubleArrayOf(0.0, 0.0), 1.0, 1e-6) {
        himmelblau(it[0], it[1])
    }
    val t1 = System.nanoTime()
    println(solution.joinToString())
    println(ctr)
    println("${((t1-t0)*1e-9)}")
}