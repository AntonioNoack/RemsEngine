package me.anno.tests.utils

import me.anno.maths.Maths
import me.anno.maths.Optimization

fun main() {
    // test gradient descent
    val t0 = System.nanoTime()
    fun himmelblau(x: Double, y: Double): Double {
        return Maths.sq(x * x + y - 11) + Maths.sq(x + y * y - 7)
    }
    /*
    * solutions:
    * f(3.0,2.0)=0.0
    * f(-2.805118,3.131312)=0.0
    * f(-3.779310,-3.283186)=0.0
    * f(3.584428,-1.848126)=0.0
    * */
    val solution = Optimization.simplexAlgorithm(doubleArrayOf(0.0, 0.0), 1.0, 1e-6, 500) {
        himmelblau(it[0], it[1])
    }.second
    val t1 = System.nanoTime()
    println(solution.joinToString() + ", value: ${himmelblau(solution[0], solution[1])}")
    println("${Optimization.ctr} sub-steps used")
    println("${((t1 - t0) * 1e-9f)}s used")
}