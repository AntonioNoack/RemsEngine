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
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class AxisAngleTest {

    @Test
    fun testEquality3x3f() {
        assertEquals(Matrix3f().rotateX(1f), Matrix3f().rotate(AxisAngle4f(1f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix3f().rotateX(-2f), Matrix3f().rotate(AxisAngle4f(-2f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix3f().rotateY(1f), Matrix3f().rotate(AxisAngle4f(1f, 0f, 1f, 0f)), 1e-6)
        assertEquals(Matrix3f().rotateZ(1f), Matrix3f().rotate(AxisAngle4f(1f, 0f, 0f, 1f)), 1e-6)
    }

    @Test
    fun testEquality4x3f() {
        assertEquals(Matrix4x3f().rotateX(1f), Matrix4x3f().rotate(AxisAngle4f(1f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4x3f().rotateX(-2f), Matrix4x3f().rotate(AxisAngle4f(-2f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4x3f().rotateY(1f), Matrix4x3f().rotate(AxisAngle4f(1f, 0f, 1f, 0f)), 1e-6)
        assertEquals(Matrix4x3f().rotateZ(1f), Matrix4x3f().rotate(AxisAngle4f(1f, 0f, 0f, 1f)), 1e-6)
    }

    @Test
    fun testEquality4x4f() {
        assertEquals(Matrix4f().rotateX(1f), Matrix4f().rotate(AxisAngle4f(1f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4f().rotateX(-2f), Matrix4f().rotate(AxisAngle4f(-2f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4f().rotateY(1f), Matrix4f().rotate(AxisAngle4f(1f, 0f, 1f, 0f)), 1e-6)
        assertEquals(Matrix4f().rotateZ(1f), Matrix4f().rotate(AxisAngle4f(1f, 0f, 0f, 1f)), 1e-6)
    }

    @Test
    fun testEquality3x3d() {
        assertEquals(Matrix3d().rotateX(1.0), Matrix3d().rotate(AxisAngle4d(1.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix3d().rotateX(-2.0), Matrix3d().rotate(AxisAngle4d(-2.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix3d().rotateY(1.0), Matrix3d().rotate(AxisAngle4d(1.0, 0.0, 1.0, 0.0)), 1e-15)
        assertEquals(Matrix3d().rotateZ(1.0), Matrix3d().rotate(AxisAngle4d(1.0, 0.0, 0.0, 1.0)), 1e-15)
    }

    @Test
    fun testEquality4x3d() {
        assertEquals(Matrix4x3d().rotateX(1.0), Matrix4x3d().rotate(AxisAngle4d(1.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4x3d().rotateX(-2.0), Matrix4x3d().rotate(AxisAngle4d(-2.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4x3d().rotateY(1.0), Matrix4x3d().rotate(AxisAngle4d(1.0, 0.0, 1.0, 0.0)), 1e-15)
        assertEquals(Matrix4x3d().rotateZ(1.0), Matrix4x3d().rotate(AxisAngle4d(1.0, 0.0, 0.0, 1.0)), 1e-15)
    }

    @Test
    fun testEquality4x4d() {
        assertEquals(Matrix4d().rotateX(1.0), Matrix4d().rotate(AxisAngle4d(1.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4d().rotateX(-2.0), Matrix4d().rotate(AxisAngle4d(-2.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4d().rotateY(1.0), Matrix4d().rotate(AxisAngle4d(1.0, 0.0, 1.0, 0.0)), 1e-15)
        assertEquals(Matrix4d().rotateZ(1.0), Matrix4d().rotate(AxisAngle4d(1.0, 0.0, 0.0, 1.0)), 1e-15)
    }

    @Test
    fun testFromM3x3fAndBack() {
        val original = Matrix3f().rotateYXZ(1f, 2f, 3f)
        val tested = AxisAngle4f().set(original)
        val cloned = Matrix3f().rotate(tested)
        assertEquals(original, cloned, 1e-6)
    }

    @Test
    fun testFromM4x3fAndBack() {
        val original = Matrix4x3f().rotateYXZ(1f, 2f, 3f)
        val tested = AxisAngle4f().set(original)
        val cloned = Matrix4x3f().rotate(tested)
        assertEquals(original, cloned, 1e-6)
    }

    @Test
    fun testFromM4x4fAndBack() {
        val original = Matrix4f().rotateYXZ(1f, 2f, 3f)
        val tested = AxisAngle4f().set(original)
        val cloned = Matrix4f().rotate(tested)
        assertEquals(original, cloned, 1e-6)
    }

    @Test
    fun testFromM3x3dAndBack() {
        val original = Matrix3d().rotateYXZ(1.0, 2.0, 3.0)
        val tested = AxisAngle4d().set(original)
        val cloned = Matrix3d().rotate(tested)
        assertEquals(original, cloned, 1e-15)
    }

    @Test
    fun testFromM4x3dAndBack() {
        val original = Matrix4x3d().rotateYXZ(1.0, 2.0, 3.0)
        val tested = AxisAngle4d().set(original)
        val cloned = Matrix4x3d().rotate(tested)
        assertEquals(original, cloned, 1e-15)
    }

    @Test
    fun testFromM4x4dAndBack() {
        val original = Matrix4d().rotateYXZ(1.0, 2.0, 3.0)
        val tested = AxisAngle4d().set(original)
        val cloned = Matrix4d().rotate(tested)
        assertEquals(original, cloned, 1e-15)
    }

    @Test
    fun testSettersF() {
        assertEquals(AxisAngle4f(1f, 2f, 3f, 4f), AxisAngle4f().set(1f, Vector3f(2f, 3f, 4f)))
        assertEquals(AxisAngle4f(1f, 2f, 3f, 4f), AxisAngle4f().set(1f, 2f, 3f, 4f))
        assertEquals(AxisAngle4f(1f, 2f, 3f, 4f), AxisAngle4f().set(AxisAngle4f(1f, 2f, 3f, 4f)))
        assertEquals(AxisAngle4f(1f, 2f, 3f, 4f), AxisAngle4f().set(AxisAngle4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(AxisAngle4f(1f, 2f, 3f, 4f), AxisAngle4f(AxisAngle4f(1f, 2f, 3f, 4f)))
        val rotation = Quaternionf().rotationXYZ(1f, 2f, 3f)
        val expected = AxisAngle4f().set(rotation)
        val e = 1e-6
        assertEquals(expected, AxisAngle4f().set(Quaterniond(rotation)))
        assertEquals1(expected, AxisAngle4f().set(Matrix3f().rotate(rotation)), e)
        assertEquals1(expected, AxisAngle4f().set(Matrix3d().rotate(rotation)), e)
        assertEquals1(expected, AxisAngle4f().set(Matrix4f().rotate(rotation)), e)
        assertEquals1(expected, AxisAngle4f().set(Matrix4d().rotate(rotation)), e)
        assertEquals1(expected, AxisAngle4f().set(Matrix4x3f().rotate(rotation)), e)
        // why is 4x3d missing?
    }

    @Test
    fun testSettersD() {
        val e = 1e-6
        assertEquals(AxisAngle4d(1.0, 2.0, 3.0, 4.0), AxisAngle4d().set(1.0, Vector3d(2.0, 3.0, 4.0)))
        assertEquals(AxisAngle4d(1.0, 2.0, 3.0, 4.0), AxisAngle4d().set(1.0, 2.0, 3.0, 4.0))
        assertEquals(AxisAngle4d(1.0, 2.0, 3.0, 4.0), AxisAngle4d().set(AxisAngle4f(1f, 2f, 3f, 4f)))
        assertEquals(AxisAngle4d(1.0, 2.0, 3.0, 4.0), AxisAngle4d().set(AxisAngle4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(AxisAngle4d(1.0, 2.0, 3.0, 4.0), AxisAngle4d(AxisAngle4f(1f, 2f, 3f, 4f)))
        assertEquals(AxisAngle4d(1.0, 2.0, 3.0, 4.0), AxisAngle4d(AxisAngle4d(1.0, 2.0, 3.0, 4.0)))
        val rotation = Quaterniond().rotationXYZ(1.0, 2.0, 3.0)
        val expected = AxisAngle4d().set(rotation)
        assertEquals(expected, AxisAngle4d().set(Quaterniond(rotation)))
        assertEquals1(expected, AxisAngle4d().set(Matrix3d().rotate(rotation)), e)
        assertEquals1(expected, AxisAngle4d().set(Matrix4d().rotate(rotation)), e)
        assertEquals1(expected, AxisAngle4d().set(Matrix4x3d().rotate(rotation)), e)
    }

    @Test
    fun testGettersF() {
        val e = 1e-6
        val rotation = Quaternionf().rotationXYZ(1f, 2f, 3f)
        val expected = AxisAngle4f().set(rotation)
        assertEquals(expected, expected.get(AxisAngle4f()))
        assertEquals(Matrix3f().rotate(rotation), expected.get(Matrix3f()), e)
        assertEquals(Matrix3d().rotate(rotation), expected.get(Matrix3d()), e)
        assertEquals(Matrix4f().rotate(rotation), expected.get(Matrix4f()), e)
        assertEquals(Matrix4d().rotate(rotation), expected.get(Matrix4d()), e)
        // 4x3 doesn't exist at all?
    }

    @Test
    fun testGettersG() {
        val e = 1e-6
        val rotation = Quaterniond().rotationXYZ(1.0, 2.0, 3.0)
        val expected = AxisAngle4d().set(rotation)
        assertEquals1(AxisAngle4f().set(rotation), expected.get(AxisAngle4f()), e)
        assertEquals1(AxisAngle4d().set(rotation), expected.get(AxisAngle4d()), e)
        assertEquals(Matrix3d().rotate(rotation), expected.get(Matrix3d()), e)
        assertEquals(Matrix4d().rotate(rotation), expected.get(Matrix4d()), e)
        // 4x3 doesn't exist at all?
    }

    @Test
    fun testGetSetCompF() {
        val v = AxisAngle4f(1f, 2f, 3f, 4f)
        assertEquals(1.0, v.getComp(0))
        assertEquals(2.0, v.getComp(1))
        assertEquals(3.0, v.getComp(2))
        assertEquals(4.0, v.getComp(3))
        v.setComp(0, 7.0)
        v.setComp(1, 8.0)
        v.setComp(2, 9.0)
        v.setComp(3, 10.0)
        assertEquals(AxisAngle4f(7f, 8f, 9f, 10f), v)
    }

    @Test
    fun testGetSetCompD() {
        val v = AxisAngle4d(1.0, 2.0, 3.0, 4.0)
        assertEquals(1.0, v.getComp(0))
        assertEquals(2.0, v.getComp(1))
        assertEquals(3.0, v.getComp(2))
        assertEquals(4.0, v.getComp(3))
        v.setComp(0, 7.0)
        v.setComp(1, 8.0)
        v.setComp(2, 9.0)
        v.setComp(3, 10.0)
        assertEquals(AxisAngle4d(7.0, 8.0, 9.0, 10.0), v)
    }

    fun assertEquals1(a: AxisAngle4f, b: AxisAngle4f, e: Double) {
        assertEquals(a.transform(Vector3f(1f, 0f, 0f)), b.transform(Vector3f(1f, 0f, 0f)), e)
        assertEquals(a.transform(Vector3f(0f, 1f, 0f)), b.transform(Vector3f(0f, 1f, 0f)), e)
        assertEquals(a.transform(Vector3f(0f, 0f, 1f)), b.transform(Vector3f(0f, 0f, 1f)), e)
    }

    fun assertEquals1(a: AxisAngle4d, b: AxisAngle4d, e: Double) {
        assertEquals(a.transform(Vector3f(1f, 0f, 0f)), b.transform(Vector3f(1f, 0f, 0f)), e)
        assertEquals(a.transform(Vector3f(0f, 1f, 0f)), b.transform(Vector3f(0f, 1f, 0f)), e)
        assertEquals(a.transform(Vector3f(0f, 0f, 1f)), b.transform(Vector3f(0f, 0f, 1f)), e)
    }
}