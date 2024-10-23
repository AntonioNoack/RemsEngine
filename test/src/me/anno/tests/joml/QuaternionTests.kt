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

    fun <Q : Any, V : Vector> testTransform(
        q: Q,
        createVector: () -> V,
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