package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Vectors
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class VectorTests {
    @Test
    fun testAverages() {
        testAverage2(::Vector2f, Vectors::avg)
        testAverage2(::Vector2d, Vectors::avg)
        testAverage2(::Vector3f, Vectors::avg)
        testAverage2(::Vector3d, Vectors::avg)
        testAverage3(::Vector2f, Vectors::avg, 1e-7)
        testAverage3(::Vector2d, Vectors::avg, 1e-15)
        testAverage3(::Vector3f, Vectors::avg, 1e-7)
        testAverage3(::Vector3d, Vectors::avg, 1e-15)
    }

    fun <V : Vector> testAverage2(createVector: () -> V, avgFunc: (V, V) -> V, delta: Double = 0.0) {
        val a = createVector()
        val b = createVector()
        val avg = createVector()
        val rnd = Random(165)
        for (i in 0 until 20) {
            for (j in 0 until a.numComponents) {
                a.setComp(j, rnd.nextDouble())
                b.setComp(j, rnd.nextDouble())
                avg.setComp(j, (a.getComp(j) + b.getComp(j)) * 0.5)
            }
            assertEquals(avg, avgFunc(a, b), delta)
        }
    }

    fun <V : Vector> testAverage3(createVector: () -> V, avgFunc: (V, V, V) -> V, delta: Double = 0.0) {
        val a = createVector()
        val b = createVector()
        val c = createVector()
        val avg = createVector()
        val rnd = Random(165)
        for (i in 0 until 20) {
            for (j in 0 until a.numComponents) {
                a.setComp(j, rnd.nextDouble())
                b.setComp(j, rnd.nextDouble())
                c.setComp(j, rnd.nextDouble())
                avg.setComp(j, (a.getComp(j) + b.getComp(j) + c.getComp(j)) / 3.0)
            }
            assertEquals(avg, avgFunc(a, b, c), delta)
        }
    }
}