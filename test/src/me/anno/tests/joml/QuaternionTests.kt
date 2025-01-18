package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.AxisAngle4d
import org.joml.AxisAngle4f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
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

    @Test
    fun testGetAxisD() {
        val quot = Quaterniond().rotateYXZ(0.1, 0.2, 0.3)
        val baseline = Matrix3d().rotateYXZ(0.1, 0.2, 0.3)
        assertEquals(baseline.getRow(0, Vector3d()), quot.positiveX(Vector3d()), 1e-15)
        assertEquals(baseline.getRow(1, Vector3d()), quot.positiveY(Vector3d()), 1e-15)
        assertEquals(baseline.getRow(2, Vector3d()), quot.positiveZ(Vector3d()), 1e-15)
        assertEquals(baseline.getRow(0, Vector3d()), quot.normalizedPositiveX(Vector3d()), 1e-15)
        assertEquals(baseline.getRow(1, Vector3d()), quot.normalizedPositiveY(Vector3d()), 1e-15)
        assertEquals(baseline.getRow(2, Vector3d()), quot.normalizedPositiveZ(Vector3d()), 1e-15)
        assertEquals(baseline.getColumn(0, Vector3d()), quot.transformPositiveX(Vector3d()), 1e-15)
        assertEquals(baseline.getColumn(1, Vector3d()), quot.transformPositiveY(Vector3d()), 1e-15)
        assertEquals(baseline.getColumn(2, Vector3d()), quot.transformPositiveZ(Vector3d()), 1e-15)
        assertEquals(baseline.getColumn(0, Vector3d()), quot.transformUnitPositiveX(Vector3d()), 1e-15)
        assertEquals(baseline.getColumn(1, Vector3d()), quot.transformUnitPositiveY(Vector3d()), 1e-15)
        assertEquals(baseline.getColumn(2, Vector3d()), quot.transformUnitPositiveZ(Vector3d()), 1e-15)
    }

    @Test
    fun testGetAxisF() {
        val quot = Quaternionf().rotateYXZ(0.1f, 0.2f, 0.3f)
        val baseline = Matrix3f().rotateYXZ(0.1f, 0.2f, 0.3f)
        assertEquals(baseline.getRow(0, Vector3f()), quot.positiveX(Vector3f()), 1e-7)
        assertEquals(baseline.getRow(1, Vector3f()), quot.positiveY(Vector3f()), 1e-7)
        assertEquals(baseline.getRow(2, Vector3f()), quot.positiveZ(Vector3f()), 1e-7)
        assertEquals(baseline.getRow(0, Vector3f()), quot.normalizedPositiveX(Vector3f()), 1e-7)
        assertEquals(baseline.getRow(1, Vector3f()), quot.normalizedPositiveY(Vector3f()), 1e-7)
        assertEquals(baseline.getRow(2, Vector3f()), quot.normalizedPositiveZ(Vector3f()), 1e-7)
        assertEquals(baseline.getColumn(0, Vector3f()), quot.transformPositiveX(Vector3f()), 1e-7)
        assertEquals(baseline.getColumn(1, Vector3f()), quot.transformPositiveY(Vector3f()), 1e-7)
        assertEquals(baseline.getColumn(2, Vector3f()), quot.transformPositiveZ(Vector3f()), 1e-7)
        assertEquals(baseline.getColumn(0, Vector3f()), quot.transformUnitPositiveX(Vector3f()), 1e-7)
        assertEquals(baseline.getColumn(1, Vector3f()), quot.transformUnitPositiveY(Vector3f()), 1e-7)
        assertEquals(baseline.getColumn(2, Vector3f()), quot.transformUnitPositiveZ(Vector3f()), 2e-7)
    }

    private fun d() = Quaterniond()
    private fun f() = Quaternionf()

    @Test
    fun testSetFromNormalized() {
        val ad = Vector3d(0.1, 0.2, 0.3)
        val af = Vector3f(ad)
        val baseD = d().rotateYXZ(ad.y, ad.x, ad.z)
        assertEquals(baseD, d().setFromNormalized(Matrix3d().rotateYXZ(ad)), 1e-15)
        assertEquals(baseD, d().setFromNormalized(Matrix4x3d().rotateYXZ(ad)), 1e-15)
        assertEquals(baseD, d().setFromNormalized(Matrix4d().rotateYXZ(ad)), 1e-15)
        val baseF = f().rotateYXZ(af.y, af.x, af.z)
        assertEquals(baseF, f().setFromNormalized(Matrix3f().rotateYXZ(af)), 1e-7)
        assertEquals(baseF, f().setFromNormalized(Matrix4x3f().rotateYXZ(af)), 1e-7)
        assertEquals(baseF, f().setFromNormalized(Matrix4f().rotateYXZ(af)), 1e-7)
    }

    @Test
    fun testSetFromUnnormalized() {
        val ad = Vector3d(0.1, 0.2, 0.3)
        val af = Vector3f(ad)
        val sd = Vector3d(1.2, 1.5, 1.7)
        val sf = Vector3f(sd)
        val baseD = d().rotateYXZ(ad.y, ad.x, ad.z)
        assertEquals(baseD, d().setFromUnnormalized(Matrix3d().rotateYXZ(ad).scale(sd)), 1e-15)
        assertEquals(baseD, d().setFromUnnormalized(Matrix4x3d().rotateYXZ(ad).scale(sd)), 1e-15)
        assertEquals(baseD, d().setFromUnnormalized(Matrix4d().rotateYXZ(ad).scale(sd)), 1e-15)
        val baseF = f().rotateYXZ(af.y, af.x, af.z)
        assertEquals(baseF, f().setFromUnnormalized(Matrix3f().rotateYXZ(af).scale(sf)), 1e-7)
        assertEquals(baseF, f().setFromUnnormalized(Matrix4x3f().rotateYXZ(af).scale(sf)), 1e-7)
        assertEquals(baseF, f().setFromUnnormalized(Matrix4f().rotateYXZ(af).scale(sf)), 1e-7)
    }

    @Test
    fun testFromAxisAngleD() {
        val baseline = AxisAngle4d(1.0, 2.0, 3.0, 0.5).normalize()
        val quat = Quaterniond().fromAxisAngleRad(baseline.x, baseline.y, baseline.z, baseline.angle)
        val clone = AxisAngle4d().set(quat)
        assertEquals(baseline, clone, 1e-15)
    }

    @Test
    fun testFromAxisAngleF() {
        val baseline = AxisAngle4f(1f, 2f, 3f, 0.5f).normalize()
        val quat = Quaternionf().fromAxisAngleRad(baseline.x, baseline.y, baseline.z, baseline.angle)
        val clone = AxisAngle4f().set(quat)
        assertEquals(baseline, clone, 1e-7)
    }

    @Test
    fun testMulF() {
        val baseline = Quaternionf()
            .rotateYXZ(0.1f, 0.2f, 0.3f)
            .rotateYXZ(0.4f, 0.5f, 0.6f)
        val multiplied = Quaternionf()
            .rotateYXZ(0.1f, 0.2f, 0.3f)
            .mul(Quaternionf().rotateYXZ(0.4f, 0.5f, 0.6f))
        assertEquals(baseline, multiplied)
    }

    @Test
    fun testMulD() {
        val baseline = Quaterniond()
            .rotateYXZ(0.1, 0.2, 0.3)
            .rotateYXZ(0.4, 0.5, 0.6)
        val multiplied = Quaterniond()
            .rotateYXZ(0.1, 0.2, 0.3)
            .mul(Quaterniond().rotateYXZ(0.4, 0.5, 0.6))
        assertEquals(baseline, multiplied)
    }

    @Test
    fun testPreMulF() {
        val baseline = Quaternionf()
            .rotateYXZ(0.4f, 0.5f, 0.6f)
            .rotateYXZ(0.1f, 0.2f, 0.3f)
        val multiplied = Quaternionf()
            .rotateYXZ(0.1f, 0.2f, 0.3f)
            .premul(Quaternionf().rotateYXZ(0.4f, 0.5f, 0.6f))
        assertEquals(baseline, multiplied)
    }

    @Test
    fun testPreMulD() {
        val baseline = Quaterniond()
            .rotateYXZ(0.4, 0.5, 0.6)
            .rotateYXZ(0.1, 0.2, 0.3)
        val multiplied = Quaterniond()
            .rotateYXZ(0.1, 0.2, 0.3)
            .premul(Quaterniond().rotateYXZ(0.4, 0.5, 0.6))
        assertEquals(baseline, multiplied)
    }

    @Test
    fun testRotationF() {
        val baseline0 = f().rotateYXZ(0.1f, 0.2f, 0.3f)
        val baselineYXZ1 = f().rotateY(0.1f).rotateX(0.2f).rotateZ(0.3f)
        val baselineYXZ0 = baseline0.mul(baselineYXZ1, f())
        assertEquals(baselineYXZ0, Quaternionf(baseline0).rotateYXZ(0.1f, 0.2f, 0.3f), 1e-7)
        assertEquals(baselineYXZ1, f().rotationYXZ(0.1f, 0.2f, 0.3f), 1e-7)
        val baselineXYZ1 = f().rotateX(0.1f).rotateY(0.2f).rotateZ(0.3f)
        val baselineXYZ0 = baseline0.mul(baselineXYZ1, f())
        assertEquals(baselineXYZ0, Quaternionf(baseline0).rotateXYZ(0.1f, 0.2f, 0.3f), 1e-7)
        assertEquals(baselineXYZ1, f().rotationXYZ(0.1f, 0.2f, 0.3f), 1e-7)
        val baselineZYX1 = f().rotateZ(0.1f).rotateY(0.2f).rotateX(0.3f)
        val baselineZYX0 = baseline0.mul(baselineZYX1, f())
        assertEquals(baselineZYX0, Quaternionf(baseline0).rotateZYX(0.1f, 0.2f, 0.3f), 1e-7)
        assertEquals(baselineZYX1, f().rotationZYX(0.1f, 0.2f, 0.3f), 1e-7)
    }

    @Test
    fun testRotationD() {
        val baseline0 = d().rotateYXZ(0.1, 0.2, 0.3)
        val baselineYXZ1 = d().rotateY(0.1).rotateX(0.2).rotateZ(0.3)
        val baselineYXZ0 = baseline0.mul(baselineYXZ1, d())
        assertEquals(baselineYXZ0, Quaterniond(baseline0).rotateYXZ(0.1, 0.2, 0.3), 1e-7)
        assertEquals(baselineYXZ1, d().rotationYXZ(0.1, 0.2, 0.3), 1e-7)
        val baselineXYZ1 = d().rotateX(0.1).rotateY(0.2).rotateZ(0.3)
        val baselineXYZ0 = baseline0.mul(baselineXYZ1, d())
        assertEquals(baselineXYZ0, Quaterniond(baseline0).rotateXYZ(0.1, 0.2, 0.3), 1e-7)
        assertEquals(baselineXYZ1, d().rotationXYZ(0.1, 0.2, 0.3), 1e-7)
        val baselineZYX1 = d().rotateZ(0.1).rotateY(0.2).rotateX(0.3)
        val baselineZYX0 = baseline0.mul(baselineZYX1, d())
        assertEquals(baselineZYX0, Quaterniond(baseline0).rotateZYX(0.1, 0.2, 0.3), 1e-7)
        assertEquals(baselineZYX1, d().rotationZYX(0.1, 0.2, 0.3), 1e-7)
    }

    @Test
    fun transformInverseF() {
        val normal = f().rotateYXZ(0.1f, 0.2f, 0.3f)
        val vec = Vector3f(1f, 2f, 3f)
        val baseline = normal.invert(Quaternionf())
        assertEquals(baseline.transform(vec, Vector3f()), baseline.transformUnit(vec, Vector3f()), 1e-6)
        assertEquals(baseline.transform(vec, Vector3f()), normal.transformInverse(vec, Vector3f()), 1e-6)
        assertEquals(baseline.transformUnit(vec, Vector3f()), normal.transformInverseUnit(vec, Vector3f()), 1e-6)
    }

    @Test
    fun transformInverseD() {
        val normal = d().rotateYXZ(0.1, 0.2, 0.3)
        val vec = Vector3d(1f, 2f, 3f)
        val baseline = normal.invert(Quaterniond())
        assertEquals(baseline.transform(vec, Vector3d()), baseline.transformUnit(vec, Vector3d()), 1e-6)
        assertEquals(baseline.transform(vec, Vector3d()), normal.transformInverse(vec, Vector3d()), 1e-6)
        assertEquals(baseline.transformUnit(vec, Vector3d()), normal.transformInverseUnit(vec, Vector3d()), 1e-6)
    }
}