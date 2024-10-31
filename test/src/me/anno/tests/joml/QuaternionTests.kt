package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import org.junit.jupiter.api.Test
import kotlin.math.min
import kotlin.random.Random

class QuaternionTests {

    @Test
    fun testRotate() {
        assertEquals(Quaternionf().rotateX(1f), Quaternionf().rotationX(1f))
        assertEquals(Quaternionf().rotateY(1f), Quaternionf().rotationY(1f))
        assertEquals(Quaternionf().rotateZ(1f), Quaternionf().rotationZ(1f))
        assertEquals(Quaterniond().rotateX(1.0), Quaterniond().rotationX(1.0))
        assertEquals(Quaterniond().rotateY(1.0), Quaterniond().rotationY(1.0))
        assertEquals(Quaterniond().rotateZ(1.0), Quaterniond().rotationZ(1.0))
    }

    @Test
    fun testRotating() {
        testTransform(Quaternionf().rotateX(1f), ::Vector3f, "rotate") { it.rotateX(1f) }
        testTransform(Quaternionf().rotateY(2f), ::Vector3f, "rotate") { it.rotateY(2f) }
        testTransform(Quaternionf().rotateZ(3f), ::Vector3f, "rotate") { it.rotateZ(3f) }
        testTransform(Quaternionf().rotateX(1f), ::Vector4f, "rotate") { it.rotateX(1f) }
        testTransform(Quaternionf().rotateY(2f), ::Vector4f, "rotate") { it.rotateY(2f) }
        testTransform(Quaternionf().rotateZ(3f), ::Vector4f, "rotate") { it.rotateZ(3f) }
        testTransform(Quaterniond().rotateX(1.0), ::Vector3d, "rotate") { it.rotateX(1.0) }
        testTransform(Quaterniond().rotateY(2.0), ::Vector3d, "rotate") { it.rotateY(2.0) }
        testTransform(Quaterniond().rotateZ(3.0), ::Vector3d, "rotate") { it.rotateZ(3.0) }
        testTransform(Quaterniond().rotateX(1.0), ::Vector4d, "rotate") { it.rotateX(1.0) }
        testTransform(Quaterniond().rotateY(2.0), ::Vector4d, "rotate") { it.rotateY(2.0) }
        testTransform(Quaterniond().rotateZ(3.0), ::Vector4d, "rotate") { it.rotateZ(3.0) }
    }

    @Test
    fun testRotatingInv() {
        testTransform(Quaternionf().rotateX(-1f), ::Vector3f, "rotateInv") { it.rotateX(1f) }
        testTransform(Quaternionf().rotateY(-2f), ::Vector3f, "rotateInv") { it.rotateY(2f) }
        testTransform(Quaternionf().rotateZ(-3f), ::Vector3f, "rotateInv") { it.rotateZ(3f) }
        testTransform(Quaternionf().rotateX(-1f), ::Vector4f, "rotateInv") { it.rotateX(1f) }
        testTransform(Quaternionf().rotateY(-2f), ::Vector4f, "rotateInv") { it.rotateY(2f) }
        testTransform(Quaternionf().rotateZ(-3f), ::Vector4f, "rotateInv") { it.rotateZ(3f) }
        testTransform(Quaterniond().rotateX(-1.0), ::Vector3d, "rotateInv") { it.rotateX(1.0) }
        testTransform(Quaterniond().rotateY(-2.0), ::Vector3d, "rotateInv") { it.rotateY(2.0) }
        testTransform(Quaterniond().rotateZ(-3.0), ::Vector3d, "rotateInv") { it.rotateZ(3.0) }
        testTransform(Quaterniond().rotateX(-1.0), ::Vector4d, "rotateInv") { it.rotateX(1.0) }
        testTransform(Quaterniond().rotateY(-2.0), ::Vector4d, "rotateInv") { it.rotateY(2.0) }
        testTransform(Quaterniond().rotateZ(-3.0), ::Vector4d, "rotateInv") { it.rotateZ(3.0) }
    }

    @Test
    fun testRotateAxis() {
        assertEquals(Quaternionf().rotateX(1f), Quaternionf().rotateAxis(1f, 1f, 0f, 0f))
        assertEquals(Quaternionf().rotateY(1f), Quaternionf().rotateAxis(1f, 0f, 1f, 0f))
        assertEquals(Quaternionf().rotateZ(1f), Quaternionf().rotateAxis(1f, 0f, 0f, 1f))
        assertEquals(Quaterniond().rotateX(1.0), Quaterniond().rotateAxis(1.0, 1.0, 0.0, 0.0))
        assertEquals(Quaterniond().rotateY(1.0), Quaterniond().rotateAxis(1.0, 0.0, 1.0, 0.0))
        assertEquals(Quaterniond().rotateZ(1.0), Quaterniond().rotateAxis(1.0, 0.0, 0.0, 1.0))
    }

    @Test
    fun testEulerAnglesYXZ() {
        assertEquals(
            Vector3f(0.1f, 0.2f, 0.3f), Quaternionf()
                .rotateYXZ(0.2f, 0.1f, 0.3f)
                .getEulerAnglesYXZ(Vector3f()), 1e-6
        )
        assertEquals(
            Vector3d(0.1, 0.2, 0.3), Quaterniond()
                .rotateYXZ(0.2, 0.1, 0.3)
                .getEulerAnglesYXZ(Vector3d()), 1e-16
        )
    }

    @Test
    fun testEulerAnglesXYZ() {
        assertEquals(
            Vector3f(0.1f, 0.2f, 0.3f), Quaternionf()
                .rotateXYZ(0.1f, 0.2f, 0.3f)
                .getEulerAnglesXYZ(Vector3f()), 1e-6
        )
        assertEquals(
            Vector3d(0.1, 0.2, 0.3), Quaterniond()
                .rotateXYZ(0.1, 0.2, 0.3)
                .getEulerAnglesXYZ(Vector3d()), 1e-16
        )
    }

    @Test
    fun testEulerAnglesZXY() {
        assertEquals(
            Vector3f(0.1f, 0.2f, 0.3f), Quaternionf()
                .rotateZ(0.3f).rotateX(0.1f).rotateY(0.2f)
                .getEulerAnglesZXY(Vector3f()), 1e-6
        )
        assertEquals(
            Vector3d(0.1, 0.2, 0.3), Quaterniond()
                .rotateZ(0.3).rotateX(0.1).rotateY(0.2)
                .getEulerAnglesZXY(Vector3d()), 1e-16
        )
    }

    @Test
    fun testEulerAnglesZYX() {
        assertEquals(
            Vector3f(0.1f, 0.2f, 0.3f), Quaternionf()
                .rotateZYX(0.3f, 0.2f, 0.1f)
                .getEulerAnglesZYX(Vector3f()), 1e-6
        )
        assertEquals(
            Vector3d(0.1, 0.2, 0.3), Quaterniond()
                .rotateZYX(0.3, 0.2, 0.1)
                .getEulerAnglesZYX(Vector3d()), 1e-16
        )
    }

    fun <Q : Any, V : Vector> testTransform(
        q: Q, createVector: () -> V,
        transformName: String,
        transformManually: (V) -> V
    ) {
        val random = Random(1234)
        for (i in 0 until 20) {
            val a = createVector()
            val b = createVector()
            for (j in 0 until min(a.numComponents, 3)) {
                val v = (random.nextDouble() - 0.5) * 20.0
                a.setComp(j, v)
                b.setComp(j, v)
            }
            val expected = transformManually(a)
            val actually = b::class.java
                .getMethod(transformName, q::class.java, b::class.java)
                .invoke(b, q, createVector()) as Vector
            assertEquals(expected, actually, 1e-4)
        }
    }
}