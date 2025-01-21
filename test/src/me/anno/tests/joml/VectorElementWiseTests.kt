package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.JomlMath
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class VectorElementWiseTests {
    fun <V : Vector> testElementWise2(
        createVector: () -> V,
        transformVector: (V, V) -> V,
        transformScalar: (Double, Double) -> Double,
        threshold: Double = 0.0
    ) {
        val rnd = Random(13245)
        val a = createVector()
        val b = createVector()
        val c = createVector()
        for (i in 0 until 20) {
            for (j in 0 until a.numComponents) {
                a.setComp(j, rnd.nextDouble())
                b.setComp(j, rnd.nextDouble())
                c.setComp(j, transformScalar(a.getComp(j), b.getComp(j)))
            }
            assertEquals(c, transformVector(a, b), threshold)
        }
    }

    fun <V : Vector> testElementWise3(
        createVector: () -> V,
        transformVector: (V, V, V) -> V,
        transformScalar: (Double, Double, Double) -> Double,
        threshold: Double = 0.0
    ) {
        val rnd = Random(13245)
        val a = createVector()
        val b = createVector()
        val c = createVector()
        val d = createVector()
        for (i in 0 until 20) {
            for (j in 0 until a.numComponents) {
                a.setComp(j, rnd.nextDouble())
                b.setComp(j, rnd.nextDouble())
                c.setComp(j, rnd.nextDouble())
                d.setComp(j, transformScalar(a.getComp(j), b.getComp(j), c.getComp(j)))
            }
            assertEquals(d, transformVector(a, b, c), threshold)
        }
    }

    fun <V : Vector> testElementWise4(
        createVector: () -> V,
        transformVector: (V, V, V, V) -> V,
        transformScalar: (Double, Double, Double, Double) -> Double,
        threshold: Double = 0.0
    ) {
        val rnd = Random(13245)
        val a = createVector()
        val b = createVector()
        val c = createVector()
        val d = createVector()
        val e = createVector()
        for (i in 0 until 20) {
            for (j in 0 until a.numComponents) {
                a.setComp(j, rnd.nextDouble())
                b.setComp(j, rnd.nextDouble())
                c.setComp(j, rnd.nextDouble())
                d.setComp(j, rnd.nextDouble())
                e.setComp(j, transformScalar(a.getComp(j), b.getComp(j), c.getComp(j), d.getComp(j)))
            }
            assertEquals(e, transformVector(a, b, c, d), threshold)
        }
    }

    @Test
    fun testAdd() {
        val add = { a: Double, b: Double -> a + b }
        testElementWise2(::Vector2f, { a, b -> a.add(b) }, add)
        testElementWise2(::Vector2d, { a, b -> a.add(b) }, add)
        testElementWise2(::Vector3f, { a, b -> a.add(b) }, add)
        testElementWise2(::Vector3d, { a, b -> a.add(b) }, add)
        testElementWise2(::Vector4f, { a, b -> a.add(b) }, add)
        testElementWise2(::Vector4d, { a, b -> a.add(b) }, add)
    }

    @Test
    fun testSub() {
        val sub = { a: Double, b: Double -> a - b }
        testElementWise2(::Vector2f, { a, b -> a.sub(b) }, sub)
        testElementWise2(::Vector2d, { a, b -> a.sub(b) }, sub)
        testElementWise2(::Vector3f, { a, b -> a.sub(b) }, sub)
        testElementWise2(::Vector3d, { a, b -> a.sub(b) }, sub)
        testElementWise2(::Vector4f, { a, b -> a.sub(b) }, sub)
        testElementWise2(::Vector4d, { a, b -> a.sub(b) }, sub)
    }

    @Test
    fun testMul() {
        val mul = { a: Double, b: Double -> a * b }
        testElementWise2(::Vector2f, { a, b -> a.mul(b) }, mul)
        testElementWise2(::Vector2d, { a, b -> a.mul(b) }, mul)
        testElementWise2(::Vector3f, { a, b -> a.mul(b) }, mul)
        testElementWise2(::Vector3d, { a, b -> a.mul(b) }, mul)
        testElementWise2(::Vector4f, { a, b -> a.mul(b) }, mul)
        testElementWise2(::Vector4d, { a, b -> a.mul(b) }, mul)
    }

    @Test
    fun testDiv() {
        val div = { a: Double, b: Double -> a / b }
        testElementWise2(::Vector2f, { a, b -> a.div(b) }, div)
        testElementWise2(::Vector2d, { a, b -> a.div(b) }, div)
        testElementWise2(::Vector3f, { a, b -> a.div(b) }, div)
        testElementWise2(::Vector3d, { a, b -> a.div(b) }, div)
        testElementWise2(::Vector4f, { a, b -> a.div(b) }, div)
        testElementWise2(::Vector4d, { a, b -> a.div(b) }, div)
    }

    @Test
    fun testFma() {
        val tf = 2e-7
        val fma = { a: Double, b: Double, c: Double -> a + b * c }
        testElementWise3(::Vector2f, { a, b, c -> a.fma(b, c) }, fma, tf)
        testElementWise3(::Vector2d, { a, b, c -> a.fma(b, c) }, fma)
        testElementWise3(::Vector3f, { a, b, c -> a.fma(b, c) }, fma, tf)
        testElementWise3(::Vector3d, { a, b, c -> a.fma(b, c) }, fma)
        testElementWise3(::Vector4f, { a, b, c -> a.fma(b, c) }, fma, tf)
        testElementWise3(::Vector4d, { a, b, c -> a.fma(b, c) }, fma)
    }

    @Test
    fun testMulAdd() {
        val tf = 2e-7
        val mulAdd = { a: Double, b: Double, c: Double -> a * b + c }
        testElementWise3(::Vector2f, { a, b, c -> a.mulAdd(b, c) }, mulAdd, tf)
        testElementWise3(::Vector2d, { a, b, c -> a.mulAdd(b, c) }, mulAdd)
        testElementWise3(::Vector3f, { a, b, c -> a.mulAdd(b, c) }, mulAdd, tf)
        testElementWise3(::Vector3d, { a, b, c -> a.mulAdd(b, c) }, mulAdd)
        testElementWise3(::Vector4f, { a, b, c -> a.mulAdd(b, c) }, mulAdd, tf)
        testElementWise3(::Vector4d, { a, b, c -> a.mulAdd(b, c) }, mulAdd)
    }

    @Test
    fun testMixing() {
        val tf = 1e-7
        val td = 1e-16
        for (d in listOf(0.2, 0.7)) {
            val f = d.toFloat()
            val mix = { a: Double, b: Double -> a + (b - a) * d }
            testElementWise2(::Vector2f, { a, b -> a.mix(b, f) }, mix, tf)
            testElementWise2(::Vector2d, { a, b -> a.mix(b, d) }, mix, td)
            testElementWise2(::Vector3f, { a, b -> a.mix(b, f) }, mix, tf)
            testElementWise2(::Vector3d, { a, b -> a.mix(b, d) }, mix, td)
            testElementWise2(::Vector4f, { a, b -> a.mix(b, f) }, mix, tf)
            testElementWise2(::Vector4d, { a, b -> a.mix(b, d) }, mix, td)
        }
    }

    @Test
    fun testSmoothstep() {
        val tf = 2e-7
        val td = 1e-16
        for (d in listOf(0.2, 0.7)) {
            val f = d.toFloat()
            val smooth = { a: Double, b: Double ->
                JomlMath.smoothStep(a, b, d, d * d, d * d * d)
            }
            testElementWise2(::Vector2f, { a, b -> a.smoothStep(b, f) }, smooth, tf)
            testElementWise2(::Vector2d, { a, b -> a.smoothStep(b, d) }, smooth, td)
            testElementWise2(::Vector3f, { a, b -> a.smoothStep(b, f) }, smooth, tf)
            testElementWise2(::Vector3d, { a, b -> a.smoothStep(b, d) }, smooth, td)
            testElementWise2(::Vector4f, { a, b -> a.smoothStep(b, f) }, smooth, tf)
            testElementWise2(::Vector4d, { a, b -> a.smoothStep(b, d) }, smooth, td)
        }
    }

    @Test
    fun testHermite() {
        val tf = 3e-7
        val td = 1e-16
        for (dx in listOf(0.2, 0.7)) {
            val fx = dx.toFloat()
            val mix = { a: Double, b: Double, c: Double, d: Double ->
                JomlMath.hermite(a, b, c, d, dx, dx * dx, dx * dx * dx)
            }
            testElementWise4(::Vector2f, { a, b, c, d -> a.hermite(b, c, d, fx) }, mix, tf)
            testElementWise4(::Vector2d, { a, b, c, d -> a.hermite(b, c, d, dx) }, mix, td)
            testElementWise4(::Vector3f, { a, b, c, d -> a.hermite(b, c, d, fx) }, mix, tf)
            testElementWise4(::Vector3d, { a, b, c, d -> a.hermite(b, c, d, dx) }, mix, td)
            testElementWise4(::Vector4f, { a, b, c, d -> a.hermite(b, c, d, fx) }, mix, tf)
            testElementWise4(::Vector4d, { a, b, c, d -> a.hermite(b, c, d, dx) }, mix, td)
        }
    }
}