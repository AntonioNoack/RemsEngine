package me.anno.tests.maths.optimization

import me.anno.maths.Maths
import me.anno.maths.optimization.GradientDescent
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * test gradient descent of Optimization.simplexAlgorithm
 * */
object GradientDescentTest {

    /**
    * solutions:
    * f(3.0,2.0)=0.0
    * f(-2.805118,3.131312)=0.0
    * f(-3.779310,-3.283186)=0.0
    * f(3.584428,-1.848126)=0.0
    * */
    fun himmelblau(x: Double, y: Double): Double {
        return Maths.sq(x * x + y - 11) + Maths.sq(x + y * y - 7)
    }

    @Test
    fun testHimmelblauOptimization() {
        val maxError = 1e-16
        val solution = GradientDescent.simplexAlgorithm(doubleArrayOf(0.0, 0.0), 1.0, maxError, 200) {
            himmelblau(it[0], it[1])
        }.second
        assertEquals(himmelblau(solution[0], solution[1]), 0.0, maxError)
    }
}